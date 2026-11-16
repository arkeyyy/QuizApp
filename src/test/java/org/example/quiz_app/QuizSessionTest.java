package org.example.quiz_app;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class QuizSessionTest {
    private final Question question = new Question("Question", List.of("A", "B", "C", "D"), 1);

    @Test
    void requiresAnAnswerAndSubmissionBeforeAdvancing() {
        QuizSession session = new QuizSession(List.of(question));
        assertThrows(IllegalStateException.class, session::submit);
        assertThrows(IllegalStateException.class, session::next);
        assertThrows(IllegalArgumentException.class, () -> session.selectAnswer(4));
        assertEquals(0, session.score());
        assertFalse(session.isSubmitted());
        assertEquals(0, session.questionIndex());
    }

    @Test
    void selectionCanChangeButSubmissionLocksAndScoresOnlyOnce() {
        QuizSession session = new QuizSession(List.of(question));
        session.selectAnswer(0);
        session.selectAnswer(1);
        assertEquals(0, session.score());
        assertTrue(session.submit());
        assertThrows(IllegalStateException.class, session::submit);
        assertThrows(IllegalStateException.class, () -> session.selectAnswer(2));
        assertEquals(1, session.score());
    }

    @Test
    void completeMixedAttemptResetsQuestionStateAndCalculatesPercentage() {
        QuizSession session = new QuizSession(List.of(question, question, question));
        session.selectAnswer(0);
        assertFalse(session.submit());
        session.next();
        assertEquals(-1, session.selectedAnswerIndex());
        assertFalse(session.isSubmitted());
        assertEquals(1, session.questionIndex());
        session.selectAnswer(1);
        session.submit();
        session.next();
        assertTrue(session.isLastQuestion());
        session.selectAnswer(1);
        session.submit();
        assertFalse(session.isComplete());
        session.next();
        assertTrue(session.isComplete());
        assertEquals(2, session.score());
        assertEquals(66.6666666667, session.percentage(), 0.00001);
        assertThrows(IllegalStateException.class, session::next);
        assertThrows(IllegalStateException.class, session::submit);
        assertThrows(IllegalStateException.class, () -> session.selectAnswer(1));
    }

    @Test
    void zeroAndPerfectScoresAreExact() {
        for (int choice : List.of(0, 1)) {
            QuizSession session = new QuizSession(List.of(question));
            session.selectAnswer(choice);
            session.submit();
            session.next();
            assertEquals(choice == 1 ? 100.0 : 0.0, session.percentage());
        }
    }

    @Test
    void attemptUsesSnapshotAndRetakeStartsFresh() {
        var bank = new ArrayList<>(List.of(question));
        QuizSession first = new QuizSession(bank);
        first.selectAnswer(1);
        first.submit();
        first.next();
        bank.add(question);
        QuizSession retake = new QuizSession(bank);
        assertEquals(1, first.totalQuestions());
        assertEquals(2, retake.totalQuestions());
        assertEquals(0, retake.score());
        assertEquals(0, retake.questionIndex());
        assertEquals(-1, retake.selectedAnswerIndex());
        assertFalse(retake.isSubmitted());
        assertFalse(retake.isComplete());
        assertThrows(IllegalArgumentException.class, () -> new QuizSession(List.of()));
    }
}
