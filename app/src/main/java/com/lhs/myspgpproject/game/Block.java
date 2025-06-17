package com.lhs.myspgpproject.game;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

import com.lhs.myspgpproject.R;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.AnimSprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Block extends AnimSprite implements IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {
    private static final String TAG = Block.class.getSimpleName();

    private static final float RADIUS = 70f;
    public static final float RAD = RADIUS;
    public static final float seletedScale = 1.2f;
    private static final float GRID_X = 7;
    public static final int TYPE_NORMAL_BLOCK_COUNT = 7;
    public static final int TYPE_CLOCK_BOOSTER = 7;
    public static final int TYPE_BOMB_BOOSTER = 8;
    public static final int TYPE_LINE_BOOSTER = 9;

    public static final int TYPE_SPECIAL_BLOCK_START = TYPE_CLOCK_BOOSTER;
    public static final int TOTAL_BLOCK_TYPES = TYPE_LINE_BOOSTER + 1;

    private static final int[] resIds = {
            R.mipmap.block_01, R.mipmap.block_02, R.mipmap.block_03,
            R.mipmap.block_04, R.mipmap.block_05, R.mipmap.block_06,
            R.mipmap.block_07, R.mipmap.clock,
    };
    protected RectF collisionRect = new RectF();
    private int type;
    private int gridX, gridY;
    private float x, y;

    // 블럭 상태 ---------------------------------------------------------------
    public enum State {
        Idle,
        Swapping,
        Dragging
    }
    private State state = State.Idle;
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    // -----------------------------------------------------------------------
    public static Block get(int type, int gridX, int gridY) {
        return Scene.top().getRecyclable(Block.class).init(type, gridX, gridY);
    }
    public Block() {
        super(0, 0, 0);
        Log.v(TAG, "Created Block@ " + System.identityHashCode(this));
    }
    private Block init(int type, int gridX, int gridY) {
        if(type == TYPE_CLOCK_BOOSTER) {
            this.setImageResourceId(resIds[type], 1);
        } else {
            this.setImageResourceId(resIds[type], 10);
        }
        this.type = type;
        this.gridX = gridX;
        this.gridY = gridY;

        this.x = gridXToX(gridX);
        this.y = gridYToY(gridY);

        setPosition(this.x, this.y, RADIUS);

        this.state = State.Idle;
        this.targetX = this.x;
        this.targetY = this.y;

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
            case Dragging:
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
            float moveDist = MOVE_SPEED * GameView.frameTime;
            float ratio = moveDist / distance;

            if (ratio > 1) ratio = 1;

            setPosition(x + dx * ratio, y + dy * ratio, RADIUS);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (y + RADIUS >= (Metrics.height - (BlockController.GRID_Y * RADIUS * 2))) {
            super.draw(canvas);
        }
    }

    private void updateCollisionRect() {
        collisionRect.set(x - RADIUS, y - RADIUS, x + RADIUS, y + RADIUS);
    }

    public RectF getCollisionRect() {
        return collisionRect;
    }

    private float gridXToX(int gridX) {
        return Metrics.width / GRID_X * (gridX + 0.5f);
    }

    private float gridYToY(int gridY) {
        return Metrics.height - (gridY + 0.5f) * RADIUS * 2;
    }

    public int getType() {
        return type;
    }

    // 블록 집고 움직이기 처리용---------------------------------------------
    private float startDragLogicX, startDragLogicY;
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


    public void startDrag(float logicX, float logicY) {
        startDragLogicX = logicX;
        startDragLogicY = logicY;
        setPosition(logicX, logicY, RADIUS * seletedScale);
        setState(State.Dragging);
    }

    public void updateDrag(float logicX, float logicY) {
        float dx = logicX - startDragLogicX;
        float dy = logicY - startDragLogicY;

        // 이동 제한 적용
        dx = Math.max(-DRAG_LIMIT, Math.min(dx, DRAG_LIMIT));
        dy = Math.max(-DRAG_LIMIT, Math.min(dy, DRAG_LIMIT));

        setPosition(startDragLogicX + dx, startDragLogicY + dy, RADIUS * seletedScale);
    }
    public void endDrag() {
        setPosition(gridXToX(gridX), gridYToY(gridY), RADIUS);
        setState(State.Idle);
    }

    // ------------------------------------------------------------------
    
    // 블록 교환-----------------------------------------------------------
    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }


    public void setGridPosition(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
    }

    // ------------------------------------------------------------------

    // 블록 이동 시 처리----------------------------------------------------
    private float targetX, targetY;
    private static final float MOVE_SPEED = 200f; // 초당 이동 픽셀 수

    public float getStartDragLogicX() {
        return startDragLogicX;
    }
    public float getStartDragLogicY() {
        return startDragLogicY;
    }

    public void setTargetPosition(float x, float y) {
        targetX = x;
        targetY = y;
    }

    public void setTargetPositionToGrid() {
        this.setTargetPosition(gridXToX(gridX), gridYToY(gridY));
    }

    public void swapWith(Block other) {
        this.setTargetPosition(other.getX(), other.getY());
        other.setTargetPosition(this.getX(), this.getY());

        this.setState(State.Swapping);
        other.setState(State.Swapping);
    }
    // ------------------------------------------------------------------

    // 블록 재활용 처리----------------------------------------------------

    @Override
    public void onRecycle() {
        this.gridX = -1;
        this.gridY = -1;
        this.type = -1;
        this.state = State.Idle;
        this.x = 0;
        this.y = 0;
        super.setPosition(0, 0, RADIUS);
        this.startDragLogicX = 0;
        this.startDragLogicY = 0;
        this.targetX = 0;
        this.targetY = 0;
        this.dx = 0;
        this.dy = 0;
    }

    @Override
    public MainScene.Layer getLayer() {
        return MainScene.Layer.block;
    }

    // ------------------------------------------------------------------

    // 특수 블록 처리----------------------------------------------------
    public boolean isClockBooster() { return this.type == TYPE_CLOCK_BOOSTER; }
    public boolean isBombBooster() { return this.type == TYPE_BOMB_BOOSTER; }
    public boolean isLineBooster() { return this.type == TYPE_LINE_BOOSTER; }
    public boolean isSpecialBlock() { return this.type >= TYPE_SPECIAL_BLOCK_START; }

    // ------------------------------------------------------------------
}
