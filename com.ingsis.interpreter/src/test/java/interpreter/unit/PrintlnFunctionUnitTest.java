/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.PrintlnFunction;
import interpreter.fake.SpyOutput;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrintlnFunctionUnitTest {

    @Test
    @DisplayName("Given: PrintlnFunction | When: asking its name | Then: is println")
    void println_whenAskingName_thenIsPrintln() {
        // Given
        PrintlnFunction println = new PrintlnFunction();

        // When
        String name = println.name();

        // Then
        assertEquals("println", name);
    }

    @Test
    @DisplayName("Given: a string argument | When: executing println | Then: emits the text")
    void println_whenArgumentIsString_thenEmitsIt() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        new PrintlnFunction().execute(List.of("Hello"), output);

        // Then
        assertEquals(List.of("Hello"), output.lines());
    }

    @Test
    @DisplayName("Given: the number 1E+2 | When: executing println | Then: emits 100")
    void println_whenArgumentIsExponentNumber_thenEmitsPlainNumber() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        new PrintlnFunction().execute(List.of(new BigDecimal("1E+2")), output);

        // Then
        assertEquals(List.of("100"), output.lines());
    }

    @Test
    @DisplayName("Given: the number 3.0 | When: executing println | Then: emits 3")
    void println_whenArgumentHasTrailingZeros_thenEmitsNormalizedNumber() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        new PrintlnFunction().execute(List.of(new BigDecimal("3.0")), output);

        // Then
        assertEquals(List.of("3"), output.lines());
    }

    @Test
    @DisplayName("Given: a boolean argument | When: executing println | Then: emits true")
    void println_whenArgumentIsBoolean_thenEmitsLiteral() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        new PrintlnFunction().execute(List.of(Boolean.TRUE), output);

        // Then
        assertEquals(List.of("true"), output.lines());
    }

    @Test
    @DisplayName("Given: no arguments | When: executing println | Then: emits nothing")
    void println_whenNoArguments_thenEmitsNothing() {
        // Given
        SpyOutput output = new SpyOutput();

        // When
        new PrintlnFunction().execute(List.of(), output);

        // Then
        assertTrue(output.lines().isEmpty());
    }

    @Test
    @DisplayName("Given: no output emitter | When: executing println | Then: does not fail")
    void println_whenEmitterIsNull_thenDoesNothing() {
        // Given
        PrintlnFunction println = new PrintlnFunction();

        // When / Then
        assertDoesNotThrow(() -> println.execute(List.of("x"), null));
    }
}
