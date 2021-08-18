package com.battlesnake.minimax;

import com.battlesnake.board.Tile;
import com.battlesnake.board.TileType;
import com.battlesnake.data.Move;
import com.battlesnake.data.MovePoint;
import com.battlesnake.data.MoveValue;
import com.battlesnake.data.Snake;
import com.battlesnake.math.ObjectCloner;
import com.battlesnake.math.Point;
import com.battlesnake.pathfinding.Pathfinding;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class Minimax {

    private static final int MIN = -999999;
    private static final int NONE = -50;
    private static final int MAX = 999999;

    // board size
    private int width;
    private int height;
    private final int TILE_WIDTH = 1;
    private final int TILE_HEIGHT = 1;
    private int turn;

    // Time for MinMax
    private long startTime;

    // Pathfinding
    private Pathfinding pathfinding;

    // my snake
    private Snake mySnake;
    private Snake enemy;

    // board data
    private List<Snake> snakes;
    private List<Snake> deadSnakes;
    private List<Point> food;

    private transient Tile[][] board;
    private transient Integer[][] regions;

    public void init(Snake mySnake, int turn) {
        this.turn = turn;
        this.mySnake = mySnake;
        this.pathfinding = new Pathfinding();
        this.enemy = findEnemySnake();
        this.board = updateBoard(this.mySnake, enemy);
        this.regions = fillIn(board, regions, this.mySnake);
    }

    public MoveValue maximize() {
        startTime = System.currentTimeMillis();
        MoveValue move = maximize(board, mySnake, enemy, 0, Minimax.MIN, Minimax.MAX);
        // System.out.println(move.returnMove + ", " + move.returnValue);
        return move;
    }

    public MoveValue maximize(Tile[][] board, Snake player, Snake enemy, int depth, double alpha, double beta) {
        boolean isMaximizing = (depth % 2 == 0);

        int value = evaluate(board, player, enemy);
        if (value == MAX || value == -MIN) return new MoveValue(value);

        MoveValue returnMove;
        MoveValue bestMove = null;

        if (isMaximizing) {

            // check snake state
            List<Move> moves = getPossibleMoves(board, player.getHead(), false);
            if (moves.size() == 0) {
                System.out.println("NO move for ME");
                return new MoveValue(MIN);
            }

            for (Move currentMove : moves) {
                try {
                    Snake tempSnake = (Snake) ObjectCloner.deepCopy(player);
                    Snake tempEnemy = (Snake) ObjectCloner.deepCopy(enemy);
                    tempSnake.applyMove(currentMove, food);
                    Tile[][] tempBoard = updateBoard(tempSnake, tempEnemy);
                    returnMove = maximize(tempBoard, tempSnake, tempEnemy, depth + 1, alpha, beta);

                    if (bestMove == null || returnMove.returnValue > bestMove.returnValue) {
                        bestMove = returnMove;
                        bestMove.returnMove = currentMove;
                        bestMove.returnValue = returnMove.returnValue;
                    }
                    if (returnMove.returnValue > alpha) {
                        alpha = returnMove.returnValue;
                    }
                    if (beta <= alpha) break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {

            if (System.currentTimeMillis() - startTime >= 350 || depth == 5) {
                return new MoveValue(value);
            }

            // check snake state
            List<Move> moves = getPossibleMoves(board, enemy.getHead(), true);
            if (moves.size() == 0) {
                System.out.println("NO move for ENEMY");
                return new MoveValue(MAX);
            }

            for (Move currentMove : moves) {
                try {
                    Snake tempSnake = (Snake) ObjectCloner.deepCopy(enemy);
                    Snake tempPlayer = (Snake) ObjectCloner.deepCopy(player);
                    tempSnake.applyMove(currentMove, food);
                    Tile[][] tempBoard = updateBoard(tempPlayer, tempSnake);
                    returnMove = maximize(tempBoard, tempPlayer, tempSnake, depth + 1, alpha, beta);

                    if (bestMove == null || returnMove.returnValue < bestMove.returnValue) {
                        bestMove = returnMove;
                        bestMove.returnValue = returnMove.returnValue;
                    }
                    if (returnMove.returnValue < beta) {
                        beta = returnMove.returnValue;
                    }
                    if (beta <= alpha) break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return bestMove;
    }

    private int evaluate(Tile[][] board, Snake snake, Snake enemy) {
        int score = 0;
        int enemyReg = 0;
        int playerReg = 0;
        Point head = snake.getHead();

        if (snake.checkCollision(enemy)) {
            if (snake.length() > enemy.length()) {
                score = MAX;
                System.out.println("REALLY GOOD");
                return score;
            }
            score = MIN;
            System.out.println("BAD");
            return score;
        }
//        if (snake.longerThan(enemy)) {
//            for (Point p : findAdjacent(enemy.getHead())) {
//                if (movable(board, p, false)) {
//                    score += 10 / Point.manhattanDistance(snake.getHead(), enemy.getHead());
//                    break;
//                }
//            }
//        }

        Integer[][] regions = new Integer[width][height];
        Integer[][] enemyRegions = new Integer[width][height];
        fillIn(board, regions, snake);
        fillIn(board, enemyRegions, enemy);

        if(board[head.getX()][head.getY()].getTileType() == TileType.FOOD) score += 10;
        score += (5 - Point.manhattanDistance(head, nearestFoodManhattan(head)));


//        for (Map.Entry<Move, Point> move : Move.adjacent(head).entrySet()) {
//            if (movable(board, move.getValue(), true)) {
//                playerReg += regions[move.getValue().getX()][move.getValue().getY()];
//                score += regions[move.getValue().getX()][move.getValue().getY()] / 2;
//            }
//        }
//        for (Map.Entry<Move, Point> move : Move.adjacent(enemy.getHead()).entrySet()) {
//            if (movable(board, move.getValue(), true)) {
//                enemyReg += enemyRegions[move.getValue().getX()][move.getValue().getY()];
//            }
//        }
//
//        Point center = new Point(width / 2, height / 2);
//        score -= Point.distance(head, center) * 2;
//
//        if (playerReg > enemyReg) score += (playerReg - enemyReg);


        return score;
    }

    // Checks if point exist within the bounds of the board
    public boolean exists(Point point) {
        if (point.getX() < 0) return false;
        if (point.getY() < 0) return false;
        if (point.getX() > width - 1) return false;
        if (point.getY() > height - 1) return false;
        return true;
    }

    // Takes in a point and returns all adjacent points
    private List<Point> findAdjacent(Point point) {
        return new ArrayList<>(Move.adjacent(point).values());
    }

    private boolean isFilled(Point point, Tile[][] board, boolean flag) {
        if (!exists(point)) return true;

        if (flag) {
            return board[point.getX()][point.getY()].getTileType() != TileType.EMPTY
                    && board[point.getX()][point.getY()].getTileType() != TileType.FOOD
                    && board[point.getX()][point.getY()].getTileType() != TileType.TAIL
                    && board[point.getX()][point.getY()].getTileType() != TileType.HEADS
                    && board[point.getX()][point.getY()].getTileType() != TileType.FAKE_WALL
                    && board[point.getX()][point.getY()].getTileType() != TileType.ME;
        } else {
            return board[point.getX()][point.getY()].getTileType() != TileType.EMPTY
                    && board[point.getX()][point.getY()].getTileType() != TileType.FOOD
                    && board[point.getX()][point.getY()].getTileType() != TileType.TAIL
                    && board[point.getX()][point.getY()].getTileType() != TileType.HEADS;

        }
    }


    private boolean movable(Tile[][] board, Point point, boolean flag) {
        return !isFilled(point, board, flag);
    }

    private List<Move> getPossibleMoves(Tile[][] board, Point point, boolean flag) {
        List<Move> moves = new ArrayList<>();
        for (Map.Entry<Move, Point> move : Move.adjacent(point).entrySet()) {
            if (movable(board, move.getValue(), flag))
                moves.add(move.getKey());
        }
        return moves;
    }

    private List<MovePoint> getPossibleMoves(Tile[][] board, MovePoint point, boolean excludeDanger) {
        ArrayList<MovePoint> moves = new ArrayList<>();
        Move initial = point.getInitialMove();
        for (Map.Entry<Move, Point> move : Move.adjacent(point.getPoint()).entrySet()) {
            if (movable(board, move.getValue(), excludeDanger)) {
                moves.add(new MovePoint(
                                move.getKey(),
                                move.getValue(),
                                initial != null ? initial : move.getKey()
                        )
                );
            }
        }
        return moves;
    }

    private Integer[][] fillIn(Tile[][] tiles, Integer[][] regions, Snake s) {
        if (regions == null) regions = new Integer[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isFilled(new Point(x, y), tiles, true)) {
                    regions[x][y] = 0;
                }
            }
        }
        for (Snake snake : snakes) {
            if (snake.equals(s) || snake.length() <= 1) continue;
            Point head = snake.getHead();
            Point neck = snake.getBody().get(1);
            Point delta = head.delta(neck);
            for (int i = 1; i <= 2; i++) {
                fill(regions, new Point(head.getX() + delta.getX() * i, head.getY() + delta.getY() * i));
            }
        }
        Exit condition = new Exit() {
            public boolean shouldExit(MovePoint point, Point initial) {
                return false;
            }

            public List<MovePoint> onFailure(List<MovePoint> path) {
                return path;
            }
        };
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (regions[x][y] != null) continue;
                List<MovePoint> region = floodFill(tiles, new Point(x, y), condition, false);
                for (MovePoint point : region) {
                    regions[point.getPoint().getX()][point.getPoint().getY()] = region.size();
                }
            }
        }
        return regions;
    }

    private interface Exit {
        boolean shouldExit(MovePoint point, Point initial);

        List<MovePoint> onFailure(List<MovePoint> path);
    }

    private void fill(Integer[][] regions, Point point) {
        if (!exists(point)) return;
        regions[point.getX()][point.getY()] = 0;
    }

    protected List<MovePoint> floodFill(Tile[][] tiles, Point point, Exit condition, boolean excludeDanger) {
        LinkedList<MovePoint> points = new LinkedList<>();
        ArrayList<MovePoint> list = new ArrayList<>();
        ArrayList<MovePoint> visited = new ArrayList<>();

        MovePoint loopPoint = new MovePoint(null, point, null);
        points.add(loopPoint);
        list.add(loopPoint);
        while (!points.isEmpty()) {
            loopPoint = points.pollFirst();
            visited.add(loopPoint);
            if (condition.shouldExit(loopPoint, point)) {
                return visited;
            }
            List<MovePoint> moves = getPossibleMoves(tiles, loopPoint, excludeDanger);
            for (MovePoint move : moves) {
                move.setLength(loopPoint.getLength() + 1);
                if (list.contains(move)) continue;
                points.add(move);
                list.add(move);
            }
        }
        return condition.onFailure(visited);
    }

    protected int[][] voronoi(Tile[][] board, Snake snake, Snake enemy) {
        int[][] voronoi = new int[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // get distance from each player
                Point tile = new Point(x, y);
                double snakeDist = Point.manhattanDistance(snake.getHead(), tile);
                double enemyDist = Point.manhattanDistance(enemy.getHead(), tile);

                if (snakeDist > enemyDist) {
                    voronoi[x][y] = 1;
                } else if (snakeDist < enemyDist) {
                    voronoi[x][y] = -1;
                } else {
                    voronoi[x][y] = 0;
                }
            }
        }

        return voronoi;
    }

    private Snake findEnemySnake() {
        Snake enemy = null;
        //get max distance
        double distance = 1000;
        for (Snake s : snakes) {
            if (!s.equals(mySnake)) {
                double dist = Point.distance(mySnake.getHead(), s.getHead());
                if (dist < distance) {
                    distance = dist;
                    enemy = s;
                }
            }
        }
        return enemy;
    }

    private Move moveToTile(Tile tile, Point point) {
        Point p = new Point(tile.getX(), tile.getY());
        for (Map.Entry<Move, Point> move : Move.adjacent(point).entrySet()) {
            if (p.equals(move.getValue())) return move.getKey();
        }
        return null;
    }

    private Point nearestFood(Point current) {
        int min = 1000;
        int dist;
        Point found = food.get(0);
        for (Point snack : food) {
            dist = Math.abs((current.getX()) - snack.getX()) + (Math.abs((current.getY()) - snack.getY()));
            dist -= regions[snack.getX()][snack.getY()] / 100;
            if (dist < min) {
                min = dist;
                found = snack;
            }
        }
        if (regions[found.getX()][found.getY()] < mySnake.length() * 2) return null;
        return found;
    }

    private Point nearestFoodManhattan(Point current) {
        double min = 1000;
        double dist;
        Point found = food.get(0);
        for (Point snack : food) {
            dist = Point.manhattanDistance(current, snack);
            if (dist < min) {
                min = dist;
                found = snack;
            }
        }
        return found;
    }

    private Point bestHeadTile(Point head) {
        List<Point> points = new ArrayList<Point>();
        Point tile = null;
        Point center = new Point(width / 2, height / 2);
        for (int x = -1; x <= 1; x++) {
            if (x == 0) continue;
            for (int y = -1; y <= 1; y++) {
                if (y == 0) continue;
                Point curr = new Point(head.getX() + x, head.getY() + y);
                if (movable(board, curr, true))
                    points.add(curr);
            }
        }
        if (points.size() == 0) {
            for (Map.Entry<Move, Point> move : Move.adjacent(head).entrySet()) {
                if (movable(board, move.getValue(), true))
                    points.add(move.getValue());
            }
        }
        for (Point p : points) {
            if (tile == null || Point.distance(p, center) < Point.distance(tile, center)) {
                tile = p;
            }
        }
        return tile;
    }

    public Move findFood(Point current) {
        Point food = nearestFood(current);
        if (food == null || mySnake.distance(food) > enemy.distance(food)) return null;

        List<Tile> path = pathfinding.getRoute(board, regions, current, food);

        if (path.size() <= 1) return null;
        Move move = moveToTile(path.get(path.size() - 2), current);

        return move;
    }

    public Move findTail(Point current) {
        List<Tile> path = pathfinding.getRoute(board, regions, current, mySnake.getTail());
        if (path.size() <= 1) return null;
        Move move = moveToTile(path.get(path.size() - 2), current);

        return move;
    }

    public Move findHead(Point current, Snake enemy) {
        Point cutoff = bestHeadTile(enemy.getHead());
        List<Tile> path = pathfinding.getRoute(board, regions, current, cutoff);
        if (path.size() <= 1) return null;
        Move move = moveToTile(path.get(path.size() - 2), current);

        return move;
    }

    public Move findCenter(Point current) {
        Point center = new Point(board[0].length / 2, board.length / 2);
        List<Tile> path = pathfinding.getRoute(board, regions, current, center);
        if (path.size() <= 1) return null;
        Move move = moveToTile(path.get(path.size() - 2), current);

        return move;
    }

    public Move findExit(Point current) {
        System.out.println("FINDING EXIT");
        int size = 999;
        Move move = null;
        for (Map.Entry<Move, Point> m : Move.adjacent(current).entrySet()) {
            if (movable(board, m.getValue(), true)) {
                Point p = m.getValue();
                if (regions[p.getX()][p.getY()] < size) {
                    size = regions[p.getX()][p.getY()];
                    move = m.getKey();
                }
            }
        }
        if (move == null) return Move.UP;
        return move;
    }

    public int longestSnake() {
        int len = 0;
        for (Snake s : snakes) {
            if (s.length() > len && !s.equals(mySnake)) {
                len = s.length();
            }
        }
        return len;
    }

    private Tile[][] updateBoard(Snake sn, Snake e) {
        Tile[][] board = new Tile[11][11];

        for (int y = 0; y < 11; y++) {
            for (int x = 0; x < 11; x++) {
                board[x][y] = new Tile(TileType.EMPTY, x, y);
            }
        }

        for (Point snack : food) {
            board[snack.getX()][snack.getY()] = new Tile(TileType.FOOD, snack.getX(), snack.getY());
        }

        for (Snake snake : snakes) {
            try {
                if (snake.equals(sn)) snake = (Snake) ObjectCloner.deepCopy(sn);
                else if (snake.equals(e)) snake = (Snake) ObjectCloner.deepCopy(e);
            } catch (Exception exc) {
                exc.printStackTrace();
            }

            List<Point> body = snake.getBody();
            Point head = body.get(0);
            for (int i = 1; i < body.size(); i++) {
                if ((i == body.size() - 1)
                        && body.size() > 1
                        && !snake.justAte()) {
                    board[body.get(i).getX()][body.get(i).getY()] = new Tile(TileType.TAIL, body.get(i).getX(), body.get(i).getY());
                } else {
                    try {
                        board[body.get(i).getX()][body.get(i).getY()] = new Tile(TileType.WALL, body.get(i).getX(), body.get(i).getY());
                    } catch (ArrayIndexOutOfBoundsException arr) {
                        System.out.println("Out of bounds at index: " + i);
                    }
                }
            }

            if (snake.equals(sn)) {
                try {
                    board[head.getX()][head.getY()] = new Tile(TileType.ME, head.getX(), head.getY());
                } catch (ArrayIndexOutOfBoundsException as) {
                    System.out.println("Out of bounds at index: " + head.getX() + ", " + head.getY());
                }
            } else {
                board[head.getX()][head.getY()] = new Tile(TileType.HEADS, head.getX(), head.getY());

                if (!sn.longerThan(snake)) {
                    List<Point> around = findAdjacent(head);
                    for (Point point : around) {
                        if (exists(point)) {
                            if (board[point.getX()][point.getY()].getTileType() == TileType.EMPTY
                                    || board[point.getX()][point.getY()].getTileType() == TileType.FOOD
                                    || board[point.getX()][point.getY()].getTileType() == TileType.TAIL) {
                                board[point.getX()][point.getY()] = new Tile(TileType.FAKE_WALL, point.getX(), point.getY());
                            }
                        }
                    }
                }
            }
        }
        return board;
    }

    private void clearSnake(Snake snake) {
        for (Point p : snake.getBody()) {
            board[p.getX()][p.getY()] = new Tile(TileType.EMPTY, p.getX(), p.getY());
        }
    }

    public void printBoard(Tile[][] board) {
        System.out.println("---------------------------- Turn: " + turn);
        for (int i = height - 1; i >= 0; i--) {
            for (int j = 0; j < width; j++) {
                if (board[j][i].getTileType() == TileType.WALL) System.out.print("W, ");
                if (board[j][i].getTileType() == TileType.ME) System.out.print("M, ");
                if (board[j][i].getTileType() == TileType.EMPTY) System.out.print("E, ");
                if (board[j][i].getTileType() == TileType.HEADS) System.out.print("H, ");
                if (board[j][i].getTileType() == TileType.TAIL) System.out.print("T, ");
                if (board[j][i].getTileType() == TileType.FAKE_WALL) System.out.print("F, ");
                if (board[j][i].getTileType() == TileType.FOOD) System.out.print("X, ");
            }
            System.out.println();
        }
        System.out.println("----------------------------");

    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Snake getMySnake() {
        return mySnake;
    }

    public void setMySnake(Snake mySnake) {
        this.mySnake = mySnake;
    }

    public List<Snake> getSnakes() {
        return snakes;
    }

    public void setSnakes(List<Snake> snakes) {
        this.snakes = snakes;
    }

    public int getTurn() {
        return this.turn;
    }

    @JsonProperty("food")
    public List<Point> getFood() {
        return food;
    }

    public void setFood(List<Point> food) {
        this.food = food;
    }

    public Tile[][] getBoard() {
        return board;
    }

}
