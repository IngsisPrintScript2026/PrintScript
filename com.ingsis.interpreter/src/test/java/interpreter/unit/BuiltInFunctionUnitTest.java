/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.DefaultFunctionRegistry;
import builtin.PrintlnFunction;
import builtin.ReadEnvFunction;
import builtin.ReadInputFunction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import node.expression.literal.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BuiltInFunctionUnitTest {

    @Test
    @DisplayName(
            "What: PrintlnFunction | When: executed with fake consumer | Then: outputs string"
                    + " values")
    void printlnFunction_whenExecutedWithFakeConsumer_thenOutputsStringValues() {
        // What: PrintlnFunction and fake output collector list
        PrintlnFunction fn = new PrintlnFunction();
        assertEquals("println", fn.name());
        List<String> output = new ArrayList<>();

        // When: executing println with arguments
        fn.execute(List.of("Hello World"), output::add);

        // Then: consumer receives output
        assertEquals(List.of("Hello World"), output);

        // Empty args and null consumer
        fn.execute(List.of(), output::add);
        fn.execute(List.of("test"), null);
    }

    @Test
    @DisplayName(
            "What: ReadEnvFunction | When: evaluated with fake environment variables | Then:"
                    + " coerces values or throws on invalid types")
    void readEnvFunction_whenEvaluatedWithFakeEnvProvider_thenCoercesValuesCorrectly() {
        // What: fake environment map and ReadEnvFunction
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

        // When & Then: evaluate valid variables
        assertEquals("Alice", fn.evaluate(List.of("USER_NAME"), DataType.STRING));
        assertEquals(new BigDecimal("8080"), fn.evaluate(List.of("PORT"), DataType.NUMBER));
        assertEquals(true, fn.evaluate(List.of("IS_ACTIVE"), DataType.BOOLEAN));
        assertEquals("Alice", fn.evaluate(List.of("USER_NAME"), null));

        // When & Then: missing args or bad coercion throws RuntimeException
        assertThrows(RuntimeException.class, () -> fn.evaluate(List.of(), DataType.STRING));
        assertThrows(
                RuntimeException.class,
                () -> fn.evaluate(List.of("NON_EXISTENT"), DataType.STRING));
        assertThrows(
                RuntimeException.class, () -> fn.evaluate(List.of("BAD_NUM"), DataType.NUMBER));
        assertThrows(
                RuntimeException.class, () -> fn.evaluate(List.of("BAD_BOOL"), DataType.BOOLEAN));

        assertNotNull(new ReadEnvFunction().name());
    }

    @Test
    @DisplayName(
            "What: ReadInputFunction | When: evaluated with fake input provider | Then: coerces"
                    + " input to requested DataType")
    void readInputFunction_whenEvaluatedWithFakeInputProvider_thenCoercesInputCorrectly() {
        // What: fake input provider and ReadInputFunction
        List<String> outputs = new ArrayList<>();
        ReadInputFunction fn =
                new ReadInputFunction(
                        prompt -> {
                            if ("Enter number:".equals(prompt)) return "123.45";
                            if ("Enter boolean:".equals(prompt)) return "false";
                            return "sample input";
                        });

        assertEquals("readInput", fn.name());

        // When: executing prompt
        fn.execute(List.of("Prompt:"), outputs::add);

        // Then: prompt was written to output consumer
        assertEquals(List.of("Prompt:"), outputs);

        // When & Then: evaluate types
        assertEquals(
                "sample input", fn.evaluate(List.of("Prompt:"), DataType.STRING, outputs::add));
        assertEquals(
                new BigDecimal("123.45"),
                fn.evaluate(List.of("Enter number:"), DataType.NUMBER, outputs::add));
        assertEquals(false, fn.evaluate(List.of("Enter boolean:"), DataType.BOOLEAN, outputs::add));

        // When & Then: null or bad inputs throw RuntimeException
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

        assertNotNull(new ReadInputFunction().name());
    }

    @Test
    @DisplayName(
            "What: DefaultFunctionRegistry | When: checking registered functions | Then: confirms"
                    + " builtin availability")
    void functionRegistry_whenCheckingRegisteredFunctions_thenConfirmsBuiltinAvailability() {
        // What: DefaultFunctionRegistry
        DefaultFunctionRegistry registry = new DefaultFunctionRegistry();

        // When & Then: check contains and get
        assertTrue(registry.contains("println"));
        assertTrue(registry.contains("readEnv"));
        assertTrue(registry.contains("readInput"));
        assertFalse(registry.contains("unknownFunc"));
        assertFalse(registry.contains(null));
        assertNull(registry.get(null));
        assertNotNull(registry.get("println"));
    }
}
