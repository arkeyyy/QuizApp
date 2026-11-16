package org.example.quiz_app;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {
    @Test
    void trimsInputAndDefensivelyCopiesOptions() {
        var options = new ArrayList<>(List.of(" A ", "B", "C", "D"));
        Question question = new Question("  Prompt  ", options, 0);
        options.set(0, "Changed");
        assertEquals("Prompt", question.prompt());
        assertEquals("A", question.options().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> question.options().set(0, "Changed"));
    }

    @Test
    void rejectsInvalidQuestionData() {
        List<String> options = List.of("A", "B", "C", "D");
        assertThrows(IllegalArgumentException.class, () -> new Question(" ", options, 0));
        assertThrows(IllegalArgumentException.class, () -> new Question(null, options, 0));
        assertThrows(IllegalArgumentException.class, () -> new Question("Prompt", List.of("A"), 0));
        assertThrows(IllegalArgumentException.class, () -> new Question("Prompt", List.of("A", " ", "C", "D"), 0));
        assertThrows(IllegalArgumentException.class, () -> new Question("Prompt", List.of(" A", "a ", "C", "D"), 0));
        assertThrows(IllegalArgumentException.class, () -> new Question("Prompt", options, -1));
        assertThrows(IllegalArgumentException.class, () -> new Question("Prompt", options, 4));
    }

    @Test
    void builtInBankMeetsAssignmentRequirements() {
        List<Question> questions = QuestionBank.builtInQuestions();
        assertEquals(10, questions.size());
        assertEquals(10, questions.stream().map(Question::prompt).distinct().count());
        assertEquals(4, questions.stream().map(Question::correctAnswerIndex).distinct().count());
        assertTrue(questions.stream().allMatch(question -> question.options().size() == 4));
    }
}
