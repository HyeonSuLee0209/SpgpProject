package com.lhs.myspgpproject.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.BitmapPool;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class LimitTime implements IGameObject {
    private final Bitmap bitmap;
    private final float right, top, dstCharWidth, dstCharHeight;
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();
    private final int srcCharWidth, srcCharHeight;
    private int limitTime;
    private float timeAccumulator;

    public LimitTime(int mipmapId, float right, float top, float width) {
        this.bitmap = BitmapPool.get(mipmapId);
        this.right = right;
        this.top = top;
        this.dstCharWidth = width;
        this.srcCharWidth = bitmap.getWidth() / 10;
        this.srcCharHeight = bitmap.getHeight();
        this.dstCharHeight = dstCharWidth * srcCharHeight / srcCharWidth;
    }

    public void setLimitTime(int time) {
        this.limitTime = time;
    }

    @Override
    public void update() {
        timeAccumulator += GameView.frameTime;

        if(timeAccumulator >= 1.0f) {
            limitTime -= 1;
            timeAccumulator = 0.0f;
        }

        if (limitTime <= 0 && callback != null) {
            callback.onTimeOver();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int value = this.limitTime;
        float x = right;

        if (value == 0) {
            srcRect.set(0 * srcCharWidth, 0, (0 + 1) * srcCharWidth, srcCharHeight);
            x -= dstCharWidth;
            dstRect.set(x, top, x + dstCharWidth, top + dstCharHeight);
            canvas.drawBitmap(bitmap, srcRect, dstRect, null);
        } else {
            while (value > 0) {
                int digit = value % 10;
                srcRect.set(digit * srcCharWidth, 0, (digit + 1) * srcCharWidth, srcCharHeight);
                x -= dstCharWidth;
                dstRect.set(x, top, x + dstCharWidth, top + dstCharHeight);
                canvas.drawBitmap(bitmap, srcRect, dstRect, null);
                value /= 10;
            }
        }
    }

    public int getLimitTime() {
        return limitTime;
    }

    // 제한시간 종료 시 
    private TimeOverCallback callback;

    public interface TimeOverCallback {
        void onTimeOver();
    }

    public void setCallback(TimeOverCallback callback) {
        this.callback = callback;
    }

    public void addTime(int seconds) {
        this.limitTime += seconds;
    }
}
