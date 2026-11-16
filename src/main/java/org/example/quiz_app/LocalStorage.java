package org.example.quiz_app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Small local store. Failed reads block writes so damaged data is never silently replaced. */
public final class LocalStorage {
    public enum Theme { LIGHT, DARK }
    public record LoadResult<T>(T value, String warning) { }

    private final Path directory;
    private boolean questionsLoaded;
    private boolean themeLoaded;
    private boolean questionsBlocked;
    private boolean themeBlocked;

    public LocalStorage(Path directory) { this.directory = directory; }

    public static LocalStorage forCurrentUser() {
        return new LocalStorage(Path.of(System.getProperty("user.home"), ".csit228-quiz-app"));
    }

    public LoadResult<List<Question>> loadQuestions() {
        questionsLoaded = true;
        Path path = directory.resolve("questions.properties");
        try {
            if (Files.notExists(path)) {
                questionsBlocked = false;
                return new LoadResult<>(List.of(), null);
            }
            Properties properties = read(path);
            int count = Integer.parseInt(required(properties, "count"));
            if (count < 0) throw new IllegalArgumentException("Invalid question count.");
            List<Question> questions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String prefix = "question." + i + ".";
                List<String> options = new ArrayList<>();
                for (int j = 0; j < 4; j++) options.add(required(properties, prefix + "option." + j));
                questions.add(new Question(required(properties, prefix + "prompt"), options,
                    Integer.parseInt(required(properties, prefix + "correct"))));
            }
            questionsBlocked = false;
            return new LoadResult<>(List.copyOf(questions), null);
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            questionsBlocked = true;
            return new LoadResult<>(List.of(), readWarning(path, "custom questions"));
        }
    }

    public LoadResult<Theme> loadTheme() {
        themeLoaded = true;
        Path path = directory.resolve("settings.properties");
        try {
            if (Files.notExists(path)) {
                themeBlocked = false;
                return new LoadResult<>(Theme.LIGHT, null);
            }
            Theme theme = Theme.valueOf(required(read(path), "theme"));
            themeBlocked = false;
            return new LoadResult<>(theme, null);
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            themeBlocked = true;
            return new LoadResult<>(Theme.LIGHT, readWarning(path, "theme settings"));
        }
    }

    public void saveQuestions(List<Question> questions) throws IOException {
        if (!questionsLoaded) loadQuestions();
        if (questionsBlocked) throw new IOException("Custom questions could not be read. Back up and repair or move questions.properties, then restart the app before saving.");
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(questions.size()));
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            String prefix = "question." + i + ".";
            properties.setProperty(prefix + "prompt", question.prompt());
            properties.setProperty(prefix + "correct", Integer.toString(question.correctAnswerIndex()));
            for (int j = 0; j < 4; j++) properties.setProperty(prefix + "option." + j, question.options().get(j));
        }
        write(directory.resolve("questions.properties"), properties);
    }

    public void saveTheme(Theme theme) throws IOException {
        if (!themeLoaded) loadTheme();
        if (themeBlocked) throw new IOException("Theme settings could not be read. Back up and repair or move settings.properties, then restart the app before saving.");
        Properties properties = new Properties();
        properties.setProperty("theme", theme.name());
        write(directory.resolve("settings.properties"), properties);
    }

    private static Properties read(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        if (!"1".equals(required(properties, "version"))) {
            throw new IllegalArgumentException("Unsupported storage version.");
        }
        return properties;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing property: " + key);
        return value;
    }

    private void write(Path target, Properties properties) throws IOException {
        Files.createDirectories(directory);
        properties.setProperty("version", "1");
        Path temporary = Files.createTempFile(directory, "quiz-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "CSIT228 Quiz App");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String readWarning(Path path, String description) {
        return "Could not load " + description + " from " + path + ". The file was left unchanged. "
            + "Back it up and repair or move it, then restart the app to enable saving. You can still take the built-in quiz.";
    }
}
