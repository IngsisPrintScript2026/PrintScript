/*
 * My Project
 */

package interpreter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.provider.InputProvider;
import interpreter.fake.FakeEnvProvider;
import interpreter.fake.FakeInputProvider;
import interpreter.fake.SpyOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import version.Version;

/** readInput / readEnv (PrintScript 1.1) with type coercion to the declared type. */
class InputAndEnvIntegrationTest {

    private final SpyOutput output = new SpyOutput();

    private Result<SemanticEnvironment> runWithInput(String source, String... lines) {
        return new PrintScriptRunner(
                        Version.V_1_1, new FakeInputProvider(lines), new FakeEnvProvider())
                .run(source, output);
    }

    private Result<SemanticEnvironment> runWithEnv(String source, Map<String, String> env) {
        return new PrintScriptRunner(
                        Version.V_1_1, new FakeInputProvider(), new FakeEnvProvider(env))
                .run(source, output);
    }

    private static String errorOf(Result<SemanticEnvironment> result) {
        assertFalse(result.isCorrect(), () -> "expected failure but was " + result);
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    @Test
    @DisplayName(
            "Given: input \"41\" read into a number | When: printing x + 1 | Then: prints 42 (not"
                    + " 411)")
    void interpreter_whenReadInputIntoNumber_thenDoesArithmetic() {
        // Given
        String source = "let x: number = readInput(\"n\");\nprintln(x + 1);";

        // When
        runWithInput(source, "41");

        // Then
        assertEquals(List.of("n", "42"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: input \"Joe\" read into a string | When: greeting | Then: prints the prompt"
                    + " and the greeting")
    void interpreter_whenReadInputIntoString_thenConcatenates() {
        // Given
        String source = "let name: string = readInput(\"Name:\");\nprintln(\"Hi \" + name);";

        // When
        runWithInput(source, "Joe");

        // Then
        assertEquals(List.of("Name:", "Hi Joe"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: a readInput call | When: running | Then: the prompt is printed before the"
                    + " input is read")
    void interpreter_whenReadingInput_thenPromptIsPrintedFirst() {
        // Given
        List<String> events = new ArrayList<>();
        InputProvider input =
                prompt -> {
                    events.add("read");
                    return "5";
                };
        String source = "let x: number = readInput(\"Enter:\");";

        // When
        new PrintScriptRunner(Version.V_1_1, input, new FakeEnvProvider())
                .run(source, line -> events.add("out:" + line));

        // Then
        assertEquals(List.of("out:Enter:", "read"), events);
    }

    @Test
    @DisplayName(
            "Given: input \"true\" read into a boolean | When: using it as if condition | Then:"
                    + " the if branch runs")
    void interpreter_whenReadInputIntoBoolean_thenUsableAsCondition() {
        // Given
        String source = "let b: boolean = readInput(\"b\");\nif (b) {\n  println(\"on\");\n}";

        // When
        runWithInput(source, "true");

        // Then
        assertEquals(List.of("b", "on"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: input \"Hola\" read into a boolean | When: running | Then: fails with a"
                    + " runtime error about parsing the boolean")
    void interpreter_whenReadInputIsNotBoolean_thenRuntimeError() {
        // Given
        String source = "let b: boolean = readInput(\"b\");";

        // When
        String error = errorOf(runWithInput(source, "Hola"));

        // Then
        assertTrue(error.startsWith("Runtime error:"));
        assertTrue(error.contains("Cannot parse input 'Hola' as boolean"));
    }

    @Test
    @DisplayName(
            "Given: a builtin failure | When: reporting it | Then: 'Runtime error:' appears only"
                    + " once")
    void interpreter_whenBuiltinFails_thenRuntimePrefixIsNotDuplicated() {
        // Given
        String source = "let b: boolean = readInput(\"b\");";

        // When
        String error = errorOf(runWithInput(source, "Hola"));

        // Then
        assertEquals(error.indexOf("Runtime error"), error.lastIndexOf("Runtime error"));
    }

    @Test
    @DisplayName(
            "Given: a number variable | When: assigning readInput \"8\" to it | Then: the value is"
                    + " coerced to number")
    void interpreter_whenAssigningReadInputToNumber_thenCoerces() {
        // Given
        String source = "let x: number = 1;\nx = readInput(\"again\");\nprintln(x * 2);";

        // When
        runWithInput(source, "8");

        // Then
        assertEquals(List.of("again", "16"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: env PORT=8080 read into a number | When: printing PORT + 1 | Then: prints"
                    + " 8081")
    void interpreter_whenReadEnvIntoNumber_thenDoesArithmetic() {
        // Given
        String source = "let port: number = readEnv(\"PORT\");\nprintln(port + 1);";

        // When
        runWithEnv(source, Map.of("PORT", "8080"));

        // Then
        assertEquals(List.of("8081"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: env DEBUG=yes read into a boolean | When: running | Then: fails with a runtime"
                    + " error about parsing the boolean")
    void interpreter_whenReadEnvIsNotBoolean_thenRuntimeError() {
        // Given
        String source = "let debug: boolean = readEnv(\"DEBUG\");";

        // When
        String error = errorOf(runWithEnv(source, Map.of("DEBUG", "yes")));

        // Then
        assertTrue(error.startsWith("Runtime error:"));
        assertTrue(error.contains("as boolean"));
    }

    @Test
    @DisplayName(
            "Given: an unset env variable | When: reading it | Then: fails with a runtime error"
                    + " saying it is not set")
    void interpreter_whenReadEnvIsMissing_thenRuntimeError() {
        // Given
        String source = "let home: string = readEnv(\"MISSING\");";

        // When
        String error = errorOf(runWithEnv(source, Map.of()));

        // Then
        assertTrue(error.contains("Environment variable 'MISSING' is not set"));
    }
}
