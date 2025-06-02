package com.lhs.myspgpproject.game;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.lhs.myspgpproject.R;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.AnimSprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Block extends AnimSprite implements IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {
    private static final float RADIUS = 70f;
    public static final float RAD = RADIUS;
    public static final float seletedScale = 1.2f;
    private static final float HORZ = 7;
    private static final int[] resIds = {
            R.mipmap.block_01, R.mipmap.block_02, R.mipmap.block_03,
            R.mipmap.block_04, R.mipmap.block_05, R.mipmap.block_06,
            R.mipmap.block_07
    };
    protected RectF collisionRect = new RectF();
    private int type;
    private int vert, horz;
    private float x, y;

    // 블럭 상태 ---------------------------------------------------------------
    public enum State {
        Idle,
        Swapping,
        Falling,
    }
    private State state = State.Idle;
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    // -----------------------------------------------------------------------
    public static Block get(int type, int vert, int horz) {
        return Scene.top().getRecyclable(Block.class).init(type, vert, horz);
    }
    public Block() {
        super(0, 0, 0);
    }
    private Block init(int type, int vert, int horz) {
        this.setImageResourceId(resIds[type], 10);
        this.type = type;
        this.vert = vert;
        this.horz = horz;
        this.x = vertToX(vert);
        this.y = horzToY(horz);

        setPosition(vertToX(vert), horzToY(horz), RADIUS);
        return this;
    }

    @Override
    public void update() {
        super.update();
        updateCollisionRect();

        switch (state) {
            case Idle:
                break;
            case Swapping:
                handleSwapping();
                break;
            case Falling:
                handleFalling();
                break;
            default:
                break;
        }
    }

    private static final float SWAPPING_THRESHOLD = 10f;
    private void handleSwapping() {
        float dx = targetX - x;
        float dy = targetY - y;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);

        if (distance < SWAPPING_THRESHOLD) {
            setPosition(targetX, targetY, RADIUS);
            setState(State.Idle);
        } else {
            moveTowardsTarget(dx, dy, distance);
        }
    }

    private void handleFalling() {
    }


    private void moveTowardsTarget(float dx, float dy, float distance) {
        float moveDist = MOVE_SPEED * GameView.frameTime;
        float ratio = moveDist / distance;
        if (ratio > 1) ratio = 1;
        setPosition(x + dx * ratio, y + dy * ratio, RADIUS);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    private void updateCollisionRect() {
        collisionRect.set(x - RADIUS, y - RADIUS, x + RADIUS, y + RADIUS);
    }

    public RectF getCollisionRect() {
        return collisionRect;
    }

    @Override
    public void onRecycle() {
    }

    @Override
    public MainScene.Layer getLayer() {
        return MainScene.Layer.block;
    }

    private float vertToX(int vert) {
        return Metrics.width / HORZ * (vert + 0.5f);
    }

    private float horzToY(int horz) {
        return Metrics.height - (horz + 0.5f) * RADIUS * 2;
    }

    public int getType() {
        return type;
    }

    // 블록 집고 움직이기 처리용---------------------------------------------
    private float startX, startY;
    private static final float DRAG_LIMIT = 40f; // 최대 이동 거리

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @Override
    public void setPosition(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        super.setPosition(x, y, radius);
    }


    public void startDrag(float x, float y) {
        startX = x;
        startY = y;
        setPosition(x, y, RADIUS * seletedScale);
    }

    public void updateDrag(float x, float y) {
        float dx = x - startX;
        float dy = y - startY;

        // 이동 제한 적용
        dx = Math.max(-DRAG_LIMIT, Math.min(dx, DRAG_LIMIT));
        dy = Math.max(-DRAG_LIMIT, Math.min(dy, DRAG_LIMIT));

        setPosition(startX + dx, startY + dy, RADIUS * seletedScale);
    }
    public void endDrag() {
        setPosition(vertToX(vert), horzToY(horz), RADIUS);
    }

    // ------------------------------------------------------------------
    
    // 블록 교환-----------------------------------------------------------
    public int getVert() {
        return vert;
    }

    public int getHorz() {
        return horz;
    }

    public void setGridPosition(int vert, int horz) {
        this.vert = vert;
        this.horz = horz;
    }

    // ------------------------------------------------------------------

    // 블록 이동 시 처리----------------------------------------------------
    private float targetX, targetY;
    private static final float MOVE_SPEED = 200f; // 초당 이동 픽셀 수

    public float getStartX() {
        return startX;
    }
    public float getStartY() {
        return startY;
    }

    public void setTargetPosition(float x, float y) {
        targetX = x;
        targetY = y;
    }

    public void setTargetPositionToGrid() {
        this.setTargetPosition(vertToX(vert), horzToY(horz));
    }

    public void swapWith(Block other) {
        int tempVert = this.vert;
        int tempHorz = this.horz;

        this.setGridPosition(other.vert, other.horz);
        other.setGridPosition(tempVert, tempHorz);

        this.setTargetPositionToGrid();
        other.setTargetPositionToGrid();

        this.setState(State.Swapping);
        other.setState(State.Swapping);
    }
    // ------------------------------------------------------------------
}
