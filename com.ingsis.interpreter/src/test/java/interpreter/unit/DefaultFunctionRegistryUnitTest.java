/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.DefaultFunctionRegistry;
import interpreter.fake.FakeEnvProvider;
import interpreter.fake.FakeInputProvider;
import interpreter.fake.SpyBuiltInFunction;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultFunctionRegistryUnitTest {

    private static DefaultFunctionRegistry builtIns() {
        return new DefaultFunctionRegistry(new FakeInputProvider(), new FakeEnvProvider());
    }

    @Test
    @DisplayName(
            "Given: the built-in registry | When: asking for println, readInput and readEnv |"
                    + " Then: all are registered")
    void registry_whenAskingForBuiltIns_thenAllAreRegistered() {
        // Given
        DefaultFunctionRegistry registry = builtIns();

        // When / Then
        assertTrue(registry.contains("println"));
        assertTrue(registry.contains("readInput"));
        assertTrue(registry.contains("readEnv"));
    }

    @Test
    @DisplayName(
            "Given: the built-in registry | When: looking up READINPUT | Then: lookup ignores case")
    void registry_whenLookingUpWithOtherCase_thenFindsFunction() {
        // Given
        DefaultFunctionRegistry registry = builtIns();

        // When
        String name = registry.get("READINPUT").name();

        // Then
        assertEquals("readInput", name);
    }

    @Test
    @DisplayName("Given: the built-in registry | When: looking up 'foo' | Then: it is absent")
    void registry_whenLookingUpUnknownFunction_thenIsAbsent() {
        // Given
        DefaultFunctionRegistry registry = builtIns();

        // When / Then
        assertFalse(registry.contains("foo"));
        assertNull(registry.get("foo"));
    }

    @Test
    @DisplayName("Given: the built-in registry | When: looking up null | Then: it is absent")
    void registry_whenLookingUpNull_thenIsAbsent() {
        // Given
        DefaultFunctionRegistry registry = builtIns();

        // When / Then
        assertFalse(registry.contains(null));
        assertNull(registry.get(null));
    }

    @Test
    @DisplayName(
            "Given: a registry built from a custom list | When: looking it up | Then: returns that"
                    + " function")
    void registry_whenBuiltFromList_thenRegistersEachFunction() {
        // Given
        SpyBuiltInFunction custom = new SpyBuiltInFunction("shout");

        // When
        DefaultFunctionRegistry registry = new DefaultFunctionRegistry(List.of(custom));

        // Then
        assertSame(custom, registry.get("shout"));
    }

    @Test
    @DisplayName("Given: the default registry | When: asking for println | Then: it is registered")
    void registry_whenDefaultConstructed_thenHasPrintln() {
        // Given
        DefaultFunctionRegistry registry = new DefaultFunctionRegistry();

        // When / Then
        assertTrue(registry.contains("println"));
    }
}
