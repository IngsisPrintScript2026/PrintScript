/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.ReadInputFunction;
import interpreter.fake.FakeInputProvider;
import interpreter.fake.SpyOutput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import node.expression.literal.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReadInputFunctionUnitTest {

    private static Object read(String input, DataType target) {
        return new ReadInputFunction(new FakeInputProvider(input))
                .evaluate(List.of("prompt"), target, new SpyOutput());
    }

    private static String failureReading(String input, DataType target) {
        return assertThrows(RuntimeException.class, () -> read(input, target)).getMessage();
    }

    @Test
    @DisplayName("Given: ReadInputFunction | When: asking its name | Then: is readInput")
    void readInput_whenAskingName_thenIsReadInput() {
        // Given
        ReadInputFunction readInput = new ReadInputFunction();

        // When
        String name = readInput.name();

        // Then
        assertEquals("readInput", name);
    }

    @Test
    @DisplayName(
            "Given: a prompt and an input line | When: evaluating | Then: the prompt is emitted"
                    + " before the input is read")
    void readInput_whenEvaluating_thenEmitsPromptBeforeReading() {
        // Given
        List<String> events = new ArrayList<>();
        ReadInputFunction readInput =
                new ReadInputFunction(
                        prompt -> {
                            events.add("read");
                            return "Joe";
                        });

        // When
        readInput.evaluate(List.of("Name:"), DataType.STRING, line -> events.add("out:" + line));

        // Then
        assertEquals(List.of("out:Name:", "read"), events);
    }

    @Test
    @DisplayName("Given: an empty prompt | When: evaluating | Then: nothing is emitted")
    void readInput_whenPromptIsEmpty_thenEmitsNothing() {
        // Given
        SpyOutput output = new SpyOutput();
        ReadInputFunction readInput = new ReadInputFunction(new FakeInputProvider("x"));

        // When
        readInput.evaluate(List.of(""), DataType.STRING, output);

        // Then
        assertTrue(output.lines().isEmpty());
    }

    @Test
    @DisplayName(
            "Given: no arguments | When: evaluating | Then: reads with an empty prompt and emits"
                    + " nothing")
    void readInput_whenNoArguments_thenUsesEmptyPrompt() {
        // Given
        FakeInputProvider input = new FakeInputProvider("x");
        SpyOutput output = new SpyOutput();

        // When
        new ReadInputFunction(input).evaluate(List.of(), DataType.STRING, output);

        // Then
        assertEquals(List.of(""), input.prompts());
        assertTrue(output.lines().isEmpty());
    }

    @Test
    @DisplayName("Given: input \"Hola\" | When: evaluating as string | Then: returns \"Hola\"")
    void readInput_whenTargetIsString_thenReturnsRawInput() {
        // Given / When
        Object value = read("Hola", DataType.STRING);

        // Then
        assertEquals("Hola", value);
    }

    @Test
    @DisplayName("Given: input \"Hola\" | When: evaluating without target | Then: returns text")
    void readInput_whenTargetIsNull_thenReturnsRawInput() {
        // Given / When
        Object value = read("Hola", null);

        // Then
        assertEquals("Hola", value);
    }

    @Test
    @DisplayName("Given: input \" 41 \" | When: evaluating as number | Then: returns 41")
    void readInput_whenTargetIsNumber_thenParsesTrimmedInput() {
        // Given / When
        Object value = read(" 41 ", DataType.NUMBER);

        // Then
        assertEquals(new BigDecimal("41"), value);
    }

    @Test
    @DisplayName(
            "Given: input \"abc\" | When: evaluating as number | Then: fails with 'Cannot parse"
                    + " input'")
    void readInput_whenNumberIsInvalid_thenThrows() {
        // Given / When
        String message = failureReading("abc", DataType.NUMBER);

        // Then
        assertTrue(message.contains("Cannot parse input 'abc' as number"));
    }

    @Test
    @DisplayName("Given: input \"TRUE\" | When: evaluating as boolean | Then: returns true")
    void readInput_whenBooleanTrueInAnyCase_thenReturnsTrue() {
        // Given / When
        Object value = read("TRUE", DataType.BOOLEAN);

        // Then
        assertEquals(Boolean.TRUE, value);
    }

    @Test
    @DisplayName("Given: input \"false\" | When: evaluating as boolean | Then: returns false")
    void readInput_whenBooleanFalse_thenReturnsFalse() {
        // Given / When
        Object value = read("false", DataType.BOOLEAN);

        // Then
        assertEquals(Boolean.FALSE, value);
    }

    @Test
    @DisplayName(
            "Given: input \"Hola\" | When: evaluating as boolean | Then: fails with 'Cannot parse"
                    + " input 'Hola' as boolean'")
    void readInput_whenBooleanIsInvalid_thenThrows() {
        // Given / When
        String message = failureReading("Hola", DataType.BOOLEAN);

        // Then
        assertTrue(message.contains("Cannot parse input 'Hola' as boolean"));
    }

    @Test
    @DisplayName("Given: no more input lines | When: evaluating | Then: fails with 'Input is null'")
    void readInput_whenInputIsExhausted_thenThrows() {
        // Given
        ReadInputFunction readInput = new ReadInputFunction(new FakeInputProvider());

        // When
        RuntimeException error =
                assertThrows(
                        RuntimeException.class,
                        () -> readInput.evaluate(List.of("p"), DataType.STRING, null));

        // Then
        assertTrue(error.getMessage().contains("Input is null"));
    }

    @Test
    @DisplayName("Given: no input provider | When: evaluating as string | Then: returns empty text")
    void readInput_whenProviderIsNull_thenReturnsEmptyString() {
        // Given
        ReadInputFunction readInput = new ReadInputFunction(null);

        // When
        Object value = readInput.evaluate(List.of("p"), DataType.STRING, null);

        // Then
        assertEquals("", value);
    }

    @Test
    @DisplayName(
            "Given: the default ReadInputFunction | When: evaluating as string | Then: returns"
                    + " empty text")
    void readInput_whenDefaultConstructed_thenReadsEmptyString() {
        // Given
        ReadInputFunction readInput = new ReadInputFunction();

        // When
        Object value = readInput.evaluate(List.of(), DataType.STRING, null);

        // Then
        assertEquals("", value);
    }

    @Test
    @DisplayName(
            "Given: readInput used as a statement | When: executing | Then: emits the prompt and"
                    + " consumes one input line")
    void readInput_whenExecutedAsStatement_thenEmitsPromptAndReads() {
        // Given
        FakeInputProvider input = new FakeInputProvider("ignored");
        SpyOutput output = new SpyOutput();

        // When
        new ReadInputFunction(input).execute(List.of("Press enter"), output);

        // Then
        assertEquals(List.of("Press enter"), output.lines());
        assertEquals(List.of("Press enter"), input.prompts());
    }

    @Test
    @DisplayName(
            "Given: readInput as a statement without provider or prompt | When: executing | Then:"
                    + " emits nothing")
    void readInput_whenExecutedWithoutProviderOrPrompt_thenEmitsNothing() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        new ReadInputFunction(null).execute(List.of(), output);

        // Then
        assertTrue(output.lines().isEmpty());
    }
}
