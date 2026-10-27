/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sca.ScaContext;

class ScaContextUnitTest {

    @Test
    @DisplayName(
            "Given: no configuration | When: creating the default ScaContext | Then: no naming"
                    + " convention is enforced")
    void scaContext_whenCreatedWithDefaults_thenHasNoIdentifierFormat() {
        // Given
        // When
        ScaContext context = new ScaContext();

        // Then
        assertNull(context.identifierFormat());
    }

    @Test
    @DisplayName(
            "Given: no configuration | When: creating the default ScaContext | Then: the println"
                    + " and readInput argument rules are disabled")
    void scaContext_whenCreatedWithDefaults_thenArgumentRulesAreDisabled() {
        // Given
        // When
        ScaContext context = new ScaContext();

        // Then
        assertFalse(context.mandatoryLiteralOrIdentifierInPrintln());
        assertFalse(context.mandatoryLiteralOrIdentifierInReadInput());
    }

    @Test
    @DisplayName(
            "Given: explicit rule values | When: creating the ScaContext | Then: exposes exactly"
                    + " those values")
    void scaContext_whenCreatedWithExplicitRules_thenExposesThem() {
        // Given
        String format = "snake case";

        // When
        ScaContext context = new ScaContext(format, true, true);

        // Then
        assertEquals("snake case", context.identifierFormat());
        assertTrue(context.mandatoryLiteralOrIdentifierInPrintln());
        assertTrue(context.mandatoryLiteralOrIdentifierInReadInput());
    }
}
