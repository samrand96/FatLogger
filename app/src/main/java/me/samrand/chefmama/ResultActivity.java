package me.samrand.chefmama;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.samrand.chefmama.engine.ai.Classifier;
import me.samrand.chefmama.engine.ai.TensorFlowImageClassifier;
import me.samrand.chefmama.engine.database.Entry;
import me.samrand.chefmama.engine.database.EntryRepository;
import me.samrand.chefmama.engine.database.Food;
import me.samrand.chefmama.fragment.AddFoodDialogFragment;
import me.samrand.chefmama.util.RecyclerAdapter;

public class ResultActivity extends AppCompatActivity
        implements AddFoodDialogFragment.DialogListener{
    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    private static final String INPUT_NAME = "Mul:0";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/trained_model.pb";
    private static final String LABEL_FILE = "file:///android_asset/labels.txt";
    private static final String JSON_FILE = "data.json";

    private static final boolean MAINTAIN_ASPECT = true;

    private Bitmap croppedBitmap;

    RecyclerAdapter adapter;
    ArrayList<Food> foodInfos;

    JSONObject jsonReader;
    private EntryRepository entryRepository;
    private String imageName = null;

    public Context getActivityContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        Uri imageUri = null;

        if (extras != null) {
            if (extras.containsKey("selectedImage"))
                imageUri = Uri.parse(extras.getString("selectedImage"));
            else if (extras.containsKey("capturedImage"))
                imageUri = Uri.parse("file://" + extras.getString("capturedImage"));

            try {
                InputStream image_stream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap= BitmapFactory.decodeStream(image_stream );

                croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);
                Matrix transformMatrix = getTransformationMatrix(
                                bitmap.getWidth(), bitmap.getHeight(),
                                INPUT_SIZE, INPUT_SIZE,
                                0, MAINTAIN_ASPECT);
                final Canvas canvas = new Canvas(croppedBitmap);
                canvas.drawBitmap(bitmap, transformMatrix, null);

                new ClassifyImageTask().execute(croppedBitmap);

                RecyclerView rv = (RecyclerView)findViewById(R.id.rv);
                LinearLayoutManager llm = new LinearLayoutManager(this);
                rv.setLayoutManager(llm);

                foodInfos = new ArrayList<Food>();
                adapter = new RecyclerAdapter(this, bitmap, true, foodInfos);
                rv.setAdapter(adapter);

                InputStream is = getAssets().open(JSON_FILE);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String jsonString = new String(buffer, "UTF-8");
                jsonReader = new JSONObject(jsonString);

                entryRepository = new EntryRepository(this);

            } catch (Exception ex) {
                Toast.makeText(this, "There was a problem", Toast.LENGTH_SHORT).show();
            }

        }

    }

    @Override
    public void onDialogPositiveClick(AddFoodDialogFragment dialog) {
        SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-DD HH:MM:SS.SSS");
        String time = df.format(Calendar.getInstance().getTime());
        if (imageName == null) {
            imageName = time + ".jpg";
            imageName = saveBitmap(croppedBitmap, imageName);
        }
        Entry entry = new Entry(dialog.getCode(), time, dialog.getAmount(), imageName);
        entryRepository.addEntry(entry);
        Toast.makeText(this, "Added to diary", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDialogNegativeClick(AddFoodDialogFragment dialog) {
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private String saveBitmap(Bitmap bitmap, String imageName) {
        ContextWrapper cw = new ContextWrapper(this);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File file = new File(directory, imageName);
        if (!file.exists()) {
            try {
                OutputStream outStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Log.d(TAG, "Saved " + imageName);
        return imageName;
    }


    private Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            matrix.postRotate(applyRotation);
        }

        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }


    private class ClassifyImageTask extends AsyncTask<Bitmap, Void, List> {
        @Override
        protected List doInBackground(Bitmap... bitmap) {
            Classifier classifier =
                    TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
            List<Classifier.Recognition> results = classifier.recognizeImage(bitmap[0]);

            return results;
        }

        @Override
        protected void onPostExecute(List results) {
            View loadingPanel = findViewById(R.id.loading_panel);
            loadingPanel.setVisibility(View.GONE);
            try {
                for (Object res : results) {
                    Classifier.Recognition recognition = (Classifier.Recognition) res;
                    foodInfos.add(getFoodInfoFromName(recognition.getTitle()));
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            adapter.notifyDataSetChanged();
        }

        Food getFoodInfoFromName(String foodName) throws JSONException {
            JSONObject foodInfo = jsonReader.getJSONObject(foodName);
            String name = foodInfo.getString("name");
            int serving = foodInfo.getInt("serving");
            int cal = foodInfo.getInt("calories");
            double protein = foodInfo.getDouble("protein");
            double fat = foodInfo.getDouble("fat");
            double carb = foodInfo.getDouble("carb");
            String unit = foodInfo.getString("unit");
            return new Food(foodName, name, unit, serving, cal, protein, fat, carb);
        }
    }

}
