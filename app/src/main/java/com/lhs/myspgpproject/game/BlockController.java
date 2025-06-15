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
        public static final int GRID_X = 7;
        public static final int GRID_Y = 7;
        private static boolean isBoard = true;
        private Block[][] grid = new Block[GRID_X][GRID_Y];
        private static final int TYPE_NUMS = 7;
        private static MainScene scene;

        private final List<Block> pendingDeletions = new ArrayList<>();

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
                    scene.add(block.getLayer(), block);
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
        private boolean isSameAsPrevious(int x, int y, int type) {
            if(y >= 2) {
                if (grid[x][y - 1] != null && grid[x][y - 2] != null) {
                    if (grid[x][y - 1].getType() == type && grid[x][y - 2].getType() == type)
                        return true;
                }
            }

            if(x >= 2) {
                if (grid[x - 1][y] != null && grid[x - 2][y] != null) {
                    if (grid[x - 1][y].getType() == type && grid[x - 2][y].getType() == type)
                        return true;
                }
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
            processPendingDeletions();

            if(isBoard) {
                generateBoard();
                isBoard = false;
                gameState = GameState.MATCHING;
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
                    collectBlocksForDeletion(matchedGroups);
                    gameState = GameState.FALLING_AND_GENERATING;
                    break;
                case FALLING_AND_GENERATING:
                    fallBlocks();
                    generateBlock();

                    if(allBlocksIdle()) {
                        gameState = GameState.MATCHING;
                    }
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
            if(selectedBlock == null || targetBlock == null) {
                gameState = GameState.IDLE;
                selectedBlock = null;
                targetBlock = null;
                return;
            }

            if(selectedBlock.getState() != Block.State.Swapping &&
                    targetBlock.getState() != Block.State.Swapping) {
                gameState = GameState.MATCHING;
            }
        }

        public void handleMatching() {
            if (findMatches().isEmpty()) {
                if(selectedBlock != null && targetBlock != null) {
                    undoSwap();
                }
                gameState = GameState.IDLE;
            } else {
                gameState = GameState.DELETING;
            }

            selectedBlock = null;
            targetBlock = null;
        }

        private boolean allBlocksIdle() {
            for (int x = 0; x < GRID_X; x++) {
                for (int y = 0; y < GRID_Y; y++) {
                    Block block = grid[x][y];
                    if (block != null && block.getState() != Block.State.Idle) {
                        return false;
                    }
                }
            }
            return true;
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
            return (int)(logicX / (Metrics.width / GRID_X));
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

                    if(selectedBlock == null) return false;

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

                    float startX = selectedBlock.getStartDragLogicX();
                    float startY = selectedBlock.getStartDragLogicY();

                    float dx = upLogicX - startX;
                    float dy = upLogicY - startY;
                    float dragDistance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (dragDistance < SWAP_TRIGGER_DISTANCE) {
                        selectedBlock = null;
                        return false;
                    }

                    direction = getSwipeDirection(startX, startY, upLogicX, upLogicY);
                    int selectedGridX  = selectedBlock.getGridX();
                    int selectedGridY  = selectedBlock.getGridY();

                    int targetGridX = selectedGridX;
                    int targetGridY = selectedGridY;

                    switch (direction) {
                        case LEFT:  targetGridX -= 1; break;
                        case RIGHT: targetGridX += 1; break;
                        case UP:    targetGridY += 1; break;
                        case DOWN:  targetGridY -= 1; break;
                        default:
                            selectedBlock = null;
                            return false; // 움직이지 않았으면 리턴
                    }

                    if (targetGridX < 0 || targetGridX >= GRID_X || targetGridY < 0 || targetGridY >= GRID_Y) {
                        selectedBlock = null;
                        return false;
                    }

                    targetBlock = grid[targetGridX][targetGridY];

                    if(targetBlock == null) {
                        selectedBlock = null;
                        return false;
                    }

                    swapGrid(selectedBlock, targetBlock);
                    selectedBlock.swapWith(targetBlock);
                    gameState = GameState.SWAPPING;
                    break;
            }

            return true;
        }

        public void swapGrid(Block a, Block b) {
            int ax = a.getGridX(), ay = a.getGridY();
            int bx = b.getGridX(), by = b.getGridY();
            grid[ax][ay] = b;
            grid[bx][by] = a;

            a.setGridPosition(bx, by);
            b.setGridPosition(ax, ay);
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
            findMatchesInDirection(true);  // 가로
            findMatchesInDirection(false); // 세로

            categorizeMatches(matchedGroups);
            return matchedGroups;
        }

        private void findMatchesInDirection(boolean horizontal) {
            int outerLimit = horizontal ? GRID_X : GRID_Y;
            int innerLimit = horizontal ? GRID_Y : GRID_X;
            boolean[][] visited = new boolean[GRID_X][GRID_Y];

            for (int outer = 0; outer < outerLimit; outer++) {
                int count = 0;
                int lastType = -1;
                List<Block> buffer = new ArrayList<>();

                for (int inner = 0; inner < innerLimit; inner++) {
                    int x = horizontal ? outer : inner;
                    int y = horizontal ? inner : outer;

                    Block block = grid[x][y];
                    int type = (block != null) ? block.getType() : -1;

                    if (type == lastType && block != null) {
                        buffer.add(block);
                        count++;
                    } else {
                        if (count >= 3) {
                            addIfNotVisited(buffer, visited);
                        }
                        buffer.clear();
                        if (block != null) buffer.add(block);
                        count = 1;
                        lastType = type;
                    }
                }

                // 마지막에도 체크
                if (count >= 3) {
                    addIfNotVisited(buffer, visited);
                }
            }
        }

        private void addIfNotVisited(List<Block> blocks, boolean[][] visited) {
            List<Block> group = new ArrayList<>();
            for (Block b : blocks) {
                int x = b.getGridX();
                int y = b.getGridY();
                if (!visited[x][y]) {
                    group.add(b);
                    visited[x][y] = true;
                }
            }
            if (group.size() >= 3) {
                matchedGroups.add(group);
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

        private void collectBlocksForDeletion(List<List<Block>> matchedGroups) {
            for (List<Block> matchGroup : matchedGroups) {
                for(Block b : matchGroup) {
                    if (b != null && !pendingDeletions.contains(b)) {
                        pendingDeletions.add(b);
                    }
                    if (b != null) {
                        grid[b.getGridX()][b.getGridY()] = null;
                    }
                }
            }
        }

        private void processPendingDeletions() {
            if (pendingDeletions.isEmpty()) {
                return;
            }
            // Log.d(TAG, "Processing " + pendingDeletions.size() + " pending deletions.");
            for (Block b : pendingDeletions) {
                if (b != null) {
                    scene.remove(MainScene.Layer.block, b);
                }
            }
            pendingDeletions.clear();
        }
        //-------------------------------------------------------------------------

        // 블록 하강 처리 -----------------------------------------------------------

        private void fallBlocks() {
            for (int x = 0; x < GRID_X; x++) {
                int emptyCount = 0;

                for (int y = 0; y < GRID_Y; y++) {
                    if (grid[x][y] == null) {
                        emptyCount++;
                    }
                }

                if(emptyCount > 0) {
                    int currentFillY = -1;

                    for(int y = 0; y < GRID_Y; y++) {
                        if (grid[x][y] == null) {
                            currentFillY = y;
                            break;
                        }
                    }

                    if (currentFillY == -1) continue;

                    for (int y = currentFillY + 1; y < GRID_Y; y++) {
                        if (grid[x][y] != null) {
                            Block fallingBlock = grid[x][y];

                            grid[x][currentFillY] = fallingBlock;
                            grid[x][y] = null;

                            fallingBlock.setGridPosition(x, currentFillY);

                            fallingBlock.setTargetPositionToGrid();
                            fallingBlock.setState(Block.State.Swapping);

                            currentFillY++;
                        }
                    }
                }
            }
        }

        private void generateBlock() {
            for (int x = 0; x < GRID_X; x++) {
                int emptyCountInColumn = 0;
                for(int y = 0; y < GRID_Y; y++) {
                    if (grid[x][y] == null) {
                        emptyCountInColumn++;
                    }
                }

                if (emptyCountInColumn > 0) {
                    int currentSpawnCount = 0;

                    for (int y = 0; y < GRID_Y; y++) {
                        if (grid[x][y] == null) {
                            int type = random.nextInt(TYPE_NUMS);
                            Block newBlock = Block.get(type, x, y);

                            grid[x][y] = newBlock;
                            scene.add(newBlock.getLayer(), newBlock);

                            newBlock.setTargetPositionToGrid();

                            float initialYPixel = Metrics.height - ( (float)GRID_Y + currentSpawnCount + 0.5f) * Block.RAD * 2;
                            newBlock.setPosition(
                                    Metrics.width / GRID_X * (x + 0.5f),
                                    initialYPixel,
                                    Block.RAD
                            );

                            newBlock.setState(Block.State.Swapping);
                            currentSpawnCount++;
                        }
                    }
                }
            }
        }

        //-------------------------------------------------------------------------
    }
