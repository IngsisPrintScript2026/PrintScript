/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import value.PrintScriptValues;

class PrintScriptValuesUnitTest {

    @Test
    @DisplayName(
            "Given: the number 3.0 | When: normalizing | Then: insignificant zeros are dropped")
    void normalize_whenNumberHasTrailingZeros_thenStripsThem() {
        // Given
        BigDecimal number = new BigDecimal("3.0");

        // When
        BigDecimal normalized = PrintScriptValues.normalize(number);

        // Then
        assertEquals("3", normalized.toPlainString());
        assertEquals(0, normalized.scale());
    }

    @Test
    @DisplayName(
            "Given: the number 1E+2 | When: normalizing | Then: becomes 100 with scale 0 (no"
                    + " exponent)")
    void normalize_whenNumberHasNegativeScale_thenReturnsScaleZero() {
        // Given
        BigDecimal number = new BigDecimal("1E+2");

        // When
        BigDecimal normalized = PrintScriptValues.normalize(number);

        // Then
        assertEquals(new BigDecimal("100"), normalized);
        assertEquals(0, normalized.scale());
    }

    @Test
    @DisplayName("Given: the number 2.50 | When: normalizing | Then: keeps significant decimals")
    void normalize_whenNumberHasSignificantDecimals_thenKeepsThem() {
        // Given
        BigDecimal number = new BigDecimal("2.50");

        // When
        BigDecimal normalized = PrintScriptValues.normalize(number);

        // Then
        assertEquals("2.5", normalized.toPlainString());
    }

    @Test
    @DisplayName("Given: a number with exponent | When: displaying | Then: uses plain notation")
    void display_whenNumberHasExponent_thenPrintsPlainString() {
        // Given
        BigDecimal number = new BigDecimal("1E+2");

        // When
        String shown = PrintScriptValues.display(number);

        // Then
        assertEquals("100", shown);
    }

    @Test
    @DisplayName("Given: the number 3.000 | When: displaying | Then: prints 3")
    void display_whenNumberHasTrailingZeros_thenPrintsWithoutThem() {
        // Given
        BigDecimal number = new BigDecimal("3.000");

        // When
        String shown = PrintScriptValues.display(number);

        // Then
        assertEquals("3", shown);
    }

    @Test
    @DisplayName("Given: a string value | When: displaying | Then: prints it unchanged")
    void display_whenValueIsString_thenPrintsItUnchanged() {
        // Given
        String text = "Hello 1.0";

        // When
        String shown = PrintScriptValues.display(text);

        // Then
        assertEquals("Hello 1.0", shown);
    }

    @Test
    @DisplayName("Given: a boolean value | When: displaying | Then: prints true/false")
    void display_whenValueIsBoolean_thenPrintsLiteral() {
        // Given
        Boolean flag = Boolean.TRUE;

        // When
        String shown = PrintScriptValues.display(flag);

        // Then
        assertEquals("true", shown);
    }

    @Test
    @DisplayName("Given: a null value | When: displaying | Then: prints null")
    void display_whenValueIsNull_thenPrintsNull() {
        // Given
        Object nothing = null;

        // When
        String shown = PrintScriptValues.display(nothing);

        // Then
        assertEquals("null", shown);
    }

    @Test
    @DisplayName(
            "Given: the division context | When: inspecting it | Then: it is DECIMAL64 (16 digits)")
    void divisionContext_whenInspected_thenIsDecimal64() {
        // Given
        MathContext expected = MathContext.DECIMAL64;

        // When
        MathContext actual = PrintScriptValues.DIVISION_CONTEXT;

        // Then
        assertEquals(expected, actual);
    }
}
