package checkers;

import javafx.application.Application;
import javafx.stage.Stage;

public class CheckersApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        CheckersGame game = new CheckersGame();
        game.start(primaryStage);
    }
}
