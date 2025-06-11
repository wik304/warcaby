package checkers;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Tile extends StackPane {
    public static final int TILE_SIZE = 80;

    private Piece piece;

    public Tile(boolean light, int x, int y) {
        setWidth(TILE_SIZE);
        setHeight(TILE_SIZE);
        relocate(x * TILE_SIZE, y * TILE_SIZE);

        Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
        rect.setFill(light ? Color.BEIGE : Color.BROWN);
        getChildren().add(rect);
    }

    public boolean hasPiece() {
        return piece != null;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }
}