package org.example.quiz_app;

import java.util.List;

public final class QuestionBank {
    private QuestionBank() { }

    public static List<Question> builtInQuestions() {
        return List.of(
            new Question("Which keyword creates a subclass in Java?",
                List.of("implements", "extends", "inherits", "instanceof"), 1),
            new Question("Which primitive type stores either true or false?",
                List.of("boolean", "int", "char", "double"), 0),
            new Question("What is the index of the first element in a Java array?",
                List.of("1", "-1", "It depends on the array", "0"), 3),
            new Question("Which method is the entry point of a conventional Java console application?",
                List.of("public void start()", "public static void run()",
                    "public static void main(String[] args)", "private void main()"), 2),
            new Question("Which expression compares the contents of two non-null String objects?",
                List.of("first == second", "first.equals(second)", "first = second", "first.compare(second)"), 1),
            new Question("Which keyword creates a new object instance?",
                List.of("new", "class", "static", "this"), 0),
            new Question("Which collection is an ordered, resizable list?",
                List.of("HashSet", "HashMap", "ArrayList", "TreeSet"), 2),
            new Question("Which block handles an exception thrown inside a try block?",
                List.of("finally", "throws", "throw", "catch"), 3),
            new Question("What is the value of 7 / 2 when both operands are int values?",
                List.of("3", "3.5", "4", "2"), 0),
            new Question("Which JavaFX class represents a clickable button?",
                List.of("Label", "Scene", "Button", "Stage"), 2)
        );
    }
}
