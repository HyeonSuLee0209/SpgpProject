package com.lhs.myspgpproject.game;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class BlockController implements IGameObject {
    private static final String TAG = BlockController.class.getSimpleName();
    private final Random random = new Random();
    private static final int VERT = 7;
    private static final int HORZ = 7;
    private static boolean isBoard = true;
    private Block[][] grid = new Block[HORZ][VERT];
    private static final int TYPE_NUMS = 7;
    private static MainScene scene;
    public BlockController(MainScene mainScene) {
        this.scene = mainScene;
        instance = this;
    }

    public void generateBoard() {
        for (int horz = 0; horz < HORZ; horz++) {
            for (int vert = 0; vert < VERT; vert++) {
                int type = checkStartBoard(vert, horz);

                Block block = Block.get(type, vert, horz);
                grid[horz][vert] = block;
                scene.add(block);
            }
        }
    }

    public int checkStartBoard(int vert, int horz) {
        int type;
        do {
            type = random.nextInt(TYPE_NUMS);
        } while (isSameAsPrevious(vert, horz, type));

        return type;
    }

    private boolean isSameAsPrevious(int vert, int horz, int type) {
        if (vert >= 2 && grid[horz][vert - 1] != null && grid[horz][vert - 2] != null) {
            if (grid[horz][vert - 1].getType() == type && grid[horz][vert - 2].getType() == type)
                return true;
        }
        if (horz >= 2 && grid[horz - 1][vert] != null && grid[horz - 2][vert] != null) {
            if (grid[horz - 1][vert].getType() == type && grid[horz - 2][vert].getType() == type)
                return true;
        }
        return false;
    }

    @Override
    public void update() {
        if(isBoard) {
            generateBoard();
            isBoard = false;
            return;
        }

        if(!isSwapping || selectedBlock == null || targetBlock == null) {
            return;
        }

        if (selectedBlock.getState() != Block.State.Idle ||
                targetBlock.getState() != Block.State.Idle) {
            return;
        }

        if (findMatches().isEmpty()) {
            undoSwap();
        } else {
            deleteBlock(matchedGroups);
            fallBlocks();
        }
        
        isSwapping = false;
        selectedBlock = null;
        targetBlock = null;
    }

    void undoSwap() {
        int selectedVert  = selectedBlock.getVert();
        int selectedHorz  = selectedBlock.getHorz();

        int targetVert = selectedVert;
        int targetHorz = selectedHorz;

        switch (direction) {
            case LEFT:  targetVert += 1; break;
            case RIGHT: targetVert -= 1; break;
            case UP:    targetHorz -= 1; break;
            case DOWN:  targetHorz += 1; break;
            default: break;
        }

        targetBlock = grid[targetHorz][targetVert];

        grid[selectedHorz][selectedVert] = targetBlock;
        grid[targetHorz][targetVert] = selectedBlock;

        selectedBlock.swapWith(targetBlock);
    }

    @Override
    public void draw(Canvas canvas) {
    }

    private static BlockController instance;
    public static BlockController getInstance() {
        return instance;
    }

    private Block selectedBlock = null;
    private Block targetBlock = null;

    private static final float SWAP_TRIGGER_DISTANCE = 20f;

    private boolean isSwappingInProgress() {
        for (Block[] row : grid) {
            for (Block element : row) {
                if (element.getState() == Block.State.Swapping) {
                    return true;
                }
            }
        }
        return false;
    }

    private int toVert(float logicX) {
        return (int)(logicX / (Metrics.width / VERT));
    }
    private int toHorz(float logicY) {
        return (int)((Metrics.height - logicY) / (Block.RAD * 2));
    }
    private boolean isSwapping = false;
    public boolean onTouchEvent(MotionEvent event) {
        if (isSwappingInProgress()) return false;

        float[] pos = Metrics.fromScreen(event.getX(), event.getY());
        float logicX = pos[0];
        float logicY = pos[1];

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int vert = toVert(logicX);
                int horz = toHorz(logicY);

                if (vert < 0 || vert >= VERT || horz < 0 || horz >= HORZ) {
                    return false;
                }

                selectedBlock = grid[horz][vert];

                selectedBlock.startDrag(logicX, logicY);
                break;
            case MotionEvent.ACTION_MOVE:
                if (selectedBlock == null) return false;

                selectedBlock.updateDrag(logicX, logicY);
                break;
            case MotionEvent.ACTION_UP:
                if (selectedBlock == null) return false;

                selectedBlock.endDrag();

                float upLogicX = pos[0];
                float upLogicY = pos[1];

                float startX = selectedBlock.getStartX();
                float startY = selectedBlock.getStartY();

                float dx = upLogicX - startX;
                float dy = upLogicY - startY;
                float dragDistance = (float) Math.sqrt(dx * dx + dy * dy);

                if (dragDistance < SWAP_TRIGGER_DISTANCE) {
                    selectedBlock = null;
                    return false; // 거리가 짧으면 스왑 안 함
                }

                float centerX = selectedBlock.getX();
                float centerY = selectedBlock.getY();

                direction = getSwipeDirection(centerX, centerY, upLogicX, upLogicY);
                int selectedVert  = selectedBlock.getVert();
                int selectedHorz  = selectedBlock.getHorz();

                int targetVert = selectedVert;
                int targetHorz = selectedHorz;

                switch (direction) {
                    case LEFT:  targetVert -= 1; break;
                    case RIGHT: targetVert += 1; break;
                    case UP:    targetHorz += 1; break;
                    case DOWN:  targetHorz -= 1; break;
                    default: return false; // 움직이지 않았으면 리턴
                }

                if (targetVert < 0 || targetVert >= VERT || targetHorz < 0 || targetHorz >= HORZ) {
                    return false;
                }

                targetBlock = grid[targetHorz][targetVert];

                // Grid 참조 먼저 바꿔줌
                grid[selectedHorz][selectedVert] = targetBlock;
                grid[targetHorz][targetVert] = selectedBlock;

                // 각 블록 상태 갱신 및 위치 애니메이션 적용
                selectedBlock.swapWith(targetBlock);

                // 스와핑 시작 시
                isSwapping = true;
                break;
        }

        return true;
    }

    public static void setBoardFlag(boolean value) {
        isBoard = value;
    }

    private enum Direction {
        LEFT, RIGHT, UP, DOWN, NONE
    }
    private Direction direction = Direction.NONE;
    private Direction getSwipeDirection(float centerX, float centerY, float upX, float upY) {
        float dx = upX - centerX;
        float dy = upY - centerY;

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;

        if (angle >= 315 || angle < 45) {
            return Direction.RIGHT;
        } else if (angle >= 45 && angle < 135) {
            return Direction.DOWN;
        } else if (angle >= 135 && angle < 225) {
            return Direction.LEFT;
        } else {
            return Direction.UP;
        }
    }

    // 매칭 판별 처리 -----------------------------------------------------------
    private final List<List<Block>> matchedGroups = new ArrayList<>();

    private List<List<Block>> findMatches() {
        matchedGroups.clear();
        boolean[][] visited = new boolean[HORZ][VERT];

        // 가로 매칭 탐색
        findHorizontalMatches(matchedGroups, visited);

        // 세로 매칭 탐색
        findVerticalMatches(matchedGroups, visited);

        // 매칭 그룹에 대해 타입을 확인하여 매칭 종류 판별
        categorizeMatches(matchedGroups);

        return matchedGroups; // 겹치는 매칭 병합 없이 매칭된 블록들을 반환
    }
    private void findHorizontalMatches(List<List<Block>> matchedGroups, boolean[][] visited) {
        for (int y = 0; y < HORZ; y++) {
            for (int x = 0; x < VERT - 2; ) {
                int type = grid[y][x].getType();
                int matchLen = countMatchingInRow(y, x, type);

                if (matchLen >= 3) {
                    addMatchToGroup(matchedGroups, visited, y, x, matchLen, true);
                }

                x += Math.max(matchLen, 1);
            }
        }
    }
    private int countMatchingInRow(int y, int x, int type) {
        int matchLen = 1;
        for (int k = x + 1; k < VERT; k++) {
            if (grid[y][k].getType() == type) {
                matchLen++;
            } else break;
        }
        return matchLen;
    }
    private int countMatchingInColumn(int x, int y, int type) {
        int matchLen = 1;
        for (int k = y + 1; k < HORZ; k++) {
            if (grid[k][x].getType() == type) {
                matchLen++;
            } else break;
        }
        return matchLen;
    }
    private void findVerticalMatches(List<List<Block>> matchedGroups, boolean[][] visited) {
        for (int x = 0; x < VERT; x++) {
            for (int y = 0; y < HORZ - 2; ) {
                int type = grid[y][x].getType();
                int matchLen = countMatchingInColumn(x, y, type);

                if (matchLen >= 3) {
                    addMatchToGroup(matchedGroups, visited, y, x, matchLen, false);
                }

                y += Math.max(matchLen, 1);
            }
        }
    }
    private void addMatchToGroup(List<List<Block>> matchedGroups, boolean[][] visited, int y, int x, int matchLen, boolean isRow) {
        List<Block> match = new ArrayList<>();
        for (int i = 0; i < matchLen; i++) {
            if (isRow) {
                if (!visited[y][x + i]) {
                    match.add(grid[y][x + i]);
                    visited[y][x + i] = true;
                }
            } else {
                if (!visited[y + i][x]) {
                    match.add(grid[y + i][x]);
                    visited[y + i][x] = true;
                }
            }
        }
        matchedGroups.add(match);
    }
    private void categorizeMatches(List<List<Block>> matchedGroups) {
        for (List<Block> matchGroup : matchedGroups) {
            if (matchGroup.size() == 3) {
                Log.d(TAG, "3개 매칭");
            } else if (matchGroup.size() == 4) {
                Log.d(TAG, "4개 매칭");
            } else if (isTShape(matchGroup)) {
                Log.d(TAG, "T형 매칭");
            } else if (matchGroup.size() == 5) {
                Log.d(TAG, "5개 매칭");
            }
        }
    }
    private boolean isTShape(List<Block> blocks) {
//        if (blocks.size() == 5) {
//            return true;
//        }
        return false;
    }

    private void deleteBlock(List<List<Block>> matchedGroups) {
        for (List<Block> matchGroup : matchedGroups) {
            for(Block b : matchGroup) {
                scene.remove(b);
                grid[b.getHorz()][b.getVert()] = null;
            }
        }
    }
    //-------------------------------------------------------------------------

    // 블록 하강 처리 -----------------------------------------------------------

    public void fallBlocks() {

    }

    //-------------------------------------------------------------------------
    
}
