package com.lhs.myspgpproject.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.lhs.myspgpproject.R;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBtnStartGame(View view) {
        startGame();
    }

    private void startGame() {
        Intent intent = new Intent(this, AnipangActivity.class);
        startActivity(intent);
    }
}