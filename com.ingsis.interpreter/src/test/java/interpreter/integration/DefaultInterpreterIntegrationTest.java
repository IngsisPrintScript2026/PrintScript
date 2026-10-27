/*
 * My Project
 */

package interpreter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import interpreter.fake.FakeEnvProvider;
import interpreter.fake.FakeInputProvider;
import interpreter.fake.SpyOutput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.Result;
import semantic.environment.SemanticEnvironment;
import version.Version;

/** Whole programs from the assignment, run from source text through the real pipeline. */
class DefaultInterpreterIntegrationTest {

    private static List<String> printedBy(String source) {
        SpyOutput output = new SpyOutput();
        Result<SemanticEnvironment> result =
                new PrintScriptRunner(Version.V_1_1, new FakeInputProvider(), new FakeEnvProvider())
                        .run(source, output);
        assertTrue(result.isCorrect(), () -> "program failed: " + result);
        return output.lines();
    }

    @Test
    @DisplayName(
            "Given: name and lastName strings | When: printing name + \" \" + lastName | Then:"
                    + " prints \"Joe Doe\"")
    void interpreter_whenConcatenatingNames_thenPrintsFullName() {
        // Given
        String source =
                "let name: string = \"Joe\";\n"
                        + "let lastName: string = \"Doe\";\n"
                        + "println(name + \" \" + lastName);\n";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("Joe Doe"), printed);
    }

    @Test
    @DisplayName(
            "Given: a = 12, b = 4, c = a / b | When: printing \"Result: \" + c | Then: prints"
                    + " \"Result: 3\"")
    void interpreter_whenDividingIntoNewVariable_thenPrintsResult3() {
        // Given
        String source =
                "let a: number = 12;\n"
                        + "let b: number = 4;\n"
                        + "let c: number = a / b;\n"
                        + "println(\"Result: \" + c);\n";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("Result: 3"), printed);
    }

    @Test
    @DisplayName(
            "Given: a = 12, b = 4 | When: reassigning a = a / b and printing | Then: prints"
                    + " \"Result: 3\"")
    void interpreter_whenReassigningWithDivision_thenPrintsResult3() {
        // Given
        String source =
                "let a: number = 12;\n"
                        + "let b: number = 4;\n"
                        + "a = a / b;\n"
                        + "println(\"Result: \" + a);\n";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("Result: 3"), printed);
    }

    @Test
    @DisplayName("Given: a single-quoted string | When: printing it | Then: prints its content")
    void interpreter_whenPrintingSingleQuotedString_thenPrintsContent() {
        // Given
        String source = "println('hello world');";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("hello world"), printed);
    }

    @Test
    @DisplayName(
            "Given: single- and double-quoted strings | When: concatenating them | Then: prints"
                    + " both contents joined")
    void interpreter_whenMixingQuoteStyles_thenConcatenates() {
        // Given
        String source = "let s: string = 'It is';\nprintln(s + \" ok\");";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("It is ok"), printed);
    }

    @Test
    @DisplayName("Given: 2 + 3 * 4 | When: printing | Then: multiplication goes first (14)")
    void interpreter_whenMixingOperators_thenRespectsPrecedence() {
        // Given
        String source = "println(2 + 3 * 4);";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("14"), printed);
    }

    @Test
    @DisplayName("Given: 20 / 2 - 3 | When: printing | Then: division goes first (7)")
    void interpreter_whenDivisionAndSubtraction_thenRespectsPrecedence() {
        // Given
        String source = "println(20 / 2 - 3);";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("7"), printed);
    }

    @Test
    @DisplayName(
            "Given: \"Total: \" + 5 | When: printing | Then: the number is concatenated as text")
    void interpreter_whenConcatenatingStringAndNumber_thenPrintsText() {
        // Given
        String source = "println(\"Total: \" + 5);";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("Total: 5"), printed);
    }

    @Test
    @DisplayName(
            "Given: a number variable | When: concatenating it before a string | Then: prints"
                    + " number then text")
    void interpreter_whenConcatenatingNumberAndString_thenPrintsText() {
        // Given
        String source = "let n: number = 7;\nprintln(n + \" days\");";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("7 days"), printed);
    }

    @Test
    @DisplayName("Given: 10 / 3 | When: printing | Then: prints 3.333333333333333")
    void interpreter_whenDividingTenByThree_thenPrintsDecimal() {
        // Given
        String source = "println(10 / 3);";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("3.333333333333333"), printed);
    }

    @Test
    @DisplayName("Given: 1.5 * 2 | When: printing | Then: prints 3 (not 3.0)")
    void interpreter_whenMultiplyingDecimal_thenPrintsNormalizedNumber() {
        // Given
        String source = "println(1.5 * 2);";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("3"), printed);
    }

    @Test
    @DisplayName("Given: a decimal literal 2.5 | When: printing | Then: prints 2.5")
    void interpreter_whenPrintingDecimalLiteral_thenKeepsDecimals() {
        // Given
        String source = "let x: number = 2.5;\nprintln(x);";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("2.5"), printed);
    }

    @Test
    @DisplayName(
            "Given: several println statements | When: running | Then: lines are printed in"
                    + " source order")
    void interpreter_whenPrintingSeveralLines_thenKeepsOrder() {
        // Given
        String source = "println(\"one\");\nprintln(\"two\");\nprintln(\"three\");";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("one", "two", "three"), printed);
    }

    @Test
    @DisplayName(
            "Given: a declaration without initializer | When: assigning later and printing |"
                    + " Then: prints the assigned value")
    void interpreter_whenDeclaringThenAssigning_thenPrintsAssignedValue() {
        // Given
        String source = "let x: number;\nx = 5;\nprintln(x);";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertEquals(List.of("5"), printed);
    }

    @Test
    @DisplayName("Given: an empty source | When: running | Then: succeeds printing nothing")
    void interpreter_whenSourceIsEmpty_thenPrintsNothing() {
        // Given
        String source = "";

        // When
        List<String> printed = printedBy(source);

        // Then
        assertTrue(printed.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a program in version 1.0 | When: running it with the 1.0 pipeline | Then:"
                    + " println works")
    void interpreter_whenRunningVersion10Program_thenPrints() {
        // Given
        SpyOutput output = new SpyOutput();
        PrintScriptRunner runner =
                new PrintScriptRunner(
                        Version.V_1_0, new FakeInputProvider(), new FakeEnvProvider());

        // When
        Result<SemanticEnvironment> result =
                runner.run("let x: number = 4;\nprintln(x * 2);", output);

        // Then
        assertTrue(result.isCorrect());
        assertEquals(List.of("8"), output.lines());
    }
}
