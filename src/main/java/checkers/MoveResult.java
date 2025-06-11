package checkers;

import java.util.ArrayList;
import java.util.List;

public class MoveResult {
    public final MoveType type;
    public final Piece piece;
    public final List<Piece> capturedPieces;

    public MoveResult(MoveType type) {
        this(type, (Piece) null);
    }

    public MoveResult(MoveType type, Piece piece) {
        this.type = type;
        this.piece = piece;
        this.capturedPieces = piece != null ? List.of(piece) : new ArrayList<>();
    }

    public MoveResult(MoveType type, List<Piece> capturedPieces) {
        this.type = type;
        this.piece = capturedPieces.isEmpty() ? null : capturedPieces.getFirst();
        this.capturedPieces = new ArrayList<>(capturedPieces);
    }
}