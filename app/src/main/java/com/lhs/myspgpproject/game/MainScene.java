package com.lhs.myspgpproject.game;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.MotionEvent;

import com.lhs.myspgpproject.R;
import com.lhs.myspgpproject.game.PauseScene;
import com.lhs.myspgpproject.app.MainActivity;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Button;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Score;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;


public class MainScene extends Scene {
    private static final String TAG = MainScene.class.getSimpleName();
    private final Score score;
    private final LimitTime limitTime; // 60초 제한 시간

    private boolean resetButtonPressed = false;
    private final float resetX = 720f;
    private final float resetY = 65f;
    private final float btnSize = 100f;

    public enum Layer {
        block, ui, controller, touch;
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

        add(Layer.controller, new BlockController(this));

        boolean resetPressedDownInside = false;

        add(Layer.touch, new Button(R.mipmap.reset, resetX, resetY, btnSize, btnSize, new Button.OnTouchListener() {
            @Override
            public boolean onTouch(boolean pressed) {
                new AlertDialog.Builder(GameView.view.getContext())
                        .setTitle("Confirm")
                        .setMessage("Do you really want to reset the game?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                popAll();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        }));

        add(Layer.touch, new Button(R.mipmap.btn_pause, 840f, 65f, btnSize, btnSize, new Button.OnTouchListener() {
            @Override
            public boolean onTouch(boolean pressed) {
                new PauseScene().push();
                return false;
            }
        }));
    }

    public LimitTime getLimitTime() {
        return this.limitTime;
    }

    // Overridables
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(super.onTouchEvent(event)) return true;

        return BlockController.getInstance().onTouchEvent(event);
    }

    @Override
    protected int getTouchLayerIndex() {
        return Layer.touch.ordinal();
    }
}
