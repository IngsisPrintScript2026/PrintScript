/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import node.expression.literal.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import semantic.environment.SemanticEnvironment;

class SemanticEnvironmentUnitTest {

    @Test
    @DisplayName(
            "What: SemanticEnvironment | When: defining variables and child scopes | Then:"
                    + " correctly manages variable scopes and attributes")
    void semanticEnvironment_whenDefiningVariablesAndScopes_thenCorrectlyManagesScopes() {
        // What: initial SemanticEnvironment
        SemanticEnvironment env = new SemanticEnvironment();

        // When: defining variable "x" as mutable and initialized
        SemanticEnvironment env1 = env.define("x", DataType.NUMBER, true, true);

        // Then: variable is resolved with expected type and mutability
        assertTrue(env1.lookup("x").isPresent());
        assertEquals(DataType.NUMBER, env1.lookup("x").get().type());
        assertTrue(env1.lookup("x").get().isMutable());
        assertTrue(env1.lookup("x").get().isInitialized());

        // When: creating child scope
        SemanticEnvironment child = new SemanticEnvironment(env1);

        // Then: child finds variable from parent and non-existent is empty
        assertTrue(child.lookup("x").isPresent());
        assertFalse(child.lookup("nonExistent").isPresent());
    }
}
