package checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GameLogic {
    public static final int TILE_SIZE = 80;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private Tile[][] board;
    private boolean redTurn = false;
    private Piece capturingPiece = null;
    private final List<Piece> piecesWithCaptures = new ArrayList<>();
    private boolean gameEnded = false;

    public GameLogic(Tile[][] board) {
        this.board = board;
    }

    public void resetGame() {
        redTurn = false;
        capturingPiece = null;
        piecesWithCaptures.clear();
        gameEnded = false;
    }

    public void setBoard(Tile[][] board) {
        this.board = board;
    }

    public boolean isRedTurn() {
        return redTurn;
    }

    public void switchTurn() {
        redTurn = !redTurn;
    }

    public Piece getCapturingPiece() {
        return capturingPiece;
    }

    public void setCapturingPiece(Piece piece) {
        this.capturingPiece = piece;
    }

    public List<Piece> getPiecesWithCaptures() {
        return piecesWithCaptures;
    }

    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }

    public void updateAvailableCaptures() {
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

    public boolean canPieceMove(Piece piece) {
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

    public void checkAndPromote(Piece piece, int newY) {
        if (!piece.isKing) {
            if ((piece.getType() == PieceType.RED && newY == HEIGHT - 1) ||
                    (piece.getType() == PieceType.WHITE && newY == 0)) {
                piece.makeKing();
            }
        }
    }

    public boolean hasAnyCaptures(Piece piece, int x, int y) {
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

    public void makeMove(Piece piece, int newX, int newY) {
        int x0 = toBoard(piece.oldX);
        int y0 = toBoard(piece.oldY);
        board[x0][y0].setPiece(null);
        piece.move(newX, newY);
        board[newX][newY].setPiece(piece);
    }

    private boolean isInBounds(int x, int y) {
        return x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT;
    }

    public MoveResult tryMove(Piece piece, int newX, int newY) {
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

    public void checkGameEnd(Consumer<PieceType> endGameCallback) {
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
            endGameCallback.accept(currentPlayerType);
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
            endGameCallback.accept(currentPlayerType);
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

    public int toBoard(double pixel) {
        return (int)(pixel + (double) TILE_SIZE / 2) / TILE_SIZE;
    }
}