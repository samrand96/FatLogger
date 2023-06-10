package me.samrand.chefmama;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import me.samrand.chefmama.fragment.DiaryFragment;
import me.samrand.chefmama.fragment.ProgressFragment;

public class DiaryActivity extends AppCompatActivity {
    private final int DIARY_SELECTED = 0;
    private final int PROGRESS_SELECTED = 1;
    int selectedFragment;
    DiaryFragment diaryFragment;
    ProgressFragment progressFragment;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FrameLayout diaryLayout = (FrameLayout) findViewById(R.id.diary);
            FrameLayout progressLayout = (FrameLayout) findViewById(R.id.progress);
            if(item.getItemId() ==  R.id.navigation_diary){
                selectedFragment = DIARY_SELECTED;
                diaryLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
                setSupportActionBar(diaryFragment.getToolbar());
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                return true;
            }
            if(item.getItemId() == R.id.navigation_progress){
                selectedFragment = PROGRESS_SELECTED;
                diaryLayout.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);
                setSupportActionBar(progressFragment.getToolbar());
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                return true;
            }

            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        diaryFragment = DiaryFragment.newInstance();
        progressFragment = ProgressFragment.newInstance();
        transaction.replace(R.id.diary, diaryFragment );
        transaction.replace(R.id.progress, progressFragment);
        transaction.commit();
        selectedFragment = DIARY_SELECTED;

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedFragment == DIARY_SELECTED) {
            setSupportActionBar(diaryFragment.getToolbar());
        }
        else
            setSupportActionBar(progressFragment.getToolbar());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(" ");

    }
    public void toggleCalendar(View view) {
        if (selectedFragment == DIARY_SELECTED) {
            diaryFragment.toggleCalendar(view);
        }

    }
}
