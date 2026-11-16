package org.example.quiz_app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class QuizApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(QuizApplication.class.getResource("quiz-view.fxml"));
        loader.setControllerFactory(type -> new QuizController(LocalStorage.forCurrentUser()));
        Scene scene = new Scene(loader.load(), 960, 820);
        stage.setTitle("Quiz Lab | Java Fundamentals");
        stage.setMinWidth(700);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
