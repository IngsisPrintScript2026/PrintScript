/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import environment.Environment;
import java.math.BigDecimal;
import node.expression.literal.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvironmentUnitTest {

    @Test
    @DisplayName("Given: an empty environment | When: declaring x | Then: get returns its value")
    void environment_whenDeclaringVariable_thenGetReturnsValue() {
        // Given
        Environment env = new Environment();

        // When
        env.declare("x", "hello", DataType.STRING, true);

        // Then
        assertEquals("hello", env.get("x").value());
    }

    @Test
    @DisplayName(
            "Given: an empty environment | When: declaring a const | Then: type and immutability"
                    + " are stored")
    void environment_whenDeclaringConst_thenStoresTypeAndImmutability() {
        // Given
        Environment env = new Environment();

        // When
        env.declare("pi", new BigDecimal("3.14"), DataType.NUMBER, false);

        // Then
        assertEquals(DataType.NUMBER, env.get("pi").type());
        assertFalse(env.get("pi").isMutable());
    }

    @Test
    @DisplayName("Given: an empty environment | When: finding an unknown name | Then: is empty")
    void environment_whenFindingUndeclaredVariable_thenReturnsEmpty() {
        // Given
        Environment env = new Environment();

        // When
        boolean found = env.find("y").isPresent();

        // Then
        assertFalse(found);
    }

    @Test
    @DisplayName(
            "Given: an empty environment | When: getting an unknown name | Then: fails with 'not"
                    + " found'")
    void environment_whenGettingUndeclaredVariable_thenThrowsNotFound() {
        // Given
        Environment env = new Environment();

        // When
        RuntimeException error = assertThrows(RuntimeException.class, () -> env.get("y"));

        // Then
        assertTrue(error.getMessage().contains("'y' not found"));
    }

    @Test
    @DisplayName(
            "Given: x already declared | When: declaring x again in the same scope | Then: fails"
                    + " with 'already been declared'")
    void environment_whenRedeclaringInSameScope_thenThrows() {
        // Given
        Environment env = new Environment();
        env.declare("x", BigDecimal.ONE, DataType.NUMBER, true);

        // When
        RuntimeException error =
                assertThrows(
                        RuntimeException.class,
                        () -> env.declare("x", BigDecimal.TEN, DataType.NUMBER, true));

        // Then
        assertTrue(error.getMessage().contains("already been declared"));
    }

    @Test
    @DisplayName(
            "Given: x declared in a parent scope | When: declaring x in a child scope | Then: the"
                    + " child shadows it and the parent keeps its value")
    void environment_whenShadowingInChildScope_thenParentValueIsKept() {
        // Given
        Environment parent = new Environment();
        parent.declare("x", "outer", DataType.STRING, true);
        Environment child = new Environment(parent);

        // When
        child.declare("x", "inner", DataType.STRING, true);

        // Then
        assertEquals("inner", child.get("x").value());
        assertEquals("outer", parent.get("x").value());
    }

    @Test
    @DisplayName("Given: a mutable variable | When: assigning a new value | Then: value is updated")
    void environment_whenAssigningMutableVariable_thenUpdatesValue() {
        // Given
        Environment env = new Environment();
        env.declare("a", BigDecimal.ONE, DataType.NUMBER, true);

        // When
        env.assign("a", BigDecimal.TEN);

        // Then
        assertEquals(BigDecimal.TEN, env.get("a").value());
    }

    @Test
    @DisplayName("Given: a mutable variable | When: assigning | Then: declared type is kept")
    void environment_whenAssigning_thenKeepsDeclaredType() {
        // Given
        Environment env = new Environment();
        env.declare("a", BigDecimal.ONE, DataType.NUMBER, true);

        // When
        env.assign("a", BigDecimal.TEN);

        // Then
        assertEquals(DataType.NUMBER, env.get("a").type());
    }

    @Test
    @DisplayName(
            "Given: a const variable | When: assigning | Then: fails with 'Cannot reassign"
                    + " constant'")
    void environment_whenAssigningConstant_thenThrows() {
        // Given
        Environment env = new Environment();
        env.declare("c", BigDecimal.ONE, DataType.NUMBER, false);

        // When
        RuntimeException error =
                assertThrows(RuntimeException.class, () -> env.assign("c", BigDecimal.TEN));

        // Then
        assertTrue(error.getMessage().contains("Cannot reassign constant variable 'c'"));
    }

    @Test
    @DisplayName(
            "Given: an empty environment | When: assigning an unknown name | Then: fails with 'is"
                    + " not defined'")
    void environment_whenAssigningUndeclaredVariable_thenThrows() {
        // Given
        Environment env = new Environment();

        // When
        RuntimeException error =
                assertThrows(RuntimeException.class, () -> env.assign("z", BigDecimal.ONE));

        // Then
        assertTrue(error.getMessage().contains("'z' is not defined"));
    }

    @Test
    @DisplayName(
            "Given: a variable in the parent scope | When: assigning it from a child scope | Then:"
                    + " the parent value changes")
    void environment_whenAssigningFromChildScope_thenUpdatesParent() {
        // Given
        Environment parent = new Environment();
        parent.declare("x", BigDecimal.ONE, DataType.NUMBER, true);
        Environment child = new Environment(parent);

        // When
        child.assign("x", BigDecimal.TEN);

        // Then
        assertEquals(BigDecimal.TEN, parent.get("x").value());
    }

    @Test
    @DisplayName(
            "Given: a variable in the parent scope | When: reading it from a child scope | Then:"
                    + " get and find resolve it")
    void environment_whenReadingFromChildScope_thenResolvesParentVariable() {
        // Given
        Environment parent = new Environment();
        parent.declare("x", "value", DataType.STRING, true);

        // When
        Environment child = new Environment(parent);

        // Then
        assertEquals("value", child.get("x").value());
        assertTrue(child.find("x").isPresent());
    }

    @Test
    @DisplayName(
            "Given: a variable declared in a child scope | When: finding it from the parent | Then:"
                    + " it is not visible")
    void environment_whenChildDeclares_thenParentCannotSeeIt() {
        // Given
        Environment parent = new Environment();
        Environment child = new Environment(parent);

        // When
        child.declare("inner", "v", DataType.STRING, true);

        // Then
        assertFalse(parent.find("inner").isPresent());
    }
}
