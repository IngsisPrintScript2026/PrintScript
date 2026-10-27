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

/** PrintScript 1.1 features: const, booleans and if/else, run from source text. */
class ControlFlowIntegrationTest {

    private final SpyOutput output = new SpyOutput();

    private Result<SemanticEnvironment> run(String source) {
        return new PrintScriptRunner(Version.V_1_1, new FakeInputProvider(), new FakeEnvProvider())
                .run(source, output);
    }

    private static String errorOf(Result<SemanticEnvironment> result) {
        assertFalse(result.isCorrect(), () -> "expected failure but was " + result);
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    @Test
    @DisplayName("Given: if (true) with else | When: running | Then: only the if branch prints")
    void interpreter_whenIfConditionIsTrue_thenRunsIfBranch() {
        // Given
        String source = "if (true) {\n  println(\"yes\");\n} else {\n  println(\"no\");\n}";

        // When
        Result<SemanticEnvironment> result = run(source);

        // Then
        assertTrue(result.isCorrect());
        assertEquals(List.of("yes"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: a false boolean variable | When: running if/else on it | Then: only the else"
                    + " branch prints")
    void interpreter_whenIfConditionIsFalseVariable_thenRunsElseBranch() {
        // Given
        String source =
                "let ok: boolean = false;\n"
                        + "if (ok) {\n  println(\"yes\");\n} else {\n  println(\"no\");\n}";

        // When
        run(source);

        // Then
        assertEquals(List.of("no"), output.lines());
    }

    @Test
    @DisplayName("Given: if (false) without else | When: running | Then: nothing is printed")
    void interpreter_whenIfWithoutElseIsFalse_thenPrintsNothing() {
        // Given
        String source = "if (false) {\n  println(\"never\");\n}\nprintln(\"after\");";

        // When
        run(source);

        // Then
        assertEquals(List.of("after"), output.lines());
    }

    @Test
    @DisplayName("Given: nested ifs | When: running | Then: the inner branch prints")
    void interpreter_whenIfsAreNested_thenRunsInnerBranch() {
        // Given
        String source =
                "if (true) {\n"
                        + "  if (false) {\n    println(\"a\");\n  } else {\n    println(\"b\");\n"
                        + "  }\n}";

        // When
        run(source);

        // Then
        assertEquals(List.of("b"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: an outer variable | When: assigning it inside an if block | Then: the change"
                    + " is visible after the block")
    void interpreter_whenAssigningOuterVariableInsideIf_thenChangePersists() {
        // Given
        String source = "let x: number = 1;\nif (true) {\n  x = 2;\n}\nprintln(x);";

        // When
        run(source);

        // Then
        assertEquals(List.of("2"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: if with a number condition | When: running | Then: fails with a semantic error"
                    + " because the condition must be boolean")
    void interpreter_whenIfConditionIsNumber_thenSemanticError() {
        // Given
        String source = "if (1) {\n  println(\"x\");\n}";

        // When
        String error = errorOf(run(source));

        // Then
        assertTrue(error.startsWith("Semantic error:"));
        assertTrue(error.contains("must be boolean"));
    }

    @Test
    @DisplayName(
            "Given: a variable declared inside an if | When: using it after the block | Then:"
                    + " fails because it is not declared")
    void interpreter_whenUsingBlockVariableOutside_thenSemanticError() {
        // Given
        String source = "if (true) {\n  let inner: number = 1;\n}\nprintln(inner);";

        // When
        String error = errorOf(run(source));

        // Then
        assertTrue(error.startsWith("Semantic error:"));
        assertTrue(error.contains("inner"));
    }

    @Test
    @DisplayName("Given: a const | When: printing it | Then: prints its value")
    void interpreter_whenReadingConst_thenPrintsValue() {
        // Given
        String source = "const pi: number = 3.14;\nprintln(pi);";

        // When
        run(source);

        // Then
        assertEquals(List.of("3.14"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: a const | When: reassigning it | Then: fails with a semantic error and the"
                    + " assignment never runs")
    void interpreter_whenReassigningConst_thenSemanticError() {
        // Given
        String source = "const x: number = 1;\nx = 2;\nprintln(x);";

        // When
        String error = errorOf(run(source));

        // Then
        assertTrue(error.startsWith("Semantic error:"));
        assertTrue(error.contains("Cannot reassign constant variable 'x'"));
        assertTrue(output.lines().isEmpty());
    }

    @Test
    @DisplayName("Given: a boolean variable | When: printing it | Then: prints true")
    void interpreter_whenPrintingBoolean_thenPrintsLiteral() {
        // Given
        String source = "let b: boolean = true;\nprintln(b);";

        // When
        run(source);

        // Then
        assertEquals(List.of("true"), output.lines());
    }
}
