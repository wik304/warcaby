package checkers;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CheckersGame extends Application {
    public static final int TILE_SIZE = 80;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private Tile[][] board = new Tile[WIDTH][HEIGHT];
    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private boolean redTurn = false;
    private Piece capturingPiece = null;
    private final List<Piece> piecesWithCaptures = new ArrayList<>();
    private boolean gameEnded = false;

    private Stage primaryStage;
    private BorderPane root;

    private long turnStartTime;
    private final VBox whiteTimeList = new VBox(5);
    private final VBox redTimeList = new VBox(5);
    private ScrollPane whiteScroll, redScroll;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initializeGame();
        primaryStage.setTitle("Warcaby - JavaFX");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private void initializeGame() {
        redTurn = false;
        capturingPiece = null;
        piecesWithCaptures.clear();
        gameEnded = false;

        tileGroup.getChildren().clear();
        pieceGroup.getChildren().clear();

        board = new Tile[WIDTH][HEIGHT];

        root = new BorderPane();

        Pane boardPane = new Pane();
        boardPane.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE);
        boardPane.getChildren().addAll(tileGroup, pieceGroup);
        root.setCenter(boardPane);

        whiteTimeList.getChildren().clear();
        redTimeList.getChildren().clear();

        whiteScroll = new ScrollPane(whiteTimeList);
        redScroll = new ScrollPane(redTimeList);
        whiteScroll.setFitToWidth(true);
        redScroll.setFitToWidth(true);
        whiteScroll.setPrefHeight(300);
        redScroll.setPrefHeight(300);

        VBox sidebar = new VBox(10,
                new Label("Czas ruchów BIAŁY:"), whiteScroll,
                new Label("Czas ruchów CZERWONY:"), redScroll
        );
        sidebar.setPrefWidth(250);
        sidebar.setStyle("-fx-padding: 20; -fx-background-color: #f0f0f0;");

        root.setRight(sidebar);

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

        updateAvailableCaptures();
        turnStartTime = System.nanoTime();
    }

    private void recordTurnDuration() {
        long endTime = System.nanoTime();
        double durationSeconds = (endTime - turnStartTime) / 1_000_000_000.0;
        String formatted = String.format("Ruch %d: %.3f s",
                (redTurn ? redTimeList.getChildren().size() + 1 : whiteTimeList.getChildren().size() + 1),
                durationSeconds);

        Label entry = new Label(formatted);
        if (redTurn) {
            redTimeList.getChildren().add(entry);
            redScroll.setVvalue(1.0);
        } else {
            whiteTimeList.getChildren().add(entry);
            whiteScroll.setVvalue(1.0);
        }

        turnStartTime = System.nanoTime();
    }

    private void updateAvailableCaptures() {
        piecesWithCaptures.clear();
        PieceType currentTurn = redTurn ? PieceType.RED : PieceType.WHITE;

        if (capturingPiece != null) {
            int x = toBoard(capturingPiece.oldX);
            int y = toBoard(capturingPiece.oldY);
            if (hasAnyCaptures(capturingPiece, x, y)) {
                piecesWithCaptures.add(capturingPiece);
            }
            return;
        }

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Tile tile = board[x][y];
                if (tile.hasPiece() && tile.getPiece().getType() == currentTurn) {
                    Piece p = tile.getPiece();
                    if (hasAnyCaptures(p, x, y)) {
                        piecesWithCaptures.add(p);
                    }
                }
            }
        }
    }

    private boolean canPieceMove(Piece piece) {
        if (gameEnded) {
            return false;
        }

        boolean isPlayerTurn = (piece.getType().isRed && redTurn) || (!piece.getType().isRed && !redTurn);

        if (!isPlayerTurn) {
            return false;
        }

        if (capturingPiece != null) {
            return capturingPiece == piece;
        }

        if (!piecesWithCaptures.isEmpty()) {
            return piecesWithCaptures.contains(piece);
        }

        return true;
    }

    private Piece makePiece(PieceType type, int x, int y) {
        Piece piece = new Piece(type, x, y);

        piece.setOnMousePressed(e -> {
            if (canPieceMove(piece)) {
                piece.mouseX = e.getSceneX() - piece.getLayoutX() - TILE_SIZE / 2.0;
                piece.mouseY = e.getSceneY() - piece.getLayoutY() - TILE_SIZE / 2.0;
                piece.toFront();
            } else {
                e.consume();
            }
        });

        piece.setOnMouseDragged(e -> {
            if (canPieceMove(piece)) {
                double newX = e.getSceneX() - piece.mouseX - TILE_SIZE / 2.0;
                double newY = e.getSceneY() - piece.mouseY - TILE_SIZE / 2.0;
                piece.setLayoutX(newX);
                piece.setLayoutY(newY);
            }
        });

        piece.setOnMouseReleased(_ -> {
            if (!canPieceMove(piece)) {
                piece.abortMove();
                return;
            }

            int newX = toBoard(piece.getLayoutX());
            int newY = toBoard(piece.getLayoutY());

            MoveResult result = tryMove(piece, newX, newY);

            if (result.type == MoveType.NORMAL) {
                if (!piecesWithCaptures.isEmpty() || capturingPiece != null) {
                    piece.abortMove();
                    return;
                }

                makeMove(piece, newX, newY);
                checkAndPromote(piece, newY);
                capturingPiece = null;
                recordTurnDuration(); // Rejestrujemy czas ruchu
                redTurn = !redTurn;
                updateAvailableCaptures();
                checkGameEnd();
                return;
            }

            if (result.type == MoveType.KILL && !result.capturedPieces.isEmpty()) {
                makeMove(piece, newX, newY);
                for (Piece captured : result.capturedPieces) {
                    board[toBoard(captured.oldX)][toBoard(captured.oldY)].setPiece(null);
                    pieceGroup.getChildren().remove(captured);
                }

                checkGameEnd();
                checkAndPromote(piece, newY);

                int currentX = toBoard(piece.oldX);
                int currentY = toBoard(piece.oldY);
                if (hasAnyCaptures(piece, currentX, currentY)) {
                    capturingPiece = piece;
                } else {
                    capturingPiece = null;
                    recordTurnDuration(); // Rejestrujemy czas ruchu
                    redTurn = !redTurn;
                }
                updateAvailableCaptures();
                checkGameEnd();
                return;
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

    private boolean hasAnyCaptures(Piece piece, int x, int y) {
        List<int[]> directions = piece.getMoveDirections();

        for (int[] dir : directions) {
            int maxDistance = piece.isKing ? Math.max(WIDTH, HEIGHT) : 1;

            for (int startDist = 1; startDist <= maxDistance; startDist++) {
                int enemyX = x + dir[0] * startDist;
                int enemyY = y + dir[1] * startDist;

                if (enemyX < 0 || enemyX >= WIDTH || enemyY < 0 || enemyY >= HEIGHT) {
                    break;
                }

                Piece enemyPiece = board[enemyX][enemyY].getPiece();

                if (enemyPiece != null && enemyPiece.getType() == piece.getType()) {
                    break;
                }

                if (enemyPiece != null) {
                    int maxLandingDist = piece.isKing ? Math.max(WIDTH, HEIGHT) : 1;

                    for (int landDist = 1; landDist <= maxLandingDist; landDist++) {
                        int landX = enemyX + dir[0] * landDist;
                        int landY = enemyY + dir[1] * landDist;

                        if (landX < 0 || landX >= WIDTH || landY < 0 || landY >= HEIGHT) {
                            break;
                        }

                        if (board[landX][landY].hasPiece()) {
                            break;
                        }

                        boolean pathClear = true;
                        for (int step = 1; step < landDist; step++) {
                            int checkX = enemyX + dir[0] * step;
                            int checkY = enemyY + dir[1] * step;
                            if (board[checkX][checkY].hasPiece()) {
                                pathClear = false;
                                break;
                            }
                        }

                        if (pathClear) {
                            boolean pathToEnemyClear = true;
                            for (int step = 1; step < startDist; step++) {
                                int checkX = x + dir[0] * step;
                                int checkY = y + dir[1] * step;
                                if (board[checkX][checkY].hasPiece()) {
                                    pathToEnemyClear = false;
                                    break;
                                }
                            }

                            if (pathToEnemyClear) {
                                return true;
                            }
                        }
                    }
                    break;
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

        if (Math.abs(dx) == 1 && Math.abs(dy) == 1) {
            if (piece.isKing || dy == piece.getType().moveDir) {
                if (!piecesWithCaptures.isEmpty() || capturingPiece != null) {
                    return new MoveResult(MoveType.NONE);
                }
                return new MoveResult(MoveType.NORMAL);
            }
            return new MoveResult(MoveType.NONE);
        }

        if (Math.abs(dx) == Math.abs(dy) && Math.abs(dx) > 1) {
            MoveResult captureResult = tryCapture(piece, x0, y0, newX, newY);
            if (captureResult.type == MoveType.KILL) {
                return captureResult;
            }

            if (piece.isKing && piecesWithCaptures.isEmpty() && capturingPiece == null) {
                if (isPathClear(x0, y0, newX, newY)) {
                    return new MoveResult(MoveType.NORMAL);
                }
            }
        }

        return new MoveResult(MoveType.NONE);
    }

    private void checkGameEnd() {
        if (gameEnded) {
            return;
        }

        PieceType currentPlayerType = redTurn ? PieceType.RED : PieceType.WHITE;
        PieceType opponentType = redTurn ? PieceType.WHITE : PieceType.RED;

        boolean opponentHasPieces = false;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (board[x][y].hasPiece() && board[x][y].getPiece().getType() == opponentType) {
                    opponentHasPieces = true;
                    break;
                }
            }
            if (opponentHasPieces) break;
        }

        if (!opponentHasPieces) {
            endGame(currentPlayerType);
            return;
        }

        boolean opponentCanMove = false;

        redTurn = !redTurn;
        updateAvailableCaptures();

        if (!piecesWithCaptures.isEmpty()) {
            opponentCanMove = true;
        } else {
            for (int y = 0; y < HEIGHT && !opponentCanMove; y++) {
                for (int x = 0; x < WIDTH && !opponentCanMove; x++) {
                    if (board[x][y].hasPiece() && board[x][y].getPiece().getType() == opponentType) {
                        Piece piece = board[x][y].getPiece();

                        List<int[]> directions = piece.getNormalMoveDirections();
                        for (int[] dir : directions) {
                            int maxDistance = piece.isKing ? Math.max(WIDTH, HEIGHT) : 1;

                            for (int dist = 1; dist <= maxDistance; dist++) {
                                int newX = x + dir[0] * dist;
                                int newY = y + dir[1] * dist;

                                if (newX >= 0 && newX < WIDTH && newY >= 0 && newY < HEIGHT) {
                                    if (!board[newX][newY].hasPiece() && (newX + newY) % 2 != 0) {
                                        if (piece.isKing && dist > 1) {
                                            if (isPathClear(x, y, newX, newY)) {
                                                opponentCanMove = true;
                                                break;
                                            }
                                        } else {
                                            opponentCanMove = true;
                                            break;
                                        }
                                    }

                                    if (board[newX][newY].hasPiece()) {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        redTurn = !redTurn;
        updateAvailableCaptures();

        if (!opponentCanMove) {
            endGame(currentPlayerType);
        }
    }

    private void endGame(PieceType winner) {
        gameEnded = true;
        String winnerName = winner == PieceType.RED ? "Czerwony" : "Biały";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Koniec gry");
        alert.setHeaderText("Gra zakończona!");
        alert.setContentText("Wygrał gracz: " + winnerName + "\n\nCzy chcesz zagrać ponownie?");

        ButtonType playAgainButton = new ButtonType("Zagraj ponownie");
        ButtonType exitButton = new ButtonType("Wyjście");
        alert.getButtonTypes().setAll(playAgainButton, exitButton);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == playAgainButton) {
            initializeGame();
            primaryStage.setScene(new Scene(root));
        } else {
            primaryStage.close();
        }
    }

    private boolean isPathClear(int x0, int y0, int newX, int newY) {
        int dx = newX - x0;
        int dy = newY - y0;
        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);

        for (int step = 1; step < Math.abs(dx); step++) {
            int checkX = x0 + step * stepX;
            int checkY = y0 + step * stepY;

            if (isInBounds(checkX, checkY) || board[checkX][checkY].hasPiece()) {
                return false;
            }
        }
        return true;
    }

    private MoveResult tryCapture(Piece piece, int startX, int startY, int targetX, int targetY) {
        int dx = targetX - startX;
        int dy = targetY - startY;
        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);

        List<Piece> capturedPieces = new ArrayList<>();

        for (int step = 1; step < Math.abs(dx); step++) {
            int checkX = startX + step * stepX;
            int checkY = startY + step * stepY;

            if (isInBounds(checkX, checkY)) {
                return new MoveResult(MoveType.NONE);
            }

            Piece pieceAtPosition = board[checkX][checkY].getPiece();

            if (pieceAtPosition != null) {
                if (pieceAtPosition.getType() == piece.getType()) {
                    return new MoveResult(MoveType.NONE);
                }
                capturedPieces.add(pieceAtPosition);
            }
        }

        if (!capturedPieces.isEmpty()) {
            if (!piece.isKing && capturedPieces.size() > 1) {
                return new MoveResult(MoveType.NONE);
            }

            if (capturedPieces.size() > 1) {
                for (int i = 0; i < capturedPieces.size() - 1; i++) {
                    Piece p1 = capturedPieces.get(i);
                    Piece p2 = capturedPieces.get(i + 1);
                    int x1 = toBoard(p1.oldX);
                    int y1 = toBoard(p1.oldY);
                    int x2 = toBoard(p2.oldX);
                    int y2 = toBoard(p2.oldY);

                    if (Math.abs(x2 - x1) == 1 && Math.abs(y2 - y1) == 1) {
                        return new MoveResult(MoveType.NONE);
                    }
                }
            }

            return new MoveResult(MoveType.KILL, capturedPieces);
        }

        return new MoveResult(MoveType.NONE);
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
            directions.add(new int[]{1, 1});
            directions.add(new int[]{-1, 1});
            directions.add(new int[]{1, -1});
            directions.add(new int[]{-1, -1});
            return directions;
        }

        public List<int[]> getNormalMoveDirections() {
            List<int[]> directions = new ArrayList<>();
            if (isKing) {
                directions.add(new int[]{1, 1});
                directions.add(new int[]{-1, 1});
                directions.add(new int[]{1, -1});
                directions.add(new int[]{-1, -1});
            } else {
                directions.add(new int[]{1, type.moveDir});
                directions.add(new int[]{-1, type.moveDir});
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