package checkers;

public enum PieceType {
    RED(true, 1), WHITE(false, -1);

    public final boolean isRed;
    public final int moveDir;

    PieceType(boolean isRed, int moveDir) {
        this.isRed = isRed;
        this.moveDir = moveDir;
    }
}