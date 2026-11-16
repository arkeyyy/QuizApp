module org.example.quiz_app {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.quiz_app to javafx.fxml;
    exports org.example.quiz_app;
}