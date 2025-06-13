package com.lhs.myspgpproject.game;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.lhs.myspgpproject.R;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Button;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class PauseScene extends Scene {
    public enum Layer {
        bg, touch
    }

    private final float pauseX, pauseY;
    private final float resumeX, resumeY;

    public PauseScene() {
        initLayers(Layer.values().length);
        float w = Metrics.width, h = Metrics.height;

        this.pauseX = w / 2;
        this.pauseY = h / 2;
        this.resumeX = 200f;
        this.resumeY = 75f;

        add(Layer.bg, new Sprite(R.mipmap.trans_50b, pauseX, pauseY, w, h));
        add(Layer.bg, new Sprite(R.mipmap.bg_city_landscape, pauseX, pauseY, 400, 400f));

        add(Layer.touch, new Button(R.mipmap.btn_resume_n, pauseX, pauseY, resumeX, resumeY, new Button.OnTouchListener() {
            @Override
            public boolean onTouch(boolean pressed) {
                pop();
                return false;
            }
        }));
    }

    @Override
    protected int getTouchLayerIndex() {
        return Layer.touch.ordinal();
    }

    // Overridables
    @Override
    public boolean isTransparent() {
        return true;
    }
}