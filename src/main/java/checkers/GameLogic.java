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
        for (int[] dir : piece.getMoveDirections()) {
            if (canCaptureInDirection(piece, x, y, dir)) {
                return true;
            }
        }
        return false;
    }

    private boolean canCaptureInDirection(Piece piece, int x, int y, int[] dir) {
        int maxDistance = piece.isKing ? Math.max(WIDTH, HEIGHT) : 1;

        for (int startDist = 1; startDist <= maxDistance; startDist++) {
            int enemyX = x + dir[0] * startDist;
            int enemyY = y + dir[1] * startDist;

            if (!isInsideBoard(enemyX, enemyY)) break;

            Piece enemyPiece = board[enemyX][enemyY].getPiece();

            if (enemyPiece != null && enemyPiece.getType() == piece.getType()) break;

            if (enemyPiece != null) {
                if (canLandBehindEnemy(piece, x, y, enemyX, enemyY, dir, startDist)) {
                    return true;
                }
                break;
            }
        }

        return false;
    }

    private boolean canLandBehindEnemy(Piece piece, int x, int y, int enemyX, int enemyY, int[] dir, int startDist) {
        int maxLandingDist = piece.isKing ? Math.max(WIDTH, HEIGHT) : 1;

        for (int landDist = 1; landDist <= maxLandingDist; landDist++) {
            int landX = enemyX + dir[0] * landDist;
            int landY = enemyY + dir[1] * landDist;

            if (!isInsideBoard(landX, landY)) break;
            if (board[landX][landY].hasPiece()) break;

            if (isPathClear(enemyX, enemyY, dir, landDist) &&
                    isPathClear(x, y, dir, startDist)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPathClear(int startX, int startY, int[] dir, int distance) {
        for (int step = 1; step < distance; step++) {
            int checkX = startX + dir[0] * step;
            int checkY = startY + dir[1] * step;
            if (board[checkX][checkY].hasPiece()) {
                return false;
            }
        }
        return true;
    }

    private boolean isInsideBoard(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
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
        if (gameEnded) return;

        PieceType currentPlayerType = redTurn ? PieceType.RED : PieceType.WHITE;
        PieceType opponentType = redTurn ? PieceType.WHITE : PieceType.RED;

        if (!opponentHasPieces(opponentType)) {
            endGameCallback.accept(currentPlayerType);
            return;
        }

        if (!opponentCanMove(opponentType)) {
            endGameCallback.accept(currentPlayerType);
        }
    }


    private boolean opponentHasPieces(PieceType opponentType) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (board[x][y].hasPiece() && board[x][y].getPiece().getType() == opponentType) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean opponentCanMove(PieceType opponentType) {
        redTurn = !redTurn;
        updateAvailableCaptures();

        if (!piecesWithCaptures.isEmpty()) {
            redTurn = !redTurn;
            updateAvailableCaptures();
            return true;
        }

        boolean canMove = canOpponentMakeNormalMove(opponentType);

        redTurn = !redTurn;
        updateAvailableCaptures();
        return canMove;
    }

    private boolean canOpponentMakeNormalMove(PieceType opponentType) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (board[x][y].hasPiece() && board[x][y].getPiece().getType() == opponentType) {
                    Piece piece = board[x][y].getPiece();
                    if (canPieceMoveNormally(piece, x, y)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canPieceMoveNormally(Piece piece, int x, int y) {
        List<int[]> directions = piece.getNormalMoveDirections();
        for (int[] dir : directions) {
            int maxDistance = piece.isKing ? Math.max(WIDTH, HEIGHT) : 1;

            for (int dist = 1; dist <= maxDistance; dist++) {
                int newX = x + dir[0] * dist;
                int newY = y + dir[1] * dist;

                if (!isWithinBounds(newX, newY)) break;

                if (!board[newX][newY].hasPiece() && (newX + newY) % 2 != 0) {
                    if (piece.isKing && dist > 1) {
                        if (isPathClear(x, y, newX, newY)) return true;
                    } else {
                        return true;
                    }
                }

                if (board[newX][newY].hasPiece()) break;
            }
        }
        return false;
    }

    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
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
        List<Piece> capturedPieces = collectCapturedPieces(piece, startX, startY, targetX, targetY);
        if (capturedPieces == null || capturedPieces.isEmpty()) {
            return new MoveResult(MoveType.NONE);
        }

        if (!validateCaptureRules(piece, capturedPieces)) {
            return new MoveResult(MoveType.NONE);
        }

        return new MoveResult(MoveType.KILL, capturedPieces);
    }

    private List<Piece> collectCapturedPieces(Piece piece, int startX, int startY, int targetX, int targetY) {
        int dx = targetX - startX;
        int dy = targetY - startY;
        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);

        List<Piece> capturedPieces = new ArrayList<>();

        for (int step = 1; step < Math.abs(dx); step++) {
            int checkX = startX + step * stepX;
            int checkY = startY + step * stepY;

            if (isInBounds(checkX, checkY)) {
                return null;
            }

            Piece pieceAtPosition = board[checkX][checkY].getPiece();

            if (pieceAtPosition != null) {
                if (pieceAtPosition.getType() == piece.getType()) {
                    return null;
                }
                capturedPieces.add(pieceAtPosition);
            }
        }

        return capturedPieces;
    }

    private boolean validateCaptureRules(Piece piece, List<Piece> capturedPieces) {
        if (!piece.isKing && capturedPieces.size() > 1) {
            return false;
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
                    return false;
                }
            }
        }

        return true;
    }

    public int toBoard(double pixel) {
        return (int)(pixel + (double) TILE_SIZE / 2) / TILE_SIZE;
    }
}
