/*
 * My Project
 */

package interpreter;

import static org.junit.jupiter.api.Assertions.*;

import builtin.DefaultFunctionRegistry;
import builtin.PrintlnFunction;
import builtin.ReadEnvFunction;
import builtin.ReadInputFunction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import node.expression.literal.DataType;
import org.junit.jupiter.api.Test;

class BuiltInFunctionTest {

    @Test
    void testPrintlnFunction() {
        PrintlnFunction fn = new PrintlnFunction();
        assertEquals("println", fn.name());

        List<String> output = new ArrayList<>();
        fn.execute(List.of("Hello World"), output::add);
        assertEquals(List.of("Hello World"), output);

        fn.execute(List.of(), output::add);
        fn.execute(List.of("test"), null);
    }

    @Test
    void testReadEnvFunction() {
        Map<String, String> envVars =
                Map.of(
                        "USER_NAME", "Alice",
                        "PORT", "8080",
                        "IS_ACTIVE", "true",
                        "BAD_NUM", "abc",
                        "BAD_BOOL", "maybe");
        ReadEnvFunction fn = new ReadEnvFunction(envVars::get);
        assertEquals("readEnv", fn.name());
        fn.execute(List.of(), null);

        assertEquals("Alice", fn.evaluate(List.of("USER_NAME"), DataType.STRING));
        assertEquals(new BigDecimal("8080"), fn.evaluate(List.of("PORT"), DataType.NUMBER));
        assertEquals(true, fn.evaluate(List.of("IS_ACTIVE"), DataType.BOOLEAN));
        assertEquals("Alice", fn.evaluate(List.of("USER_NAME"), null));

        assertThrows(RuntimeException.class, () -> fn.evaluate(List.of(), DataType.STRING));
        assertThrows(
                RuntimeException.class,
                () -> fn.evaluate(List.of("NON_EXISTENT"), DataType.STRING));
        assertThrows(
                RuntimeException.class, () -> fn.evaluate(List.of("BAD_NUM"), DataType.NUMBER));
        assertThrows(
                RuntimeException.class, () -> fn.evaluate(List.of("BAD_BOOL"), DataType.BOOLEAN));

        ReadEnvFunction defaultFn = new ReadEnvFunction();
        assertNotNull(defaultFn.name());
    }

    @Test
    void testReadInputFunction() {
        List<String> outputs = new ArrayList<>();
        ReadInputFunction fn =
                new ReadInputFunction(
                        prompt -> {
                            if ("Enter number:".equals(prompt)) return "123.45";
                            if ("Enter boolean:".equals(prompt)) return "false";
                            return "sample input";
                        });

        assertEquals("readInput", fn.name());
        fn.execute(List.of("Prompt:"), outputs::add);
        assertEquals(List.of("Prompt:"), outputs);

        assertEquals(
                "sample input", fn.evaluate(List.of("Prompt:"), DataType.STRING, outputs::add));
        assertEquals(
                new BigDecimal("123.45"),
                fn.evaluate(List.of("Enter number:"), DataType.NUMBER, outputs::add));
        assertEquals(false, fn.evaluate(List.of("Enter boolean:"), DataType.BOOLEAN, outputs::add));

        ReadInputFunction nullInputFn = new ReadInputFunction(prompt -> null);
        assertThrows(
                RuntimeException.class,
                () -> nullInputFn.evaluate(List.of(), DataType.STRING, null));

        ReadInputFunction badNumFn = new ReadInputFunction(prompt -> "invalid_number");
        assertThrows(
                RuntimeException.class, () -> badNumFn.evaluate(List.of(), DataType.NUMBER, null));

        ReadInputFunction badBoolFn = new ReadInputFunction(prompt -> "invalid_bool");
        assertThrows(
                RuntimeException.class,
                () -> badBoolFn.evaluate(List.of(), DataType.BOOLEAN, null));

        ReadInputFunction defaultFn = new ReadInputFunction();
        assertNotNull(defaultFn.name());
    }

    @Test
    void testFunctionRegistry() {
        DefaultFunctionRegistry registry = new DefaultFunctionRegistry();
        assertTrue(registry.contains("println"));
        assertTrue(registry.contains("readEnv"));
        assertTrue(registry.contains("readInput"));
        assertFalse(registry.contains("unknownFunc"));
        assertFalse(registry.contains(null));
        assertNull(registry.get(null));
        assertNotNull(registry.get("println"));
    }
}
