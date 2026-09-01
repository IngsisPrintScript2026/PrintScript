/*
 * My Project
 */

package interpreter;

import static org.junit.jupiter.api.Assertions.*;

import environment.Environment;
import node.expression.literal.DataType;
import org.junit.jupiter.api.Test;

class EnvironmentTest {

    @Test
    void testDeclarationAndAccess() {
        Environment env = new Environment();
        env.declare("x", "hello", DataType.STRING, true);

        assertTrue(env.find("x").isPresent());
        assertEquals("hello", env.get("x").value());
        assertEquals(DataType.STRING, env.get("x").type());
        assertTrue(env.get("x").isMutable());

        assertFalse(env.find("y").isPresent());
        assertThrows(RuntimeException.class, () -> env.get("y"));
    }

    @Test
    void testReDeclarationThrows() {
        Environment env = new Environment();
        env.declare("x", 10, DataType.NUMBER, true);
        assertThrows(RuntimeException.class, () -> env.declare("x", 20, DataType.NUMBER, true));
    }

    @Test
    void testAssignment() {
        Environment env = new Environment();
        env.declare("a", 10, DataType.NUMBER, true);
        env.assign("a", 20);
        assertEquals(20, env.get("a").value());

        assertThrows(RuntimeException.class, () -> env.assign("unassigned", 50));
    }

    @Test
    void testConstReassignmentThrows() {
        Environment env = new Environment();
        env.declare("PI", 3.14, DataType.NUMBER, false);
        assertThrows(RuntimeException.class, () -> env.assign("PI", 3.14159));
    }

    @Test
    void testParentScoping() {
        Environment parent = new Environment();
        parent.declare("globalVar", "global", DataType.STRING, true);

        Environment child = new Environment(parent);
        child.declare("localVar", "local", DataType.STRING, true);

        assertEquals("global", child.get("globalVar").value());
        assertEquals("local", child.get("localVar").value());
        assertTrue(child.find("globalVar").isPresent());

        child.assign("globalVar", "updatedGlobal");
        assertEquals("updatedGlobal", parent.get("globalVar").value());
    }
}
