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
        private static final int GRID_X = 7;
        private static final int GRID_Y = 7;
        private static boolean isBoard = true;
        private Block[][] grid = new Block[GRID_X][GRID_Y];
        private static final int TYPE_NUMS = 7;
        private static MainScene scene;
        public BlockController(MainScene mainScene) {
            this.scene = mainScene;
            instance = this;
        }

        public void generateBoard() {
            for (int x = 0; x < GRID_X; x++) {
                for (int y = 0; y < GRID_Y; y++) {
                    int type = checkStartBoard(x, y);

                    Block block = Block.get(type, x, y);
                    grid[x][y] = block;
                    scene.add(block);
                }
            }
        }

        // 게임 시작 시 3개 연속으로 있는 것을 제거 ---------------------------------------

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

        // -------------------------------------------------------------------------

        enum GameState {
            IDLE,
            SWAPPING,
            MATCHING,
            DELETING,
            FALLING_AND_GENERATING,
        }
        private GameState gameState = GameState.IDLE;

        @Override
        public void update() {
            if(isBoard) {
                generateBoard();
                isBoard = false;
                return;
            }

            switch (gameState) {
                case IDLE:
                    handleIdle();
                    break;
                case SWAPPING:
                    handleSwapping();
                    break;
                case MATCHING:
                    handleMatching();
                    break;
                case DELETING:
                    deleteBlock(matchedGroups);
                    gameState = GameState.FALLING_AND_GENERATING;
                    break;
                case FALLING_AND_GENERATING:
                    fallBlocks();
                    generateBlock();
                    gameState = GameState.IDLE;
                    break;
                default:
                    break;
            }
        }

        public void handleIdle() {
            if(selectedBlock != null && targetBlock != null) {
                selectedBlock.swapWith(targetBlock);
                gameState = GameState.SWAPPING;
            }
        }

        public void handleSwapping() {
            if(selectedBlock.getState() != Block.State.Swapping &&
                    targetBlock.getState() != Block.State.Swapping) {
                gameState = GameState.MATCHING;
            }
        }

        public void handleMatching() {
            if (findMatches().isEmpty()) {
                undoSwap();
                gameState = GameState.IDLE;
            } else {
                gameState = GameState.DELETING;
            }

            selectedBlock = null;
            targetBlock = null;
        }

        void undoSwap() {
            if (selectedBlock == null || targetBlock == null) return;

            swapGrid(selectedBlock, targetBlock);

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

        private int toGridX(float logicX) {
            return (int)(logicX / (Metrics.width / GRID_Y));
        }

        private int toGridY(float logicY) {
            return (int)((Metrics.height - logicY) / (Block.RAD * 2));
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (gameState != GameState.IDLE) return false;

            float[] pos = Metrics.fromScreen(event.getX(), event.getY());
            float logicX = pos[0];
            float logicY = pos[1];

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    int gridX = toGridX(logicX);
                    int gridY = toGridY(logicY);

                    Log.d(TAG, "x = " + gridX + " y = " + gridY);

                    if (gridX < 0 || gridX >= GRID_X || gridY < 0 || gridY >= GRID_Y) {
                        return false;
                    }

                    selectedBlock = grid[gridX][gridY];


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
                    int selectedGridX  = selectedBlock.getGridX();
                    int selectedGridY  = selectedBlock.getGridY();

                    int targetGridX = selectedGridX;
                    int targetGridY = selectedGridY;

                    switch (direction) {
                        case LEFT:  targetGridX -= 1; break;
                        case RIGHT: targetGridX += 1; break;
                        case UP:    targetGridY += 1; break;
                        case DOWN:  targetGridY -= 1; break;
                        default: return false; // 움직이지 않았으면 리턴
                    }

                    if (targetGridX < 0 || targetGridX >= GRID_X || targetGridY < 0 || targetGridY >= GRID_Y) {
                        return false;
                    }

                    targetBlock = grid[targetGridX][targetGridY];

                    // Grid 참조 먼저 바꿔줌
                    swapGrid(selectedBlock, targetBlock);
                    break;
            }

            return true;
        }

        public void swapGrid(Block a, Block b) {
            int ax = a.getGridX(), ay = a.getGridY();
            int bx = b.getGridX(), by = b.getGridY();
            grid[ax][ay] = b;
            grid[bx][by] = a;
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
            boolean[][] visited = new boolean[GRID_X][GRID_Y];

            // 가로, 세로 모두 탐색
            findMatchesInDirection(true, visited);  // 가로
            findMatchesInDirection(false, visited); // 세로

            categorizeMatches(matchedGroups);
            return matchedGroups;
        }

        private void findMatchesInDirection(boolean horizontal, boolean[][] visited) {
            int outerLimit = horizontal ? GRID_X : GRID_Y;
            int innerLimit = horizontal ? GRID_Y : GRID_X;

            for (int outer = 0; outer < outerLimit; outer++) {
                for (int inner = 0; inner < innerLimit - 2;) {
                    int x = horizontal ? outer : inner;
                    int y = horizontal ? inner : outer;

                    Block current = grid[x][y];
                    if (current == null) {
                        inner++;
                        continue;
                    }

                    int type = current.getType();
                    int matchLen = 1;

                    for (int k = 1; inner + k < innerLimit; k++) {
                        int nx = horizontal ? outer : inner + k;
                        int ny = horizontal ? inner + k : outer;

                        Block next = grid[nx][ny];
                        if (next != null && next.getType() == type) {
                            matchLen++;
                        } else break;
                    }

                    if (matchLen >= 3) {
                        List<Block> match = new ArrayList<>();
                        for (int i = 0; i < matchLen; i++) {
                            int mx = horizontal ? outer : inner + i;
                            int my = horizontal ? inner + i : outer;

                            if (!visited[mx][my]) {
                                match.add(grid[mx][my]);
                                visited[mx][my] = true;
                            }
                        }
                        matchedGroups.add(match);
                    }

                    inner += Math.max(matchLen, 1); // 중복 검사 방지
                }
            }
        }

        private boolean isTShape(List<Block> blocks) {
//        if (blocks.size() == 5) {
//            return true;
//        }
            return false;
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

        private void deleteBlock(List<List<Block>> matchedGroups) {
            for (List<Block> matchGroup : matchedGroups) {
                for(Block b : matchGroup) {
                    scene.remove(b);
                    grid[b.getGridX()][b.getGridY()] = null;
                }
            }
        }
        //-------------------------------------------------------------------------

        // 블록 하강 처리 -----------------------------------------------------------

        private void fallBlocks() {
//            for (int x = 0; x < GRID_X; x++) {
//                for (int y = 0; y < GRID_Y; y++) {
//                    if (grid[x][y] == null) {
//                        int targetGridX = grid[x][y].getGridX();
//                        int targetGridY = grid[x][y].getGridY() + 1;
//
//                    }
//                }
//            }

            targetBlock = null;
        }

        private void generateBlock() {

        }

        //-------------------------------------------------------------------------
    }
