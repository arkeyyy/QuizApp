package org.example.quiz_app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public final class QuizApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(QuizApplication.class.getResource("quiz-view.fxml"));
        loader.setControllerFactory(type -> new QuizController(LocalStorage.forCurrentUser()));
        Scene scene = new Scene(loader.load());
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(840, screen.getWidth() * 0.9);
        double height = Math.min(640, screen.getHeight() * 0.9);
        stage.setTitle("Quiz Lab | Java Fundamentals");
        stage.setMinWidth(Math.min(700, width));
        stage.setMinHeight(Math.min(500, height));
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
