package com.lhs.myspgpproject.game;

import android.view.MotionEvent;

import com.lhs.myspgpproject.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Score;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;


public class MainScene extends Scene {
    private static final String TAG = MainScene.class.getSimpleName();
    private final Score score;
    private final LimitTime limitTime; // 60초 제한 시간

    private final Reset reset;

    public enum Layer {
        block, ui, controller;
        public static final int COUNT = values().length;
    }
    public MainScene() {
        BlockController.setBoardFlag(true);

//        default=900x1600
        Metrics.setGameSize(900, 1600);
        initLayers(Layer.COUNT);

        this.score = new Score(R.mipmap.number_24x32, 885f, 135f, 60f);
        score.setScore(10);
        add(Layer.ui, score);

        this.limitTime = new LimitTime(R.mipmap.number_24x32, 185f, 150f, 60f);
        limitTime.setLimitTime(120);
        add(Layer.ui, limitTime);

        this.reset = new Reset(R.mipmap.reset, 670f, 15f);
        add(Layer.ui, reset);

        add(Layer.controller, new BlockController(this));
    }

    public LimitTime getLimitTime() {
        return this.limitTime;
    }

    // Overridables
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return BlockController.getInstance().onTouchEvent(event);
    }
}
