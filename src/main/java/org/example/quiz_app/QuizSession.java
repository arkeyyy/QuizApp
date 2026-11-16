package org.example.quiz_app;

import java.util.List;

/** Owns attempt state; no JavaFX dependencies and no changes to the question bank. */
public final class QuizSession {
    private final List<Question> questions;
    private int questionIndex;
    private int selectedAnswerIndex = -1;
    private int score;
    private boolean submitted;
    private boolean complete;

    public QuizSession(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("A quiz needs at least one question.");
        }
        this.questions = List.copyOf(questions);
    }

    public void selectAnswer(int index) {
        requireActive();
        if (submitted) throw new IllegalStateException("This answer has already been submitted.");
        if (index < 0 || index >= 4) throw new IllegalArgumentException("Invalid answer selection.");
        selectedAnswerIndex = index;
    }

    public boolean submit() {
        requireActive();
        if (submitted) throw new IllegalStateException("This answer has already been submitted.");
        if (selectedAnswerIndex < 0) throw new IllegalStateException("No answer selected.");
        submitted = true;
        boolean correct = selectedAnswerIndex == currentQuestion().correctAnswerIndex();
        if (correct) score++;
        return correct;
    }

    public void next() {
        requireActive();
        if (!submitted) throw new IllegalStateException("Current answer has not been submitted.");
        if (isLastQuestion()) {
            complete = true;
        } else {
            questionIndex++;
            selectedAnswerIndex = -1;
            submitted = false;
        }
    }

    private void requireActive() {
        if (complete) throw new IllegalStateException("This quiz is complete.");
    }

    public Question currentQuestion() { return questions.get(questionIndex); }
    public int questionIndex() { return questionIndex; }
    public int selectedAnswerIndex() { return selectedAnswerIndex; }
    public int score() { return score; }
    public int totalQuestions() { return questions.size(); }
    public boolean isSubmitted() { return submitted; }
    public boolean isComplete() { return complete; }
    public boolean isLastQuestion() { return questionIndex == questions.size() - 1; }
    public double percentage() { return score * 100.0 / questions.size(); }
}
