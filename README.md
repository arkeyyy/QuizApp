# Quiz Lab — CSIT228 Quiz App

A JavaFX multiple-choice quiz implementing the **Quiz App** task on page 2 of
`CSIT228-Midterms-Laboratory-v2.pdf`, including both named bonuses.

## Technical details

The project uses Java 21, JavaFX 21.0.6, Maven, and JUnit 5.
No database, account, or network connection is required once dependencies are available.

## Features

- Ten built-in Java fundamentals questions, in a fixed order, with four answer buttons each.
- Answer selection and submission, with inline errors for missing selections.
- Submission reveals correctness, locks the answer, and awards one point for a correct answer.
- **Next Question** becomes available after submission; **View Results** finishes the last question.
- Running score, progress, final score and percentage, and a fresh **Retake Quiz** attempt.
- **Dark mode** switch on every screen, with the preference saved locally.
- Custom questions with four distinct choices and one correct answer.
  Custom questions are saved and appended after the built-in questions on the next attempt.
- Keyboard-accessible controls, visible focus, wrapping text, and scrolling at smaller window sizes.

## Local data

Files live in `${user.home}/.csit228-quiz-app/`:

| File | Contents |
| --- | --- |
| `questions.properties` | Custom questions only; UTF-8, format version 1 |
| `settings.properties` | Light/dark preference; format version 1 |

The application creates these files only when saving. A failed read leaves the file unchanged
and blocks writes to that file; built-in questions remain usable. A failed question save
leaves the form open with its input intact. Failed theme saves still apply for the current session.
Scores are kept only for the current attempt.

## Tests

Tests cover scoring and navigation rules, question validation, storage round trips and
failures, FXML loading, real JavaFX button interactions, the editor, themes, results, and restart.
UI tests require a graphical desktop and briefly open windows. They use temporary storage;
they do not change saved questions or preferences. RGB screenshots are written to
`target/ui-snapshots/` for inspection.

## Structure

- `Question` and `QuestionBank`: validated immutable question data and bundled content.
- `QuizSession`: attempt state, submission guards, scoring, and completion without JavaFX dependencies.
- `LocalStorage`: versioned properties and temporary-file replacement for safe local saves.
- `QuizApplication`, `QuizController`, `QuestionEditor`: JavaFX screens and interaction handling.
- `quiz-view.fxml` and `quiz.css`: layout and shared light/dark styling.

The Perfect Number and Smarter BruteForce tasks in the PDF are outside this application's scope.
