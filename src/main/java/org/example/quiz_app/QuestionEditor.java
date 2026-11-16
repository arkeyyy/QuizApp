package org.example.quiz_app;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Modal authoring form. A failed save leaves all user input intact. */
public final class QuestionEditor {
    @FunctionalInterface
    public interface QuestionSaver { void save(Question question) throws IOException; }

    private QuestionEditor() { }

    public static void show(Window owner, boolean dark, QuestionSaver saver) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Add a question");
        dialog.setResizable(true);
        DialogPane pane = dialog.getDialogPane();
        pane.setId("questionEditor");
        pane.getStylesheets().add(QuestionEditor.class.getResource("quiz.css").toExternalForm());
        if (dark) pane.getStyleClass().add("dark");
        pane.setPrefWidth(590);
        pane.setPrefHeight(690);

        VBox form = new VBox(8);
        form.setPadding(new Insets(8, 12, 12, 4));
        Label heading = new Label("New question");
        heading.getStyleClass().add("section-title");
        Label summary = errorLabel("formError");
        form.getChildren().addAll(heading, summary);

        TextArea prompt = new TextArea();
        prompt.setId("promptField");
        prompt.setWrapText(true);
        prompt.setPrefRowCount(3);
        Label promptError = errorLabel("promptError");
        form.getChildren().addAll(labelFor("Question", prompt), prompt, promptError);

        List<TextField> fields = new ArrayList<>();
        List<Label> errors = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TextField field = new TextField();
            field.setId("option" + i);
            Label error = errorLabel("optionError" + i);
            fields.add(field);
            errors.add(error);
            form.getChildren().addAll(labelFor("Answer " + (char) ('A' + i), field), field, error);
        }
        ComboBox<String> correctChoice = new ComboBox<>();
        correctChoice.setId("correctChoice");
        correctChoice.getItems().addAll("A", "B", "C", "D");
        correctChoice.setPromptText("Not selected");
        correctChoice.setMaxWidth(Double.MAX_VALUE);
        Label correctError = errorLabel("correctError");
        form.getChildren().addAll(labelFor("Correct answer", correctChoice), correctChoice, correctError);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setContent(scroll);
        ButtonType saveType = new ButtonType("Save Question", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, saveType);
        Button save = (Button) pane.lookupButton(saveType);
        save.setId("saveQuestion");
        save.getStyleClass().add("primary");
        save.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> options = fields.stream().map(TextField::getText).map(String::strip).toList();
            setError(promptError, prompt.getText().isBlank() ? "Question is missing." : "");
            var unique = new HashSet<String>();
            for (int i = 0; i < 4; i++) {
                String value = options.get(i);
                String error = value.isBlank() ? "Answer choice is missing." :
                    !unique.add(value.toLowerCase(Locale.ROOT)) ? "Duplicate answer choice." : "";
                setError(errors.get(i), error);
            }
            int correctIndex = correctChoice.getSelectionModel().getSelectedIndex();
            setError(correctError, correctIndex < 0 ? "Correct answer is not selected." : "");
            boolean invalid = promptError.isVisible() || correctError.isVisible() || errors.stream().anyMatch(Label::isVisible);
            if (invalid) {
                event.consume();
                setError(summary, "Question contains missing or invalid fields.");
                scroll.setVvalue(0);
                if (promptError.isVisible()) prompt.requestFocus();
                else {
                    int first = -1;
                    for (int i = 0; i < errors.size(); i++) if (errors.get(i).isVisible()) { first = i; break; }
                    if (first >= 0) fields.get(first).requestFocus();
                    else correctChoice.requestFocus();
                }
                return;
            }
            try {
                saver.save(new Question(prompt.getText(), options, correctIndex));
            } catch (IOException | IllegalArgumentException | SecurityException ex) {
                event.consume();
                setError(summary, "Question was not saved. " + ex.getMessage());
                scroll.setVvalue(0);
            }
        });
        dialog.setOnShown(event -> prompt.requestFocus());
        dialog.showAndWait();
    }

    private static Label labelFor(String text, Control control) {
        Label label = new Label(text);
        label.setLabelFor(control);
        label.getStyleClass().add("field-label");
        return label;
    }

    private static Label errorLabel(String id) {
        Label label = new Label();
        label.setId(id);
        label.getStyleClass().add("error");
        label.setWrapText(true);
        setError(label, "");
        return label;
    }

    private static void setError(Label label, String text) {
        label.setText(text);
        label.setVisible(!text.isEmpty());
        label.setManaged(!text.isEmpty());
    }
}
