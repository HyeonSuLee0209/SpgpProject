    package com.lhs.myspgpproject.game;

    import android.graphics.Canvas;
    import android.view.MotionEvent;
    import android.util.Log;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.Random;

    import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
    import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Score;
    import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

    public class BlockController implements IGameObject {
        private static final String TAG = BlockController.class.getSimpleName();
        private final Random random = new Random();
        public static final int GRID_X = 7;
        public static final int GRID_Y = 7;
        private static boolean isBoard = true;
        private Block[][] grid = new Block[GRID_X][GRID_Y];
        private boolean boosterCreationPending = false;
        private int nextBoosterType = -1;
        private static MainScene scene;
        private Score gameScoreRef;
        private int comboCount = 0;
        private static final float COMBO_MULTIPLIER_INCREMENT = 0.5f;
        private int totalDeletedBlocks = 0;
        private static final int BLOCKS_FOR_CLOCK = 10;

        private final List<Block> pendingDeletions = new ArrayList<>();

        public BlockController(MainScene mainScene) {
            this.scene = mainScene;
            instance = this;
            this.gameScoreRef = mainScene.getScore();
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
                type = random.nextInt(Block.TYPE_NORMAL_BLOCK_COUNT);
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
            REGENERATING,
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
                    if(comboCount > 0) {
                        comboCount = 0;
                    }
                    if (selectedBlock == null && targetBlock == null) {
                        if (!hasMovePossible()) {
                            gameState = GameState.REGENERATING;
                        }
                    }
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
                case REGENERATING:
                    regenerateBoard();
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
                comboCount++;
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
        private boolean[][] matchedFlags = new boolean[GRID_X][GRID_Y];

        private List<List<Block>> findMatches() {
            matchedGroups.clear();

            for (int x = 0; x < GRID_X; x++) {
                for (int y = 0; y < GRID_Y; y++) {
                    matchedFlags[x][y] = false;
                }
            }
            
            // 가로
            for (int y = 0; y < GRID_Y; y++) {
                List<Block> buffer = new ArrayList<>();
                int currentMatchBaseType = -1;

                for (int x = 0; x < GRID_X; x++) {
                    Block block = grid[x][y];

                    if (block == null) {
                        if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                            markBlocksAsMatched(buffer);
                        }
                        buffer.clear();
                        currentMatchBaseType = -1;
                        continue;
                    }

                    if (!block.isClockBooster()) {
                        if (currentMatchBaseType == -1) {
                            currentMatchBaseType = block.getType();
                        } else if (block.getType() != currentMatchBaseType) {
                            if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                                markBlocksAsMatched(buffer);
                            }
                            buffer.clear();
                            currentMatchBaseType = block.getType();
                            buffer.add(block);
                            continue;
                        }
                    }

                    if (buffer.isEmpty()) {
                        buffer.add(block);
                    } else {
                        if (canBlocksMatch(buffer.get(buffer.size() - 1), block)) {
                            buffer.add(block);
                        } else {
                            if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                                markBlocksAsMatched(buffer);
                            }
                            buffer.clear();
                            buffer.add(block);
                        }
                    }
                }
                if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                    markBlocksAsMatched(buffer);
                }
            }
            
            // 세로
            for (int x = 0; x < GRID_X; x++) {
                List<Block> buffer = new ArrayList<>();
                int currentMatchBaseType = -1;

                for (int y = 0; y < GRID_Y; y++) {
                    Block block = grid[x][y];

                    if (block == null) {
                        if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                            markBlocksAsMatched(buffer);
                        }
                        buffer.clear();
                        currentMatchBaseType = -1;
                        continue;
                    }

                    if (!block.isClockBooster()) {
                        if (currentMatchBaseType == -1) {
                            currentMatchBaseType = block.getType();
                        } else if (block.getType() != currentMatchBaseType) {
                            if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                                markBlocksAsMatched(buffer);
                            }
                            buffer.clear();
                            currentMatchBaseType = block.getType();
                            buffer.add(block);
                            continue;
                        }
                    }

                    if (buffer.isEmpty()) {
                        buffer.add(block);
                    } else {
                        if (canBlocksMatch(buffer.get(buffer.size() - 1), block)) {
                            buffer.add(block);
                        } else {
                            if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                                markBlocksAsMatched(buffer);
                            }
                            buffer.clear();
                            buffer.add(block);
                        }
                    }
                }
                if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                    markBlocksAsMatched(buffer);
                }
            }

            // 최종
            for (int x = 0; x < GRID_X; x++) {
                for (int y = 0; y < GRID_Y; y++) {
                    if (matchedFlags[x][y] && grid[x][y] != null && !isBlockAlreadyInAnyGroup(grid[x][y])) {
                        List<Block> currentMatchGroup = new ArrayList<>();
                        collectConnectedMatchedBlocks(x, y, currentMatchGroup);
                        if (currentMatchGroup.size() >= 3) {
                            matchedGroups.add(currentMatchGroup);
                        }
                    }
                }
            }

            categorizeMatches(matchedGroups);
            return matchedGroups;
        }

        private void markBlocksAsMatched(List<Block> blocks) {
            for (Block b : blocks) {
                if (b != null) {
                    matchedFlags[b.getGridX()][b.getGridY()] = true;
                }
            }
        }

        private void collectConnectedMatchedBlocks(int startX, int startY, List<Block> group) {
            if (startX < 0 || startX >= GRID_X || startY < 0 || startY >= GRID_Y) return;
            if (!matchedFlags[startX][startY] || grid[startX][startY] == null) return;

            Block currentBlock = grid[startX][startY];

            if (group.contains(currentBlock)) return;

            group.add(currentBlock);

            int[] dx = {0, 0, 1, -1};
            int[] dy = {1, -1, 0, 0};

            for (int i = 0; i < 4; i++) {
                int nextX = startX + dx[i];
                int nextY = startY + dy[i];

                if (nextX >= 0 && nextX < GRID_X && nextY >= 0 && nextY < GRID_Y) {
                    Block nextBlock = grid[nextX][nextY];

                    if (nextBlock != null && !group.contains(nextBlock) && matchedFlags[nextX][nextY] &&
                            canBlocksMatch(currentBlock, nextBlock)) {
                        collectConnectedMatchedBlocks(nextX, nextY, group);
                    }
                }
            }
        }

        private boolean isBlockAlreadyInAnyGroup(Block block) {
            for (List<Block> group : matchedGroups) {
                if (group.contains(block)) {
                    return true;
                }
            }
            return false;
        }

        private boolean canBlocksMatch(Block b1, Block b2) {
            if (b1 == null || b2 == null) return false;

            if (b1.isClockBooster() || b2.isClockBooster()) {
                return true;
            }

            return b1.getType() == b2.getType();
        }

        private boolean isMatchingGroup(List<Block> group) {
            if (group.size() < 3) return false;

            List<Integer> nonSpecialBlockTypes = new ArrayList<>();
            for (Block b : group) {
                if (b == null) return false;

                if (!b.isClockBooster()) {
                    nonSpecialBlockTypes.add(b.getType());
                }
            }

            if (nonSpecialBlockTypes.isEmpty()) {
                return false;
            }

            int firstType = nonSpecialBlockTypes.get(0);
            for (int i = 1; i < nonSpecialBlockTypes.size(); i++) {
                if (nonSpecialBlockTypes.get(i) != firstType) {
                    return false;
                }
            }
            return true;
        }

        private boolean isTShape(List<Block> blocks) {
//        if (blocks.size() == 5) {
//            return true;
//        }
            return false;
        }

        private void categorizeMatches(List<List<Block>> matchedGroups) {
            for (List<Block> matchGroup : matchedGroups) {
                Log.d(TAG, matchGroup.size() + "개 매칭");
            }
        }

        private void collectBlocksForDeletion(List<List<Block>> matchedGroups) {
            int scoreToAdd = 0;
            int currentDeletedCount = 0;
            List<Block> blocksToActivateEffect = new ArrayList<>();

            for (List<Block> matchGroup : matchedGroups) {
                int groupSize = matchGroup.size();

                scoreToAdd += 100;

                if (groupSize >= 5) {
                    scoreToAdd += 200;
                } else if (groupSize == 4) {
                    scoreToAdd += 100;
                }

                for(Block b : matchGroup) {
                    if (b != null && !pendingDeletions.contains(b)) {
                        pendingDeletions.add(b);
                        totalDeletedBlocks++;
                        currentDeletedCount++;

                        if (b.isSpecialBlock()) {
                            blocksToActivateEffect.add(b);
                        }
                    }

                    if (b != null) {
                        grid[b.getGridX()][b.getGridY()] = null;
                    }
                }
            }

            activateSpecialBlockEffects(blocksToActivateEffect);

            if (currentDeletedCount > 0 && (totalDeletedBlocks >= BLOCKS_FOR_CLOCK) && !boosterCreationPending) {
                nextBoosterType = Block.TYPE_CLOCK_BOOSTER;
                boosterCreationPending = true;
                totalDeletedBlocks -= BLOCKS_FOR_CLOCK;
            }


            float comboMultiplier = 1.0f;

            if (comboCount > 1) {
                comboMultiplier += (comboCount - 1) * COMBO_MULTIPLIER_INCREMENT;
            }

            int finalScoreToAdd = (int) (scoreToAdd * comboMultiplier);

            if (finalScoreToAdd > 0) {
                gameScoreRef.add(finalScoreToAdd);
            }
        }

        private void activateSpecialBlockEffects(List<Block> specialBlocks) {
            for (Block b : specialBlocks) {
                if (b.isClockBooster()) {
                    scene.getLimitTime().addTime(5); // 5초 증가
                }
            }
        }

        private void processPendingDeletions() {
            if (pendingDeletions.isEmpty()) {
                return;
            }

            for (Block b : pendingDeletions) {
                if (b != null) {
                    scene.remove(MainScene.Layer.block, b);
                    b.onRecycle();
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
                int emptyCountInY = 0;

                for(int y = 0; y < GRID_Y; y++) {
                    if (grid[x][y] == null) {
                        emptyCountInY++;
                    }
                }

                if (emptyCountInY > 0) {
                    int currentSpawnCount = 0;

                    for (int y = 0; y < GRID_Y; y++) {
                        if (grid[x][y] == null) {
                            int type;

                            if (boosterCreationPending && nextBoosterType != -1) {
                                type = nextBoosterType;
                                boosterCreationPending = false;
                                nextBoosterType = -1;
                            } else {
                                type = random.nextInt(Block.TYPE_NORMAL_BLOCK_COUNT);
                            }

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

        private boolean hasMovePossible() {
            for (int x = 0; x < GRID_X; x++) {
                for (int y = 0; y < GRID_Y; y++) {
                    Block currentBlock = grid[x][y];
                    if (currentBlock == null) continue;

                    int[] dx = {0, 0, 1, -1};
                    int[] dy = {1, -1, 0, 0};

                    for (int i = 0; i < 4; i++) {
                        int neighborX = x + dx[i];
                        int neighborY = y + dy[i];

                        if (neighborX < 0 || neighborX >= GRID_X || neighborY < 0 || neighborY >= GRID_Y) {
                            continue;
                        }

                        Block neighborBlock = grid[neighborX][neighborY];
                        if (neighborBlock == null) continue;

                        grid[x][y] = neighborBlock;
                        grid[neighborX][neighborY] = currentBlock;

                        List<List<Block>> foundMatches = findMatchesInSimulation(grid); // <--- 이 부분이 수정된 부분입니다.

                        grid[x][y] = currentBlock;
                        grid[neighborX][neighborY] = neighborBlock;

                        if (!foundMatches.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private List<List<Block>> findMatchesInSimulation(Block[][] currentGrid) {
            List<List<Block>> simulatedMatches = new ArrayList<>();
            boolean[][] simulatedMatchedFlags = new boolean[GRID_X][GRID_Y];

            for (int y = 0; y < GRID_Y; y++) {
                List<Block> buffer = new ArrayList<>();
                for (int x = 0; x < GRID_X; x++) {
                    Block block = currentGrid[x][y];
                    if (block == null || simulatedMatchedFlags[x][y]) {
                        if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                            simulatedMatches.add(new ArrayList<>(buffer));
                            for (Block b : buffer) simulatedMatchedFlags[b.getGridX()][b.getGridY()] = true;
                        }
                        buffer.clear();
                        continue;
                    }

                    if (buffer.isEmpty()) {
                        buffer.add(block);
                    } else {
                        Block firstBlockInGroup = buffer.get(0);
                        if (canBlocksMatch(firstBlockInGroup, block)) {
                            buffer.add(block);
                        } else {
                            if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                                simulatedMatches.add(new ArrayList<>(buffer));
                                for (Block b : buffer) simulatedMatchedFlags[b.getGridX()][b.getGridY()] = true;
                            }
                            buffer.clear();
                            buffer.add(block);
                        }
                    }
                }
                if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                    simulatedMatches.add(new ArrayList<>(buffer));
                    for (Block b : buffer) simulatedMatchedFlags[b.getGridX()][b.getGridY()] = true;
                }
            }

            for (int x = 0; x < GRID_X; x++) {
                List<Block> buffer = new ArrayList<>();
                for (int y = 0; y < GRID_Y; y++) {
                    Block block = currentGrid[x][y];
                    if (block == null || simulatedMatchedFlags[x][y]) {
                        if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                            simulatedMatches.add(new ArrayList<>(buffer));
                            for (Block b : buffer) simulatedMatchedFlags[b.getGridX()][b.getGridY()] = true;
                        }
                        buffer.clear();
                        continue;
                    }

                    if (buffer.isEmpty()) {
                        buffer.add(block);
                    } else {
                        Block firstBlockInGroup = buffer.get(0);
                        if (canBlocksMatch(firstBlockInGroup, block)) {
                            buffer.add(block);
                        } else {
                            if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                                simulatedMatches.add(new ArrayList<>(buffer));
                                for (Block b : buffer) simulatedMatchedFlags[b.getGridX()][b.getGridY()] = true;
                            }
                            buffer.clear();
                            buffer.add(block);
                        }
                    }
                }
                if (buffer.size() >= 3 && isMatchingGroup(buffer)) {
                    simulatedMatches.add(new ArrayList<>(buffer));
                    for (Block b : buffer) simulatedMatchedFlags[b.getGridX()][b.getGridY()] = true;
                }
            }
            return simulatedMatches;
        }

        private void regenerateBoard() {
            for (int x = 0; x < GRID_X; x++) {
                for (int y = 0; y < GRID_Y; y++) {
                    Block block = grid[x][y];
                    if (block != null) {
                        scene.remove(MainScene.Layer.block, block);
                        block.onRecycle();
                        grid[x][y] = null;
                    }
                }
            }

            generateBoard();

            gameState = GameState.MATCHING;
        }
    }
