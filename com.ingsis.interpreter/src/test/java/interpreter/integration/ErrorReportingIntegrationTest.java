/*
 * My Project
 */

package interpreter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import interpreter.fake.FakeEnvProvider;
import interpreter.fake.FakeInputProvider;
import interpreter.fake.SpyOutput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import version.Version;

/** Syntactic, semantic and runtime errors are labelled and located. */
class ErrorReportingIntegrationTest {

    private final SpyOutput output = new SpyOutput();

    private String errorRunning(Version version, String source) {
        Result<SemanticEnvironment> result =
                new PrintScriptRunner(version, new FakeInputProvider("x"), new FakeEnvProvider())
                        .run(source, output);
        assertFalse(result.isCorrect(), () -> "expected failure but was " + result);
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    @Test
    @DisplayName(
            "Given: 1 / 0 | When: running | Then: fails with a runtime error mentioning 'Division"
                    + " by zero'")
    void interpreter_whenDividingByZero_thenRuntimeError() {
        // Given
        String source = "println(1 / 0);";

        // When
        String error = errorRunning(Version.V_1_1, source);

        // Then
        assertTrue(error.startsWith("Runtime error:"));
        assertTrue(error.contains("Division by zero"));
    }

    @Test
    @DisplayName(
            "Given: a runtime error on line 2 | When: running | Then: the error carries the range"
                    + " of the failing statement")
    void interpreter_whenRuntimeErrorOnSecondLine_thenReportsItsRange() {
        // Given
        String source = "let a: number = 0;\nprintln(5 / a);";

        // When
        String error = errorRunning(Version.V_1_1, source);

        // Then
        assertTrue(error.contains("[from line 2, column 1 to line 2"));
    }

    @Test
    @DisplayName(
            "Given: a runtime error after a println | When: running | Then: earlier output is kept"
                    + " and later statements do not run")
    void interpreter_whenRuntimeErrorMidProgram_thenStopsAfterEarlierOutput() {
        // Given
        String source = "println(\"before\");\nprintln(1 / 0);\nprintln(\"after\");";

        // When
        errorRunning(Version.V_1_1, source);

        // Then
        assertEquals(List.of("before"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: a declaration missing its value | When: running | Then: fails with a syntactic"
                    + " error located at the statement start")
    void interpreter_whenStatementIsMalformed_thenSyntacticErrorWithPosition() {
        // Given
        String source = "let x: number = ;";

        // When
        String error = errorRunning(Version.V_1_1, source);

        // Then
        assertTrue(error.startsWith("Syntactic error:"));
        assertTrue(error.contains("[in statement starting at line 1, column 1]"));
    }

    @Test
    @DisplayName(
            "Given: a string assigned to a number variable | When: running | Then: fails with a"
                    + " semantic error and a source range")
    void interpreter_whenTypesMismatch_thenSemanticErrorWithRange() {
        // Given
        String source = "let x: number = \"hello\";";

        // When
        String error = errorRunning(Version.V_1_1, source);

        // Then
        assertTrue(error.startsWith("Semantic error:"));
        assertTrue(error.contains("[from line 1, column 1 to line 1"));
    }

    @Test
    @DisplayName(
            "Given: an undeclared variable | When: printing it | Then: fails with a semantic error"
                    + " naming the variable")
    void interpreter_whenVariableIsUndeclared_thenSemanticError() {
        // Given
        String source = "println(ghost);";

        // When
        String error = errorRunning(Version.V_1_1, source);

        // Then
        assertTrue(error.startsWith("Semantic error:"));
        assertTrue(error.contains("ghost"));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 | When: calling readInput | Then: fails saying the function is"
                    + " not supported in 1.0")
    void interpreter_whenReadInputInVersion10_thenNotSupported() {
        // Given
        String source = "let x: string = readInput(\"n\");";

        // When
        String error = errorRunning(Version.V_1_0, source);

        // Then
        assertTrue(error.contains("Function 'readInput' is not supported in PrintScript 1.0"));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 | When: calling readEnv | Then: fails saying the function is"
                    + " not supported in 1.0")
    void interpreter_whenReadEnvInVersion10_thenNotSupported() {
        // Given
        String source = "let x: string = readEnv(\"HOME\");";

        // When
        String error = errorRunning(Version.V_1_0, source);

        // Then
        assertTrue(error.contains("Function 'readEnv' is not supported in PrintScript 1.0"));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 | When: using an if statement | Then: the program is"
                    + " rejected")
    void interpreter_whenIfInVersion10_thenRejected() {
        // Given
        String source = "if (true) {\n  println(\"x\");\n}";

        // When
        String error = errorRunning(Version.V_1_0, source);

        // Then
        assertTrue(error.contains("error"));
        assertTrue(output.lines().isEmpty());
    }
}
