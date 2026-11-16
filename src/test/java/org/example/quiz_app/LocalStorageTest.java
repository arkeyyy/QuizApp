package org.example.quiz_app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LocalStorageTest {
    @TempDir Path directory;

    private Question question() {
        return new Question("Which word contains ñ?\nSample: = café", List.of("mañana", "Java", "class", "new"), 0);
    }

    @Test
    void missingFilesUseDefaultsWithoutWritingAnything() {
        LocalStorage storage = new LocalStorage(directory.resolve("missing"));
        assertEquals(List.of(), storage.loadQuestions().value());
        assertNull(storage.loadQuestions().warning());
        assertEquals(LocalStorage.Theme.LIGHT, storage.loadTheme().value());
        assertFalse(Files.exists(directory.resolve("missing")));
    }

    @Test
    void questionsAndThemeSurviveNewStorageInstance() throws IOException {
        LocalStorage storage = new LocalStorage(directory);
        storage.saveQuestions(List.of(question()));
        storage.saveTheme(LocalStorage.Theme.DARK);
        LocalStorage restarted = new LocalStorage(directory);
        assertEquals(List.of(question()), restarted.loadQuestions().value());
        assertEquals(LocalStorage.Theme.DARK, restarted.loadTheme().value());
        restarted.saveQuestions(List.of(question(), question()));
        assertEquals(2, new LocalStorage(directory).loadQuestions().value().size());
        try (var files = Files.list(directory)) {
            assertEquals(2, files.count(), "Temporary save files should be cleaned up");
        }
    }

    @Test
    void malformedQuestionsArePreservedAndCannotBeOverwritten() throws IOException {
        Path path = directory.resolve("questions.properties");
        for (String contents : List.of("version=2\ncount=0", "version=1\ncount=-1", "version=1\ncount=nope",
                "version=1\ncount=1", "version=1\ncount=0\nbad=\\uZZZZ")) {
            Files.writeString(path, contents);
            LocalStorage storage = new LocalStorage(directory);
            assertNotNull(storage.loadQuestions().warning());
            assertEquals(List.of(), storage.loadQuestions().value());
            assertThrows(IOException.class, () -> storage.saveQuestions(List.of(question())));
            assertEquals(contents, Files.readString(path));
        }
    }

    @Test
    void corruptThemeDoesNotPreventSavingQuestions() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "version=1\ntheme=UNKNOWN");
        LocalStorage storage = new LocalStorage(directory);
        assertEquals(LocalStorage.Theme.LIGHT, storage.loadTheme().value());
        assertNotNull(storage.loadTheme().warning());
        assertThrows(IOException.class, () -> storage.saveTheme(LocalStorage.Theme.DARK));
        storage.saveQuestions(List.of(question()));
        assertEquals("version=1\ntheme=UNKNOWN", Files.readString(path));
        assertEquals(List.of(question()), new LocalStorage(directory).loadQuestions().value());
    }

    @Test
    void unreadableStoreIsReportedAndWriteFailureDoesNotEraseExistingData() throws IOException {
        Path unreadable = directory.resolve("questions.properties");
        Files.createDirectory(unreadable);
        LocalStorage broken = new LocalStorage(directory);
        assertNotNull(broken.loadQuestions().warning());
        assertThrows(IOException.class, () -> broken.saveQuestions(List.of(question())));
        assertTrue(Files.isDirectory(unreadable));
        Path fileInsteadOfDirectory = directory.resolve("not-a-directory");
        Files.writeString(fileInsteadOfDirectory, "preserve this data");
        LocalStorage unwritable = new LocalStorage(fileInsteadOfDirectory);
        assertThrows(IOException.class, () -> unwritable.saveTheme(LocalStorage.Theme.DARK));
        assertEquals("preserve this data", Files.readString(fileInsteadOfDirectory));
    }

    @Test
    void saveChecksForCorruptionEvenIfCallerHasNotLoaded() throws IOException {
        Files.writeString(directory.resolve("questions.properties"), "bad data");
        assertThrows(IOException.class, () -> new LocalStorage(directory).saveQuestions(List.of(question())));
        assertEquals("bad data", Files.readString(directory.resolve("questions.properties")));
    }
}
