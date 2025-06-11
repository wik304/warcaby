package checkers;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;

public class Piece extends Group {
    public static final int TILE_SIZE = 80;

    private final PieceType type;
    public boolean isKing = false;
    public double mouseX, mouseY;
    public double oldX, oldY;

    public Piece(PieceType type, int x, int y) {
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
}