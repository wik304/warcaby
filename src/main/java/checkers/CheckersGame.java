package checkers;

import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Optional;

public class CheckersGame {
    public static final int TILE_SIZE = 80;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private Tile[][] board = new Tile[WIDTH][HEIGHT];
    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();
    private GameLogic gameLogic;

    private Stage primaryStage;
    private BorderPane root;

    private long turnStartTime;
    private final VBox whiteTimeList = new VBox(5);
    private final VBox redTimeList = new VBox(5);
    private ScrollPane whiteScroll, redScroll;

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.gameLogic = new GameLogic(board);
        initializeGame();
        primaryStage.setTitle("Warcaby - JavaFX");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public void initializeGame() {
        gameLogic.resetGame();

        tileGroup.getChildren().clear();
        pieceGroup.getChildren().clear();

        board = new Tile[WIDTH][HEIGHT];
        gameLogic.setBoard(board);

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

        gameLogic.updateAvailableCaptures();
        turnStartTime = System.nanoTime();
    }

    public void recordTurnDuration() {
        long endTime = System.nanoTime();
        double durationSeconds = (endTime - turnStartTime) / 1_000_000_000.0;
        String formatted = String.format("Ruch %d: %.3f s",
                (gameLogic.isRedTurn() ? redTimeList.getChildren().size() + 1 : whiteTimeList.getChildren().size() + 1),
                durationSeconds);

        Label entry = new Label(formatted);
        if (gameLogic.isRedTurn()) {
            redTimeList.getChildren().add(entry);
            redScroll.setVvalue(1.0);
        } else {
            whiteTimeList.getChildren().add(entry);
            whiteScroll.setVvalue(1.0);
        }

        turnStartTime = System.nanoTime();
    }

    private Piece makePiece(PieceType type, int x, int y) {
        Piece piece = new Piece(type, x, y);

        piece.setOnMousePressed(e -> {
            if (gameLogic.canPieceMove(piece)) {
                piece.mouseX = e.getSceneX() - piece.getLayoutX() - TILE_SIZE / 2.0;
                piece.mouseY = e.getSceneY() - piece.getLayoutY() - TILE_SIZE / 2.0;
                piece.toFront();
            } else {
                e.consume();
            }
        });

        piece.setOnMouseDragged(e -> {
            if (gameLogic.canPieceMove(piece)) {
                double newX = e.getSceneX() - piece.mouseX - TILE_SIZE / 2.0;
                double newY = e.getSceneY() - piece.mouseY - TILE_SIZE / 2.0;
                piece.setLayoutX(newX);
                piece.setLayoutY(newY);
            }
        });

        piece.setOnMouseReleased(_ -> {
            if (!gameLogic.canPieceMove(piece)) {
                piece.abortMove();
                return;
            }

            int newX = gameLogic.toBoard(piece.getLayoutX());
            int newY = gameLogic.toBoard(piece.getLayoutY());

            MoveResult result = gameLogic.tryMove(piece, newX, newY);

            if (result.type == MoveType.NORMAL) {
                if (!gameLogic.getPiecesWithCaptures().isEmpty() || gameLogic.getCapturingPiece() != null) {
                    piece.abortMove();
                    return;
                }

                gameLogic.makeMove(piece, newX, newY);
                gameLogic.checkAndPromote(piece, newY);
                gameLogic.setCapturingPiece(null);
                recordTurnDuration();
                gameLogic.switchTurn();
                gameLogic.updateAvailableCaptures();
                gameLogic.checkGameEnd(this::endGame);
                return;
            }

            if (result.type == MoveType.KILL && !result.capturedPieces.isEmpty()) {
                gameLogic.makeMove(piece, newX, newY);
                for (Piece captured : result.capturedPieces) {
                    board[gameLogic.toBoard(captured.oldX)][gameLogic.toBoard(captured.oldY)].setPiece(null);
                    pieceGroup.getChildren().remove(captured);
                }

                gameLogic.checkGameEnd(this::endGame);
                gameLogic.checkAndPromote(piece, newY);

                int currentX = gameLogic.toBoard(piece.oldX);
                int currentY = gameLogic.toBoard(piece.oldY);
                if (gameLogic.hasAnyCaptures(piece, currentX, currentY)) {
                    gameLogic.setCapturingPiece(piece);
                } else {
                    gameLogic.setCapturingPiece(null);
                    recordTurnDuration();
                    gameLogic.switchTurn();
                }
                gameLogic.updateAvailableCaptures();
                gameLogic.checkGameEnd(this::endGame);
                return;
            }

            piece.abortMove();
        });

        return piece;
    }

    public void endGame(PieceType winner) {
        gameLogic.setGameEnded(true);
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
}