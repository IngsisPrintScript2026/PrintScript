/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.ReadEnvFunction;
import interpreter.fake.FakeEnvProvider;
import interpreter.fake.SpyOutput;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import node.expression.literal.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReadEnvFunctionUnitTest {
    private static final String UNSET = "PRINTSCRIPT_TEST_SURELY_UNSET_VARIABLE";

    private static Object readEnv(String value, DataType target) {
        return new ReadEnvFunction(new FakeEnvProvider(Map.of("VAR", value)))
                .evaluate(List.of("VAR"), target);
    }

    private static String failureReading(String value, DataType target) {
        return assertThrows(RuntimeException.class, () -> readEnv(value, target)).getMessage();
    }

    @Test
    @DisplayName("Given: ReadEnvFunction | When: asking its name | Then: is readEnv")
    void readEnv_whenAskingName_thenIsReadEnv() {
        // Given
        ReadEnvFunction readEnv = new ReadEnvFunction();

        // When
        String name = readEnv.name();

        // Then
        assertEquals("readEnv", name);
    }

    @Test
    @DisplayName("Given: VAR=hello | When: evaluating as string | Then: returns \"hello\"")
    void readEnv_whenTargetIsString_thenReturnsRawValue() {
        // Given / When
        Object value = readEnv("hello", DataType.STRING);

        // Then
        assertEquals("hello", value);
    }

    @Test
    @DisplayName("Given: VAR=hello | When: evaluating without target | Then: returns \"hello\"")
    void readEnv_whenTargetIsNull_thenReturnsRawValue() {
        // Given / When
        Object value = readEnv("hello", null);

        // Then
        assertEquals("hello", value);
    }

    @Test
    @DisplayName("Given: VAR=8080 | When: evaluating as number | Then: returns 8080")
    void readEnv_whenTargetIsNumber_thenParsesValue() {
        // Given / When
        Object value = readEnv("8080", DataType.NUMBER);

        // Then
        assertEquals(new BigDecimal("8080"), value);
    }

    @Test
    @DisplayName(
            "Given: VAR=abc | When: evaluating as number | Then: fails with 'Cannot parse env var"
                    + " value'")
    void readEnv_whenNumberIsInvalid_thenThrows() {
        // Given / When
        String message = failureReading("abc", DataType.NUMBER);

        // Then
        assertTrue(message.contains("Cannot parse env var value 'abc' as number"));
    }

    @Test
    @DisplayName("Given: VAR=True | When: evaluating as boolean | Then: returns true")
    void readEnv_whenBooleanTrue_thenReturnsTrue() {
        // Given / When
        Object value = readEnv("True", DataType.BOOLEAN);

        // Then
        assertEquals(Boolean.TRUE, value);
    }

    @Test
    @DisplayName("Given: VAR=false | When: evaluating as boolean | Then: returns false")
    void readEnv_whenBooleanFalse_thenReturnsFalse() {
        // Given / When
        Object value = readEnv("false", DataType.BOOLEAN);

        // Then
        assertEquals(Boolean.FALSE, value);
    }

    @Test
    @DisplayName("Given: VAR=maybe | When: evaluating as boolean | Then: fails with 'as boolean'")
    void readEnv_whenBooleanIsInvalid_thenThrows() {
        // Given / When
        String message = failureReading("maybe", DataType.BOOLEAN);

        // Then
        assertTrue(message.contains("Cannot parse env var value 'maybe' as boolean"));
    }

    @Test
    @DisplayName("Given: an unset variable | When: evaluating | Then: fails with 'is not set'")
    void readEnv_whenVariableIsMissing_thenThrows() {
        // Given
        ReadEnvFunction readEnv = new ReadEnvFunction(new FakeEnvProvider());

        // When
        RuntimeException error =
                assertThrows(
                        RuntimeException.class,
                        () -> readEnv.evaluate(List.of("MISSING"), DataType.STRING));

        // Then
        assertTrue(error.getMessage().contains("Environment variable 'MISSING' is not set"));
    }

    @Test
    @DisplayName("Given: no arguments | When: evaluating | Then: fails with 'requires 1 argument'")
    void readEnv_whenNoArguments_thenThrows() {
        // Given
        ReadEnvFunction readEnv = new ReadEnvFunction(new FakeEnvProvider());

        // When
        RuntimeException error =
                assertThrows(
                        RuntimeException.class, () -> readEnv.evaluate(List.of(), DataType.STRING));

        // Then
        assertTrue(error.getMessage().contains("requires 1 argument"));
    }

    @Test
    @DisplayName(
            "Given: no provider | When: evaluating an unset variable | Then: falls back to the"
                    + " process environment and fails with 'is not set'")
    void readEnv_whenProviderIsNull_thenUsesProcessEnvironment() {
        // Given
        ReadEnvFunction readEnv = new ReadEnvFunction(null);

        // When
        RuntimeException error =
                assertThrows(
                        RuntimeException.class,
                        () -> readEnv.evaluate(List.of(UNSET), DataType.STRING));

        // Then
        assertTrue(error.getMessage().contains(UNSET));
    }

    @Test
    @DisplayName("Given: readEnv used as a statement | When: executing | Then: emits nothing")
    void readEnv_whenExecutedAsStatement_thenEmitsNothing() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        new ReadEnvFunction(new FakeEnvProvider()).execute(List.of("VAR"), output);

        // Then
        assertTrue(output.lines().isEmpty());
    }
}
