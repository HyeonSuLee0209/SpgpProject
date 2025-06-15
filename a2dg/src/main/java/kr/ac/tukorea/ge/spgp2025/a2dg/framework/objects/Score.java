package kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.BitmapPool;

public class Score implements IGameObject {
    private final Bitmap bitmap;
    private final float right, top, dstCharWidth, dstCharHeight;
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();
    private final int srcCharWidth, srcCharHeight;
    private int score, displayScore;

    public Score(int mipmapId, float right, float top, float width) {
        this.bitmap = BitmapPool.get(mipmapId);
        this.right = right;
        this.top = top;
        this.dstCharWidth = width;
        this.srcCharWidth = bitmap.getWidth() / 10;
        this.srcCharHeight = bitmap.getHeight();
        this.dstCharHeight = dstCharWidth * srcCharHeight / srcCharWidth;
    }

    public void setScore(int score) {
        this.score = this.displayScore = score;
    }

    public void add(int amount) {
        score += amount;
    }

    @Override
    public void update() {
        int diff = score - displayScore;
        if (diff == 0) return;
        if (-10 < diff && diff < 0) {
            displayScore--;
        } else if (0 < diff && diff < 10) {
            displayScore++;
        } else {
            displayScore += diff / 10;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int tempValue = this.displayScore;
        float x = right;

        int digitsDrawn = 0;

        for (int i = 0; i < 7; i++) {
            int digit;
            if (tempValue == 0 && i > 0) {
                digit = 0;
            } else {
                digit = tempValue % 10;
                tempValue /= 10;
            }

            srcRect.set(digit * srcCharWidth, 0, (digit + 1) * srcCharWidth, srcCharHeight);

            float drawX = x - dstCharWidth;

            dstRect.set(drawX, top, x, top + dstCharHeight);
            canvas.drawBitmap(bitmap, srcRect, dstRect, null);

            x = drawX;
        }
    }

    public int getScore() {
        return score;
    }
}
