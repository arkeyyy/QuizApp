package org.example.quiz_app;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** An immutable, validated multiple-choice question. */
public record Question(String prompt, List<String> options, int correctAnswerIndex) {
    public Question {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Enter a question.");
        }
        prompt = prompt.strip();
        if (options == null || options.size() != 4) {
            throw new IllegalArgumentException("Provide exactly four answer choices.");
        }
        if (options.stream().anyMatch(option -> option == null || option.isBlank())) {
            throw new IllegalArgumentException("Fill in all four answer choices.");
        }
        options = options.stream().map(String::strip).toList();
        var unique = new HashSet<String>();
        for (String option : options) {
            if (!unique.add(option.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Each answer choice must be different.");
            }
        }
        if (correctAnswerIndex < 0 || correctAnswerIndex >= 4) {
            throw new IllegalArgumentException("Choose the correct answer.");
        }
    }
}
