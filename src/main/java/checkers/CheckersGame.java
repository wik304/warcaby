package checkers;

import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.scene.text.Font;
import javafx.geometry.Pos;

import java.util.Optional;
import java.time.Duration;

public class CheckersGame {
    public static final int TILE_SIZE = 80;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;
    public static final int TIME_LIMIT_MINUTES = 10;

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

    private long whiteTimeRemaining = TIME_LIMIT_MINUTES * 60L * 1_000_000_000L;
    private long redTimeRemaining = TIME_LIMIT_MINUTES * 60L * 1_000_000_000L;
    private Label whiteClockLabel, redClockLabel;
    private AnimationTimer gameTimer;
    private boolean isTimerRunning = false;

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showStartMenu();
    }

    private void showStartMenu() {
        Button localPlayButton = new Button("Graj lokalnie (1vs1)");
        Button lanPlayButton = new Button("Graj przez LAN (1vs1)");

        String buttonStyle = """
        -fx-font-size: 16px;
        -fx-padding: 10 20;
        -fx-background-color: linear-gradient(to bottom, #4CAF50, #2E7D32);
        -fx-text-fill: white;
        -fx-background-radius: 10;
        -fx-cursor: hand;
        """;

        String hoverStyle = """
        -fx-background-color: linear-gradient(to bottom, #66BB6A, #388E3C);
        """;

        localPlayButton.setStyle(buttonStyle);
        lanPlayButton.setStyle(buttonStyle);

        localPlayButton.setOnMouseEntered(_ -> localPlayButton.setStyle(buttonStyle + hoverStyle));
        localPlayButton.setOnMouseExited(_ -> localPlayButton.setStyle(buttonStyle));
        lanPlayButton.setOnMouseEntered(_ -> lanPlayButton.setStyle(buttonStyle + hoverStyle));
        lanPlayButton.setOnMouseExited(_ -> lanPlayButton.setStyle(buttonStyle));

        localPlayButton.setOnAction(_ -> {
            this.gameLogic = new GameLogic(board);
            initializeGame();
            primaryStage.setScene(new Scene(root));
        });

        lanPlayButton.setOnAction(_ -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("LAN");
            alert.setHeaderText("Tryb LAN nie jest jeszcze dostępny.");
            alert.setContentText("Funkcjonalność gry przez LAN będzie dodana w przyszłości.");
            alert.showAndWait();
        });

        VBox menuLayout = new VBox(20, localPlayButton, lanPlayButton);
        menuLayout.setStyle("-fx-padding: 40; -fx-alignment: center; -fx-background-color: linear-gradient(to bottom, #d0d0d0, #f0f0f0);");

        Scene menuScene = new Scene(menuLayout, 400, 300);
        primaryStage.setScene(menuScene);
        primaryStage.setTitle("Warcaby");
        primaryStage.show();
    }

    public void initializeGame() {
        resetAndPrepareGame();
        setupRootLayout();
        createBoardAndPieces();
        setupMoveTimeSidebar();
        setupChessClocks();
        finalizeInitialization();
    }

    private void resetAndPrepareGame() {
        gameLogic.resetGame();
        tileGroup.getChildren().clear();
        pieceGroup.getChildren().clear();

        board = new Tile[WIDTH][HEIGHT];
        gameLogic.setBoard(board);

        whiteTimeRemaining = TIME_LIMIT_MINUTES * 60L * 1_000_000_000L;
        redTimeRemaining = TIME_LIMIT_MINUTES * 60L * 1_000_000_000L;

        if (gameTimer != null) {
            gameTimer.stop();
        }
        isTimerRunning = false;
    }

    private void setupRootLayout() {
        root = new BorderPane();

        Pane boardPane = new Pane();
        boardPane.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE);
        boardPane.getChildren().addAll(tileGroup, pieceGroup);

        root.setCenter(boardPane);
    }

    private void createBoardAndPieces() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Tile tile = new Tile((x + y) % 2 == 0, x, y);
                board[x][y] = tile;
                tileGroup.getChildren().add(tile);

                Piece piece = createInitialPiece(x, y);
                if (piece != null) {
                    tile.setPiece(piece);
                    pieceGroup.getChildren().add(piece);
                }
            }
        }
    }

    private Piece createInitialPiece(int x, int y) {
        if (y <= 2 && (x + y) % 2 != 0) {
            return makePiece(PieceType.RED, x, y);
        }
        if (y >= 5 && (x + y) % 2 != 0) {
            return makePiece(PieceType.WHITE, x, y);
        }
        return null;
    }

    private void setupMoveTimeSidebar() {
        whiteTimeList.getChildren().clear();
        redTimeList.getChildren().clear();

        whiteScroll = new ScrollPane(whiteTimeList);
        redScroll = new ScrollPane(redTimeList);
        whiteScroll.setFitToWidth(true);
        redScroll.setFitToWidth(true);
        whiteScroll.setPrefHeight(200);
        redScroll.setPrefHeight(200);
    }

    private void setupChessClocks() {
        whiteClockLabel = new Label(formatTime(whiteTimeRemaining));
        redClockLabel = new Label(formatTime(redTimeRemaining));

        whiteClockLabel.setFont(Font.font(20));
        redClockLabel.setFont(Font.font(20));

        whiteClockLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        redClockLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

        Label whiteText = new Label("BIAŁY:");
        Label redText = new Label("CZERWONY:");
        whiteText.setStyle("-fx-font-weight: bold; -fx-padding: 0 5 0 0;");
        redText.setStyle("-fx-font-weight: bold; -fx-padding: 0 5 0 0;");

        HBox whiteClockBox = new HBox(whiteText, whiteClockLabel);
        HBox redClockBox = new HBox(redText, redClockLabel);

        whiteClockBox.setAlignment(Pos.CENTER_LEFT);
        redClockBox.setAlignment(Pos.CENTER_LEFT);
        whiteClockBox.setMinHeight(40);
        redClockBox.setMinHeight(40);
        whiteClockBox.setStyle("-fx-padding: 0 10; -fx-background-color: #f8f8f8; -fx-border-color: #cccccc; -fx-border-width: 1;");
        redClockBox.setStyle("-fx-padding: 0 10; -fx-background-color: #f8f8f8; -fx-border-color: #cccccc; -fx-border-width: 1;");

        VBox clocksBox = new VBox(10, whiteClockBox, redClockBox);

        VBox sidebar = new VBox(10,
                clocksBox,
                new Label("Czas ruchów BIAŁY:"), whiteScroll,
                new Label("Czas ruchów CZERWONY:"), redScroll
        );
        sidebar.setPrefWidth(250);
        sidebar.setStyle("-fx-padding: 20; -fx-background-color: #f0f0f0;");

        root.setRight(sidebar);

        gameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameLogic.isRedTurn()) {
                    redClockLabel.setText(formatTime(redTimeRemaining - (System.nanoTime() - turnStartTime)));
                    if (redTimeRemaining <= 0) {
                        endGame(PieceType.WHITE);
                    }
                } else {
                    whiteClockLabel.setText(formatTime(whiteTimeRemaining - (System.nanoTime() - turnStartTime)));
                    if (whiteTimeRemaining <= 0) {
                        endGame(PieceType.RED);
                    }
                }
            }
        };
    }

    private String formatTime(long nanoseconds) {
        if (nanoseconds <= 0) return "00:00";

        Duration duration = Duration.ofNanos(nanoseconds);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void finalizeInitialization() {
        gameLogic.updateAvailableCaptures();
        turnStartTime = System.nanoTime();
        startTimer();
    }

    private void startTimer() {
        if (!isTimerRunning) {
            gameTimer.start();
            isTimerRunning = true;
        }
    }

    private void stopTimer() {
        if (isTimerRunning) {
            gameTimer.stop();
            isTimerRunning = false;
        }
    }

    private void switchPlayerClock() {
        long currentTime = System.nanoTime();
        long elapsed = currentTime - turnStartTime;

        if (gameLogic.isRedTurn()) {
            redTimeRemaining -= elapsed;
            if (redTimeRemaining <= 0) {
                endGame(PieceType.WHITE);
                return;
            }

            double seconds = elapsed / 1_000_000_000.0;
            Label entry = new Label(String.format("Ruch %d: %.3f s", redTimeList.getChildren().size() + 1, seconds));
            redTimeList.getChildren().add(entry);
            redScroll.setVvalue(1.0);
        } else {
            whiteTimeRemaining -= elapsed;
            if (whiteTimeRemaining <= 0) {
                endGame(PieceType.RED);
                return;
            }

            double seconds = elapsed / 1_000_000_000.0;
            Label entry = new Label(String.format("Ruch %d: %.3f s", whiteTimeList.getChildren().size() + 1, seconds));
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
                piece.setLayoutX(e.getSceneX() - piece.mouseX - TILE_SIZE / 2.0);
                piece.setLayoutY(e.getSceneY() - piece.mouseY - TILE_SIZE / 2.0);
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
                handleNormalMove(piece, newX, newY);
            } else if (result.type == MoveType.KILL && !result.capturedPieces.isEmpty()) {
                handleKillMove(piece, newX, newY, result);
            } else {
                piece.abortMove();
            }
        });

        return piece;
    }

    private void handleNormalMove(Piece piece, int newX, int newY) {
        if (!gameLogic.getPiecesWithCaptures().isEmpty() || gameLogic.getCapturingPiece() != null) {
            piece.abortMove();
            return;
        }

        gameLogic.makeMove(piece, newX, newY);
        gameLogic.checkAndPromote(piece, newY);
        gameLogic.setCapturingPiece(null);
        switchPlayerClock();
        gameLogic.switchTurn();
        gameLogic.updateAvailableCaptures();
        gameLogic.checkGameEnd(this::endGame);
    }

    private void handleKillMove(Piece piece, int newX, int newY, MoveResult result) {
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
            switchPlayerClock();
            gameLogic.switchTurn();
        }

        gameLogic.updateAvailableCaptures();
        gameLogic.checkGameEnd(this::endGame);
    }

    public void endGame(PieceType winner) {
        stopTimer();
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
