/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import environment.Environment;
import node.expression.literal.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvironmentUnitTest {

    @Test
    @DisplayName(
            "What: Environment | When: declaring and accessing variable | Then: stores and"
                    + " retrieves variable attributes")
    void environment_whenDeclaringAndAccessingVariable_thenStoresAndRetrievesValue() {
        // What: fresh Environment
        Environment env = new Environment();

        // When: declaring variable "x"
        env.declare("x", "hello", DataType.STRING, true);

        // Then: variable is found and accessible with correct type and mutability
        assertTrue(env.find("x").isPresent());
        assertEquals("hello", env.get("x").value());
        assertEquals(DataType.STRING, env.get("x").type());
        assertTrue(env.get("x").isMutable());

        // Undeclared variable throws
        assertFalse(env.find("y").isPresent());
        assertThrows(RuntimeException.class, () -> env.get("y"));
    }

    @Test
    @DisplayName(
            "What: Environment | When: redeclaring existing variable | Then: throws"
                    + " RuntimeException")
    void environment_whenReDeclaringExistingVariable_thenThrowsException() {
        // What: Environment with variable "x" declared
        Environment env = new Environment();
        env.declare("x", 10, DataType.NUMBER, true);

        // When & Then: redeclaring "x" throws RuntimeException
        assertThrows(RuntimeException.class, () -> env.declare("x", 20, DataType.NUMBER, true));
    }

    @Test
    @DisplayName(
            "What: Environment | When: assigning value to declared variable | Then: updates stored"
                    + " value")
    void environment_whenAssigningNewValue_thenUpdatesStoredValue() {
        // What: Environment with variable "a" declared
        Environment env = new Environment();
        env.declare("a", 10, DataType.NUMBER, true);

        // When: assigning new value 20
        env.assign("a", 20);

        // Then: stored value is updated to 20
        assertEquals(20, env.get("a").value());

        // Assigning to undeclared throws
        assertThrows(RuntimeException.class, () -> env.assign("unassigned", 50));
    }

    @Test
    @DisplayName(
            "What: Environment | When: reassigning constant variable | Then: throws"
                    + " RuntimeException")
    void environment_whenReassigningConstVariable_thenThrowsException() {
        // What: Environment with constant "PI" declared
        Environment env = new Environment();
        env.declare("PI", 3.14, DataType.NUMBER, false);

        // When & Then: reassignment throws RuntimeException
        assertThrows(RuntimeException.class, () -> env.assign("PI", 3.14159));
    }

    @Test
    @DisplayName(
            "What: Environment | When: querying parent and child scopes | Then: correctly resolves"
                    + " scoped variables")
    void environment_whenAccessingParentAndChildScopes_thenResolvesScopingProperly() {
        // What: parent Environment and child Environment
        Environment parent = new Environment();
        parent.declare("globalVar", "global", DataType.STRING, true);

        Environment child = new Environment(parent);
        child.declare("localVar", "local", DataType.STRING, true);

        // When & Then: child finds both global and local variables
        assertEquals("global", child.get("globalVar").value());
        assertEquals("local", child.get("localVar").value());
        assertTrue(child.find("globalVar").isPresent());

        // When: assigning to parent variable from child scope
        child.assign("globalVar", "updatedGlobal");

        // Then: parent reflects update
        assertEquals("updatedGlobal", parent.get("globalVar").value());
    }
}
