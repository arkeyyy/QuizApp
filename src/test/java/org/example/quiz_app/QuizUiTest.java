package org.example.quiz_app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.BufferedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Real JavaFX controls and FXML; requires a desktop. All saved data uses a temporary directory. */
class QuizUiTest {
    @TempDir Path directory;
    private Stage stage;
    private Parent root;

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); started.countDown(); });
        assertTrue(started.await(15, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopToolkit() { Platform.exit(); }

    @BeforeEach
    void openApp() throws Exception { onFx(this::createStage); }

    private void createStage() throws Exception {
        FXMLLoader loader = new FXMLLoader(QuizApplication.class.getResource("quiz-view.fxml"));
        loader.setControllerFactory(type -> new QuizController(new LocalStorage(directory)));
        root = loader.load();
        stage = new Stage();
        stage.setScene(new Scene(root, 960, 820));
        stage.show();
        root.applyCss();
        root.layout();
    }

    @AfterEach
    void closeWindows() throws Exception {
        onFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    void fullQuizSelectionSubmissionResultsAndRetake() throws Exception {
        onFx(() -> {
            assertTrue(node("homeScreen").isVisible());
            snapshot("home-light");
            button("startButton").fire();
            assertTrue(button("nextButton").isDisabled());
            button("submitButton").fire();
            assertEquals("No answer selected.", label("feedbackLabel").getText());
            assertEquals("Score  0 / 10", label("scoreLabel").getText());
            List<Question> questions = QuestionBank.builtInQuestions();
            for (int i = 0; i < questions.size(); i++) {
                assertEquals(questions.get(i).prompt(), label("questionLabel").getText());
                assertFalse(button("submitButton").isDisabled());
                assertTrue(button("nextButton").isDisabled());
                button("answerA").fire();
                int answer = i == 0 ? (questions.get(i).correctAnswerIndex() + 1) % 4 : questions.get(i).correctAnswerIndex();
                Button selected = button("answer" + (char) ('A' + answer));
                selected.fire();
                assertTrue(selected.getStyleClass().contains("selected"));
                if (i == 0) snapshot("quiz-selected");
                button("submitButton").fire();
                assertTrue(button("submitButton").isDisabled());
                assertTrue(selected.isDisabled());
                assertFalse(button("nextButton").isDisabled());
                String scored = label("scoreLabel").getText();
                button("submitButton").fire();
                assertEquals(scored, label("scoreLabel").getText());
                if (i == 0) {
                    assertTrue(label("feedbackLabel").getText().contains("Correct answer"));
                    snapshot("quiz-feedback");
                }
                if (i == 9) assertEquals("View Results", button("nextButton").getText());
                button("nextButton").fire();
            }
            assertTrue(node("resultScreen").isVisible());
            assertEquals("9 / 10", label("resultScore").getText());
            assertEquals("90.0%", label("resultPercentage").getText());
            snapshot("results");
            button("retakeButton").fire();
            assertEquals("Score  0 / 10", label("scoreLabel").getText());
            assertTrue(button("nextButton").isDisabled());
            assertFalse(button("answerA").getStyleClass().contains("selected"));
        });
    }

    @Test
    void themePersistsAndSmallWindowRemainsScrollable() throws Exception {
        onFx(() -> {
            ((CheckBox) node("themeToggle")).fire();
            assertTrue(root.getStyleClass().contains("dark"));
            snapshot("home-dark");
            stage.close();
            createStage();
            assertTrue(((CheckBox) node("themeToggle")).isSelected());
            assertTrue(root.getStyleClass().contains("dark"));
            button("startButton").fire();
            stage.setWidth(700);
            stage.setHeight(600);
        });
        onFx(() -> {
            root.applyCss();
            root.layout();
            ScrollPane scroll = (ScrollPane) node("scroller");
            assertTrue(scroll.isFitToWidth());
            assertTrue(scroll.getContent().getBoundsInLocal().getHeight() > scroll.getViewportBounds().getHeight());
            assertTrue(button("answerA").getWidth() > 400);
            snapshot("quiz-dark-small");
        });
    }

    @Test
    void editorValidatesAndSavedQuestionSurvivesRelaunch() throws Exception {
        onFx(() -> Platform.runLater(() -> button("addQuestionButton").fire()));
        onFx(() -> {
            DialogPane editor = editor();
            Button save = (Button) editor.lookup("#saveQuestion");
            save.fire();
            assertTrue(editor.lookup("#promptError").isVisible());
            TextArea prompt = (TextArea) editor.lookup("#promptField");
            prompt.setText("Which word includes ñ?");
            String[] options = {"mañana", "mañana", "class", "new"};
            for (int i = 0; i < 4; i++) ((TextField) editor.lookup("#option" + i)).setText(options[i]);
            ((ComboBox<?>) editor.lookup("#correctChoice")).getSelectionModel().select(0);
            save.fire();
            assertTrue(editor.lookup("#optionError1").isVisible());
            assertEquals("Which word includes ñ?", prompt.getText());
            snapshotNode(editor, "editor-validation");
            ((TextField) editor.lookup("#option1")).setText("Java");
            save.fire();
        });
        onFx(() -> {
            assertEquals("11 questions", label("questionCount").getText());
            stage.close();
            createStage();
            assertEquals("11 questions", label("questionCount").getText());
            button("startButton").fire();
            for (int i = 0; i < 10; i++) {
                button("answerA").fire();
                button("submitButton").fire();
                button("nextButton").fire();
            }
            assertEquals("Which word includes ñ?", label("questionLabel").getText());
            assertTrue(button("answerA").getText().contains("mañana"));
        });
    }

    @Test
    void failedSaveKeepsEditorInputAndBankUnchanged() throws Exception {
        onFx(() -> {
            Files.createDirectory(directory.resolve("questions.properties"));
            stage.close();
            createStage();
            assertTrue(node("storageWarning").isVisible());
            Platform.runLater(() -> button("addQuestionButton").fire());
        });
        onFx(() -> {
            DialogPane editor = editor();
            ((TextArea) editor.lookup("#promptField")).setText("Keep my question");
            for (int i = 0; i < 4; i++) ((TextField) editor.lookup("#option" + i)).setText("Choice " + i);
            ((ComboBox<?>) editor.lookup("#correctChoice")).getSelectionModel().select(2);
            ((Button) editor.lookup("#saveQuestion")).fire();
            assertTrue(((Label) editor.lookup("#formError")).getText().contains("not saved"));
            assertEquals("Keep my question", ((TextArea) editor.lookup("#promptField")).getText());
            assertEquals("10 questions", label("questionCount").getText());
            ((Button) editor.lookupButton(ButtonType.CANCEL)).fire();
        });
    }

    private DialogPane editor() {
        return Window.getWindows().stream().filter(Window::isShowing)
            .map(window -> window.getScene().getRoot()).filter(DialogPane.class::isInstance)
            .map(DialogPane.class::cast).findFirst().orElseThrow();
    }

    private Node node(String id) { return root.lookup("#" + id); }
    private Button button(String id) { return (Button) node(id); }
    private Label label(String id) { return (Label) node(id); }
    private void snapshot(String name) throws Exception { snapshotNode(root, name); }

    /** Portable RGB snapshots for visual inspection without an imaging dependency. */
    private static void snapshotNode(Parent node, String name) throws Exception {
        node.applyCss();
        node.layout();
        WritableImage image = node.snapshot(null, null);
        Path directory = Path.of("target", "ui-snapshots");
        Files.createDirectories(directory);
        try (var output = new BufferedOutputStream(Files.newOutputStream(directory.resolve(name + ".ppm")))) {
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            output.write(("P6\n" + width + " " + height + "\n255\n").getBytes(StandardCharsets.US_ASCII));
            var pixels = image.getPixelReader();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = pixels.getArgb(x, y);
                    output.write((argb >> 16) & 255);
                    output.write((argb >> 8) & 255);
                    output.write(argb & 255);
                }
            }
        }
    }

    @FunctionalInterface private interface FxAction { void run() throws Exception; }

    private static void onFx(FxAction action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> { action.run(); return null; });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }
}
