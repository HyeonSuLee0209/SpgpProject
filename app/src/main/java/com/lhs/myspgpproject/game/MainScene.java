package com.lhs.myspgpproject.game;

import android.view.MotionEvent;

import com.lhs.myspgpproject.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Score;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.VertScrollBackground;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class MainScene extends Scene {
    private static final String TAG = MainScene.class.getSimpleName();
    private final Score score;

    public enum Layer {
        bg, block, ui, controller;
        public static final int COUNT = values().length;
    }
    public MainScene() {
        //Metrics.setGameSize(900, 1600); default=900x1600
        initLayers(Layer.COUNT);

        this.score = new Score(R.mipmap.number_24x32, 850f, 50f, 60f);
        score.setScore(0);
        add(Layer.ui, score);
    }
    public void addScore(int amount) {
        score.add(amount);
    }
    public int getScore() {
        return score.getScore();
    }

    // Overridables
}
