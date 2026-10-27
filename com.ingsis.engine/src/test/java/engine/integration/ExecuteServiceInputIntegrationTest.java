/*
 * My Project
 */

package engine.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.fake.FakeInputSupplier;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.IncorrectResult;
import result.Result;
import service.ExecuteService;
import version.Version;

class ExecuteServiceInputIntegrationTest {
    private final ExecuteService service = new ExecuteService();
    private final List<String> printed = new ArrayList<>();

    private static InputStream source(String code) {
        return new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName(
            "Given: a number variable initialized with readInput | When: the user types 41 |"
                    + " Then: the value is a number and adding 1 prints 42")
    void execute_whenReadInputIntoNumber_thenConvertsValue() {
        // Given
        FakeInputSupplier input = new FakeInputSupplier("41");
        String code = "let n: number = readInput(\"Enter n\");\nprintln(n + 1);";

        // When
        Result<String> result = service.execute(Version.V_1_1, printed::add, input, source(code));

        // Then
        assertTrue(result.isCorrect());
        assertEquals(List.of("Enter n", "42"), printed);
        assertEquals(List.of("Enter n"), input.prompts());
    }

    @Test
    @DisplayName(
            "Given: a boolean variable read with readInput | When: the user types 'Hola' | Then:"
                    + " execution fails because it is not a boolean")
    void execute_whenReadInputCannotBeConverted_thenFails() {
        // Given
        FakeInputSupplier input = new FakeInputSupplier("Hola");
        String code = "let b: boolean = readInput(\"ok?\");\nprintln(b);";

        // When
        Result<String> result = service.execute(Version.V_1_1, printed::add, input, source(code));

        // Then
        assertFalse(result.isCorrect());
        assertTrue(((IncorrectResult<String>) result).error().contains("'Hola' as boolean"));
    }

    @Test
    @DisplayName(
            "Given: readInput used as the argument of println | When: executing | Then: the input"
                    + " is printed as a string")
    void execute_whenReadInputInsidePrintln_thenPrintsString() {
        // Given
        FakeInputSupplier input = new FakeInputSupplier("hello");

        // When
        Result<String> result =
                service.execute(
                        Version.V_1_1, printed::add, input, source("println(readInput(\"say\"));"));

        // Then
        assertTrue(result.isCorrect());
        assertEquals(List.of("say", "hello"), printed);
    }

    @Test
    @DisplayName(
            "Given: two readInput calls in a boolean if | When: the answers are 'true' and 'Ana' |"
                    + " Then: the then-branch uses the second answer")
    void execute_whenSeveralReadInputs_thenConsumesAnswersInOrder() {
        // Given
        FakeInputSupplier input = new FakeInputSupplier("true", "Ana");
        String code =
                """
                const greet: boolean = readInput("greet?");
                if (greet) {
                    let name: string = readInput("name?");
                    println("Hi " + name);
                } else {
                    println("Bye");
                }
                """;

        // When
        Result<String> result = service.execute(Version.V_1_1, printed::add, input, source(code));

        // Then
        assertTrue(result.isCorrect());
        assertEquals("Hi Ana", printed.get(printed.size() - 1));
    }

    @Test
    @DisplayName(
            "Given: a program that prints and then fails at runtime | When: executing | Then: the"
                    + " output produced before the error is kept and the error has its position")
    void execute_whenRuntimeErrorAfterOutput_thenKeepsPreviousOutput() {
        // Given
        String code = "println(\"before\");\nlet x: number = 1 / 0;\nprintln(\"after\");";

        // When
        Result<String> result =
                service.execute(Version.V_1_0, printed::add, prompt -> "", source(code));

        // Then
        assertFalse(result.isCorrect());
        assertEquals(List.of("before"), printed);
        String error = ((IncorrectResult<String>) result).error();
        assertTrue(error.contains("Runtime error") && error.contains("Division by zero"));
        assertTrue(error.contains("from line 2, column 1 to line 2, column 22"));
    }
}
