/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import syntactic.version.Version10Strategy;
import syntactic.version.VersionStrategyRegistry;
import version.Version;

class VersionBuiltInFunctionsUnitTest {

    @Test
    @DisplayName(
            "Given: the PrintScript 1.0 strategy | When: asking its built-in functions | Then:"
                    + " only println is available")
    void versionStrategy_whenV10_thenOnlyPrintln() {
        // Given
        VersionStrategyRegistry registry = new VersionStrategyRegistry();

        // When
        Set<String> functions = registry.getStrategy(Version.V_1_0).builtInFunctions();

        // Then
        assertEquals(Set.of("println"), functions);
    }

    @Test
    @DisplayName(
            "Given: the PrintScript 1.1 strategy | When: asking its built-in functions | Then:"
                    + " println, readInput and readEnv are available")
    void versionStrategy_whenV11_thenPrintlnReadInputAndReadEnv() {
        // Given
        VersionStrategyRegistry registry = new VersionStrategyRegistry();

        // When
        Set<String> functions = registry.getStrategy(Version.V_1_1).builtInFunctions();

        // Then
        assertEquals(Set.of("println", "readInput", "readEnv"), functions);
    }

    @Test
    @DisplayName(
            "Given: a registry that only knows 1.0 | When: asking for 1.1 | Then: throws"
                    + " IllegalArgumentException")
    void versionStrategyRegistry_whenVersionNotRegistered_thenThrows() {
        // Given
        VersionStrategyRegistry registry =
                new VersionStrategyRegistry(List.of(new Version10Strategy()));

        // When
        Executable lookup = () -> registry.getStrategy(Version.V_1_1);

        // Then
        assertThrows(IllegalArgumentException.class, lookup);
    }
}
