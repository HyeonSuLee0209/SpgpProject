package kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects;

import android.util.Log;
import android.view.MotionEvent;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ITouchable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Button extends Sprite implements ITouchable {
    public interface OnTouchListener {
        public boolean onTouch(boolean pressed);
    }
    protected OnTouchListener listener;
    private static final String TAG = Button.class.getSimpleName();
    public Button(int bitmapResId, float cx, float cy, float width, float height, OnTouchListener listener) {
        super(bitmapResId, cx, cy, width, height);
        this.listener = listener;
    }
    protected boolean captures;
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction();
//        Log.d(TAG, "onTouch:" + this + " action=" + action);

        float[] pts = Metrics.fromScreen(e.getX(), e.getY());
        float x = pts[0], y = pts[1];

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!dstRect.contains(x, y)) {
                    captures = false;
                    return false;
                }
                captures = true;
                return listener.onTouch(true);
            case MotionEvent.ACTION_UP:
                if (captures && dstRect.contains(x, y)) {
                    captures = false;
                    return listener.onTouch(false);
                }
                captures = false;
                return false;
            default:
                return false;
        }
    }
}
