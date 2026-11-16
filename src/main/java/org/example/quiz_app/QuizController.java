package org.example.quiz_app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QuizController {
    private final LocalStorage storage;
    private final List<Question> customQuestions = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private QuizSession session;
    private List<Button> answerButtons;

    @FXML private VBox appRoot;
    @FXML private ScrollPane scroller;
    @FXML private VBox homeScreen;
    @FXML private VBox quizScreen;
    @FXML private VBox resultScreen;
    @FXML private CheckBox themeToggle;
    @FXML private Label storageWarning;
    @FXML private Label questionCount;
    @FXML private Label homeNotice;
    @FXML private Label questionNumber;
    @FXML private Label scoreLabel;
    @FXML private Label questionLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label resultScore;
    @FXML private Label resultPercentage;
    @FXML private Label resultMessage;
    @FXML private ProgressBar questionProgress;
    @FXML private ProgressBar resultProgress;
    @FXML private Button answerA;
    @FXML private Button answerB;
    @FXML private Button answerC;
    @FXML private Button answerD;
    @FXML private Button submitButton;
    @FXML private Button nextButton;
    @FXML private Button startButton;
    @FXML private Button retakeButton;

    public QuizController() { this(LocalStorage.forCurrentUser()); }
    public QuizController(LocalStorage storage) { this.storage = storage; }

    @FXML
    private void initialize() {
        answerButtons = List.of(answerA, answerB, answerC, answerD);
        for (int i = 0; i < answerButtons.size(); i++) {
            int index = i;
            answerButtons.get(i).setOnAction(event -> selectAnswer(index));
        }
        LocalStorage.LoadResult<List<Question>> loadedQuestions = storage.loadQuestions();
        customQuestions.addAll(loadedQuestions.value());
        addWarning(loadedQuestions.warning());
        LocalStorage.LoadResult<LocalStorage.Theme> loadedTheme = storage.loadTheme();
        themeToggle.setSelected(loadedTheme.value() == LocalStorage.Theme.DARK);
        applyTheme();
        addWarning(loadedTheme.warning());
        showHome();
    }

    private List<Question> questionBank() {
        List<Question> questions = new ArrayList<>(QuestionBank.builtInQuestions());
        questions.addAll(customQuestions);
        return questions;
    }

    @FXML
    private void startQuiz() {
        session = new QuizSession(questionBank());
        showScreen(quizScreen);
        renderQuestion();
    }

    private void renderQuestion() {
        Question question = session.currentQuestion();
        questionNumber.setText("QUESTION %02d / %02d".formatted(session.questionIndex() + 1, session.totalQuestions()));
        questionLabel.setText(question.prompt());
        scoreLabel.setText("Score  " + session.score() + " / " + session.totalQuestions());
        questionProgress.setProgress((double) session.questionIndex() / session.totalQuestions());
        questionProgress.setAccessibleText(session.questionIndex() + " of " + session.totalQuestions() + " questions submitted");
        for (int i = 0; i < 4; i++) {
            Button button = answerButtons.get(i);
            String text = (char) ('A' + i) + "    " + question.options().get(i);
            button.setText(text);
            button.setAccessibleText(text);
            button.setDisable(false);
            button.getStyleClass().removeAll("selected", "correct", "incorrect");
        }
        setFeedback("Choose one answer, then select Submit.", "muted");
        submitButton.setDisable(false);
        nextButton.setDisable(true);
        nextButton.setText(session.isLastQuestion() ? "View Results" : "Next Question");
        focus(answerA);
    }

    private void selectAnswer(int index) {
        session.selectAnswer(index);
        for (int i = 0; i < 4; i++) {
            Button button = answerButtons.get(i);
            button.getStyleClass().remove("selected");
            if (i == index) button.getStyleClass().add("selected");
            button.setAccessibleText(button.getText() + (i == index ? ", selected" : ""));
        }
        setFeedback("Answer " + (char) ('A' + index) + " selected. Submit to check your answer.", "muted");
    }

    @FXML
    private void submitAnswer() {
        try {
            boolean correct = session.submit();
            Question question = session.currentQuestion();
            int correctIndex = question.correctAnswerIndex();
            for (int i = 0; i < 4; i++) {
                Button button = answerButtons.get(i);
                button.setDisable(true);
                button.getStyleClass().remove("selected");
                if (i == correctIndex) button.getStyleClass().add("correct");
                else if (i == session.selectedAnswerIndex()) button.getStyleClass().add("incorrect");
            }
            String answer = (char) ('A' + correctIndex) + " — " + question.options().get(correctIndex);
            setFeedback(correct ? "Correct! " + answer : "Not quite. Correct answer: " + answer,
                correct ? "success" : "error");
            scoreLabel.setText("Score  " + session.score() + " / " + session.totalQuestions());
            questionProgress.setProgress((session.questionIndex() + 1.0) / session.totalQuestions());
            questionProgress.setAccessibleText((session.questionIndex() + 1) + " of " + session.totalQuestions() + " questions submitted");
            submitButton.setDisable(true);
            nextButton.setDisable(false);
            focus(nextButton);
        } catch (IllegalStateException ex) {
            setFeedback(ex.getMessage(), "error");
            focus(answerA);
        }
    }

    @FXML
    private void nextQuestion() {
        session.next();
        if (session.isComplete()) {
            resultScore.setText(session.score() + " / " + session.totalQuestions());
            resultPercentage.setText(String.format(Locale.ROOT, "%.1f%%", session.percentage()));
            resultProgress.setProgress(session.percentage() / 100.0);
            resultMessage.setText(session.score() == session.totalQuestions()
                ? "Perfect score. You know your Java fundamentals!"
                : "Every attempt is practice. Try again to strengthen what you know.");
            showScreen(resultScreen);
            focus(retakeButton);
        } else {
            renderQuestion();
            scroller.setVvalue(0);
        }
    }

    @FXML
    private void showHome() {
        questionCount.setText(questionBank().size() + " questions");
        homeNotice.setText(customQuestions.isEmpty() ? "Includes 10 Java fundamentals questions."
            : "10 built-in + " + customQuestions.size() + " custom questions. Ready for your next attempt.");
        showScreen(homeScreen);
        focus(startButton);
    }

    @FXML
    private void addQuestion() {
        QuestionEditor.show(appRoot.getScene().getWindow(), themeToggle.isSelected(), question -> {
            List<Question> updated = new ArrayList<>(customQuestions);
            updated.add(question);
            storage.saveQuestions(updated);
            customQuestions.add(question);
            questionCount.setText(questionBank().size() + " questions");
            homeNotice.setText("Question saved. Your next quiz will contain " + questionBank().size() + " questions.");
        });
    }

    @FXML
    private void toggleTheme() {
        applyTheme();
        try {
            storage.saveTheme(themeToggle.isSelected() ? LocalStorage.Theme.DARK : LocalStorage.Theme.LIGHT);
        } catch (IOException | SecurityException ex) {
            addWarning("Your theme changed for this session, but could not be saved. " + ex.getMessage());
        }
    }

    private void applyTheme() {
        appRoot.getStyleClass().remove("dark");
        if (themeToggle.isSelected()) appRoot.getStyleClass().add("dark");
    }

    private void showScreen(VBox screen) {
        for (VBox candidate : List.of(homeScreen, quizScreen, resultScreen)) {
            candidate.setVisible(candidate == screen);
            candidate.setManaged(candidate == screen);
        }
        scroller.setVvalue(0);
    }

    private void setFeedback(String text, String style) {
        feedbackLabel.setText(text);
        feedbackLabel.getStyleClass().removeAll("muted", "success", "error");
        feedbackLabel.getStyleClass().add(style);
    }

    private void addWarning(String warning) {
        if (warning != null && !warnings.contains(warning)) warnings.add(warning);
        storageWarning.setText(String.join("\n", warnings));
        storageWarning.setVisible(!warnings.isEmpty());
        storageWarning.setManaged(!warnings.isEmpty());
    }

    private static void focus(Node node) { Platform.runLater(node::requestFocus); }
}
