package com.lhs.myspgpproject.app;

import android.os.Bundle;

import com.lhs.myspgpproject.BuildConfig;
import com.lhs.myspgpproject.game.MainScene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.activity.GameActivity;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class AnipangActivity extends GameActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GameView.drawsDebugStuffs = BuildConfig.DEBUG;
        super.onCreate(savedInstanceState);
        new MainScene().push();
    }
}