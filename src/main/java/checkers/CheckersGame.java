package checkers;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class CheckersGame extends Application {
    public static final int TILE_SIZE = 80;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];
    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private boolean redTurn = false;
    private Piece capturingPiece = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE);
        root.getChildren().addAll(tileGroup, pieceGroup);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Tile tile = new Tile((x + y) % 2 == 0, x, y);
                board[x][y] = tile;
                tileGroup.getChildren().add(tile);

                Piece piece = null;

                if (y <= 2 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.RED, x, y);
                }

                if (y >= 5 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.WHITE, x, y);
                }

                if (piece != null) {
                    tile.setPiece(piece);
                    pieceGroup.getChildren().add(piece);
                }
            }
        }

        primaryStage.setTitle("Warcaby - JavaFX");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private Piece makePiece(PieceType type, int x, int y) {
        Piece piece = new Piece(type, x, y);

        piece.setOnMousePressed(e -> {
            boolean isPlayerTurn = (piece.getType().isRed && redTurn) || (!piece.getType().isRed && !redTurn);

            boolean canMove = isPlayerTurn && (capturingPiece == null || capturingPiece == piece);

            if (canMove) {
                piece.mouseX = e.getSceneX() - piece.getLayoutX() - TILE_SIZE / 2.0;
                piece.mouseY = e.getSceneY() - piece.getLayoutY() - TILE_SIZE / 2.0;
                piece.toFront();
            } else {
                e.consume();
            }
        });

        piece.setOnMouseDragged(e -> {
            boolean isPlayerTurn = (piece.getType().isRed && redTurn) || (!piece.getType().isRed && !redTurn);
            boolean canMove = isPlayerTurn && (capturingPiece == null || capturingPiece == piece);

            if (canMove) {
                double newX = e.getSceneX() - piece.mouseX - TILE_SIZE / 2.0;
                double newY = e.getSceneY() - piece.mouseY - TILE_SIZE / 2.0;
                piece.setLayoutX(newX);
                piece.setLayoutY(newY);
            }
        });

        piece.setOnMouseReleased(_ -> {
            boolean isPlayerTurn = (piece.getType().isRed && redTurn) || (!piece.getType().isRed && !redTurn);
            boolean canMove = isPlayerTurn && (capturingPiece == null || capturingPiece == piece);

            if (!canMove) {
                piece.abortMove();
                return;
            }

            int newX = toBoard(piece.getLayoutX());
            int newY = toBoard(piece.getLayoutY());

            int startX = toBoard(piece.oldX);
            int startY = toBoard(piece.oldY);

            MoveResult result = tryMove(piece, newX, newY);

            if (result.type == MoveType.NORMAL) {
                if (hasMandatoryCapture(piece.getType()) || capturingPiece != null) {
                    piece.abortMove();
                    return;
                }

                makeMove(piece, newX, newY);
                capturingPiece = null;
                redTurn = !redTurn;
                return;
            }

            if (result.type == MoveType.KILL && !result.capturedPieces.isEmpty()) {
                makeMove(piece, newX, newY);
                for (Piece captured : result.capturedPieces) {
                    board[toBoard(captured.oldX)][toBoard(captured.oldY)].setPiece(null);
                    pieceGroup.getChildren().remove(captured);
                }

                checkAndPromote(piece, newY);
                capturingPiece = null;
                redTurn = !redTurn;
                return;
            }

            List<CaptureSequence> sequences = findAllCaptureSequences(piece, startX, startY);

            for (CaptureSequence seq : sequences) {
                int[] last = seq.positions.getLast();
                if (last[0] == newX && last[1] == newY) {
                    makeMove(piece, newX, newY);
                    for (Piece captured : seq.allCapturedPieces) {
                        board[toBoard(captured.oldX)][toBoard(captured.oldY)].setPiece(null);
                        pieceGroup.getChildren().remove(captured);
                    }

                    checkAndPromote(piece, newY);
                    capturingPiece = null;
                    redTurn = !redTurn;
                    return;
                }
            }

            piece.abortMove();
        });

        return piece;
    }

    private void checkAndPromote(Piece piece, int newY) {
        if (!piece.isKing) {
            if ((piece.getType() == PieceType.RED && newY == HEIGHT - 1) ||
                    (piece.getType() == PieceType.WHITE && newY == 0)) {
                piece.makeKing();
            }
        }
    }

    private Tile[][] createBoardCopy() {
        Tile[][] copy = new Tile[WIDTH][HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                copy[x][y] = new Tile((x + y) % 2 == 0, x, y);
                if (board[x][y].hasPiece()) {
                    copy[x][y].setPiece(board[x][y].getPiece());
                }
            }
        }
        return copy;
    }

    private void copyBoardState(Tile[][] source, Tile[][] target) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (source[x][y].hasPiece()) {
                    target[x][y].setPiece(source[x][y].getPiece());
                } else {
                    target[x][y].setPiece(null);
                }
            }
        }
    }

    private MoveResult tryMultipleCaptureOnBoard(Piece piece, int x0, int y0, int newX, int newY, Tile[][] boardCopy) {
        return getMoveResult(piece, x0, y0, newX, newY, boardCopy);
    }

    private boolean hasMandatoryCapture(PieceType currentTurn) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Tile tile = board[x][y];
                if (tile.hasPiece() && tile.getPiece().getType() == currentTurn) {
                    Piece p = tile.getPiece();
                    if (hasAnyCaptures(p, x, y)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasAnyCaptures(Piece piece, int x, int y) {
        int[][] directions = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

        for (int[] dir : directions) {
            for (int distance = 2; distance <= Math.max(WIDTH, HEIGHT); distance++) {
                int targetX = x + dir[0] * distance;
                int targetY = y + dir[1] * distance;

                if (isInBounds(targetX, targetY) || board[targetX][targetY].hasPiece()) {
                    break;
                }

                MoveResult result = tryMultipleCapture(piece, x, y, targetX, targetY);
                if (result.type == MoveType.KILL) {
                    return true;
                }
            }
        }
        return false;
    }

    private void makeMove(Piece piece, int newX, int newY) {
        int x0 = toBoard(piece.oldX);
        int y0 = toBoard(piece.oldY);
        board[x0][y0].setPiece(null);
        piece.move(newX, newY);
        board[newX][newY].setPiece(piece);
    }

    private boolean isInBounds(int x, int y) {
        return x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT;
    }

    private MoveResult tryMove(Piece piece, int newX, int newY) {
        if (isInBounds(newX, newY)) return new MoveResult(MoveType.NONE);
        if (board[newX][newY].hasPiece() || (newX + newY) % 2 == 0) return new MoveResult(MoveType.NONE);

        int x0 = toBoard(piece.oldX);
        int y0 = toBoard(piece.oldY);
        int dx = newX - x0;
        int dy = newY - y0;

        if (Math.abs(dx) == 1 && Math.abs(dy) == 1 && (dy == piece.getType().moveDir || piece.isKing)) {
            if (hasMandatoryCapture(piece.getType()) || capturingPiece != null) {
                return new MoveResult(MoveType.NONE);
            }
            return new MoveResult(MoveType.NORMAL);
        }

        if (Math.abs(dx) == Math.abs(dy) && Math.abs(dx) > 1) {
            return trySequentialCapture(piece, x0, y0, newX, newY);
        }

        return new MoveResult(MoveType.NONE);
    }

    private MoveResult trySequentialCapture(Piece piece, int startX, int startY, int targetX, int targetY) {
        List<CaptureSequence> sequences = findAllCaptureSequences(piece, startX, startY);

        for (CaptureSequence seq : sequences) {
            int[] lastPos = seq.positions.getLast();
            if (lastPos[0] == targetX && lastPos[1] == targetY) {
                return new MoveResult(MoveType.KILL, seq.allCapturedPieces);
            }
        }

        return new MoveResult(MoveType.NONE);
    }

    private List<CaptureSequence> findAllCaptureSequences(Piece piece, int startX, int startY) {
        List<CaptureSequence> allSequences = new ArrayList<>();
        CaptureSequence initialSequence = new CaptureSequence();
        initialSequence.positions.add(new int[]{startX, startY});

        findCaptureSequencesRecursive(piece, startX, startY, initialSequence, allSequences, createBoardCopy());
        int maxCaptures = 0;
        for (CaptureSequence seq : allSequences) {
            maxCaptures = Math.max(maxCaptures, seq.allCapturedPieces.size());
        }

        List<CaptureSequence> longestSequences = new ArrayList<>();
        for (CaptureSequence seq : allSequences) {
            if (seq.allCapturedPieces.size() == maxCaptures) {
                longestSequences.add(seq);
            }
        }

        return longestSequences;
    }

    private void findCaptureSequencesRecursive(Piece piece, int currentX, int currentY,
                                               CaptureSequence currentSequence, List<CaptureSequence> allSequences,
                                               Tile[][] boardCopy) {
        boolean foundCapture = false;
        List<int[]> directions = piece.getMoveDirections();

        for (int[] dir : directions) {
            for (int distance = 2; distance <= Math.max(WIDTH, HEIGHT); distance++) {
                int targetX = currentX + dir[0] * distance;
                int targetY = currentY + dir[1] * distance;

                if (isInBounds(targetX, targetY) || boardCopy[targetX][targetY].hasPiece()) {
                    break;
                }

                MoveResult captureResult = tryMultipleCaptureOnBoard(piece, currentX, currentY, targetX, targetY, boardCopy);
                if (captureResult.type == MoveType.KILL) {
                    foundCapture = true;

                    CaptureSequence newSequence = new CaptureSequence(currentSequence);
                    newSequence.positions.add(new int[]{targetX, targetY});
                    newSequence.allCapturedPieces.addAll(captureResult.capturedPieces);

                    Tile[][] newBoardCopy = createBoardCopy();
                    copyBoardState(boardCopy, newBoardCopy);

                    for (Piece capturedPiece : captureResult.capturedPieces) {
                        int capturedX = toBoard(capturedPiece.oldX);
                        int capturedY = toBoard(capturedPiece.oldY);
                        newBoardCopy[capturedX][capturedY].setPiece(null);
                    }

                    findCaptureSequencesRecursive(piece, targetX, targetY, newSequence, allSequences, newBoardCopy);
                }
            }
        }

        if (!foundCapture && !currentSequence.allCapturedPieces.isEmpty()) {
            allSequences.add(new CaptureSequence(currentSequence));
        }
    }

    private static class CaptureSequence {
        List<int[]> positions = new ArrayList<>();
        List<Piece> allCapturedPieces = new ArrayList<>();

        CaptureSequence() {}

        CaptureSequence(CaptureSequence other) {
            for (int[] pos : other.positions) {
                this.positions.add(new int[]{pos[0], pos[1]});
            }
            this.allCapturedPieces.addAll(other.allCapturedPieces);
        }
    }

    private MoveResult tryMultipleCapture(Piece piece, int x0, int y0, int newX, int newY) {
        return getMoveResult(piece, x0, y0, newX, newY, board);
    }

    private MoveResult getMoveResult(Piece piece, int x0, int y0, int newX, int newY, Tile[][] board) {
        int dx = newX - x0;
        int dy = newY - y0;
        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);

        List<Piece> capturedPieces = new ArrayList<>();
        boolean lastWasEmpty = true;

        for (int step = 1; step < Math.abs(dx); step++) {
            int checkX = x0 + step * stepX;
            int checkY = y0 + step * stepY;

            if (isInBounds(checkX, checkY)) {
                return new MoveResult(MoveType.NONE);
            }

            Piece pieceAtPosition = board[checkX][checkY].getPiece();

            if (pieceAtPosition != null) {
                if (pieceAtPosition.getType() == piece.getType()) {
                    return new MoveResult(MoveType.NONE);
                }

                if (!lastWasEmpty) {
                    return new MoveResult(MoveType.NONE);
                }

                capturedPieces.add(pieceAtPosition);
                lastWasEmpty = false;
            } else {
                lastWasEmpty = true;
            }
        }

        if (capturedPieces.isEmpty()) {
            return new MoveResult(MoveType.NONE);
        }

        return new MoveResult(MoveType.KILL, capturedPieces);
    }

    private int toBoard(double pixel) {
        return (int)(pixel + (double) TILE_SIZE / 2) / TILE_SIZE;
    }

    private enum PieceType {
        RED(true, 1), WHITE(false, -1);

        final boolean isRed;
        final int moveDir;

        PieceType(boolean isRed, int moveDir) {
            this.isRed = isRed;
            this.moveDir = moveDir;
        }
    }

    private enum MoveType {
        NONE, NORMAL, KILL
    }

    private static class MoveResult {
        final MoveType type;
        final Piece piece;
        final List<Piece> capturedPieces;

        MoveResult(MoveType type) {
            this(type, (Piece) null);
        }

        MoveResult(MoveType type, Piece piece) {
            this.type = type;
            this.piece = piece;
            this.capturedPieces = piece != null ? List.of(piece) : new ArrayList<>();
        }

        MoveResult(MoveType type, List<Piece> capturedPieces) {
            this.type = type;
            this.piece = capturedPieces.isEmpty() ? null : capturedPieces.getFirst();
            this.capturedPieces = new ArrayList<>(capturedPieces);
        }
    }

    private static class Tile extends StackPane {
        private Piece piece;

        public boolean hasPiece() {
            return piece != null;
        }

        public Piece getPiece() {
            return piece;
        }

        public void setPiece(Piece piece) {
            this.piece = piece;
        }

        Tile(boolean light, int x, int y) {
            setWidth(TILE_SIZE);
            setHeight(TILE_SIZE);
            relocate(x * TILE_SIZE, y * TILE_SIZE);

            Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
            rect.setFill(light ? Color.BEIGE : Color.BROWN);
            getChildren().add(rect);
        }
    }

    private static class Piece extends Group {
        private final PieceType type;
        private boolean isKing = false;
        private double mouseX, mouseY;
        private double oldX, oldY;

        public PieceType getType() {
            return type;
        }

        public void move(int x, int y) {
            oldX = x * TILE_SIZE;
            oldY = y * TILE_SIZE;
            setLayoutX(oldX);
            setLayoutY(oldY);
        }

        public void abortMove() {
            setLayoutX(oldX);
            setLayoutY(oldY);
        }

        public void makeKing() {
            isKing = true;
            Circle crown = new Circle(TILE_SIZE * 0.15);
            crown.setFill(Color.GOLD);
            crown.setTranslateX((double) TILE_SIZE / 2);
            crown.setTranslateY((double) TILE_SIZE / 2);
            getChildren().add(crown);
        }

        public List<int[]> getMoveDirections() {
            List<int[]> directions = new ArrayList<>();
            if (isKing) {
                directions.add(new int[]{1, 1});
                directions.add(new int[]{-1, 1});
                directions.add(new int[]{1, -1});
                directions.add(new int[]{-1, -1});
            } else {
                directions.add(new int[]{1, 1});
                directions.add(new int[]{-1, 1});
                directions.add(new int[]{1, -1});
                directions.add(new int[]{-1, -1});
            }
            return directions;
        }

        Piece(PieceType type, int x, int y) {
            this.type = type;

            move(x, y);

            Circle circle = new Circle(TILE_SIZE * 0.4);
            circle.setFill(type == PieceType.RED ? Color.RED : Color.WHITE);
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(2);
            circle.setTranslateX((double) TILE_SIZE / 2);
            circle.setTranslateY((double) TILE_SIZE / 2);

            getChildren().add(circle);
        }
    }
}