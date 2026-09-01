/*
 * My Project
 */

package engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import result.Result;
import service.ExecuteService;
import service.FormatService;
import version.Version;

public class FormatServiceTest {

    @Test
    void testFormatServicePipelineV1_0() {
        String inputCode =
                """
                let a: number = 12;
                let b: number = 4;
                println(a + b);
                """;

        FormatService formatService = new FormatService();
        ByteArrayInputStream in =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        Result<String> result = formatService.format(Version.V_1_0, in, writer);

        assertTrue(result.isCorrect(), "FormatService pipeline should succeed");
        String formatted = writer.toString();
        assertTrue(formatted.contains("let a: number = 12;"));
        assertTrue(formatted.contains("let b: number = 4;"));
        assertTrue(formatted.contains("println(a + b);"));
    }

    @Test
    void testCliEngineFormatOperation() {
        String inputCode = "let x: string = \"hello\";\nprintln(x);\n";

        Engine engine = new CliEngine();
        ByteArrayInputStream in =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        Result<String> result = engine.format(Version.V_1_0, in, null, writer);

        assertTrue(result.isCorrect());
        String formatted = writer.toString();
        assertTrue(formatted.contains("let x: string = \"hello\";"));
        assertTrue(formatted.contains("println(x);"));
    }

    @Test
    void testYamlFormattingRulesIntegration() {
        String yamlConfig =
                """
                space-before-colon: true
                space-after-colon: true
                space-around-equals: false
                indent-inside-if: 2
                """;

        String inputCode =
                """
                const flag: boolean = true;
                if (flag) {
                    println("OK");
                }
                """;

        FormatService formatService = new FormatService();
        ByteArrayInputStream in =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream config =
                new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        Result<String> result = formatService.format(Version.V_1_1, in, config, writer);

        assertTrue(result.isCorrect());
        String formatted = writer.toString();
        assertTrue(formatted.contains("const flag : boolean=true;"));
        assertTrue(formatted.contains("  println(\"OK\");"));
    }

    @Test
    void testFormatCodeWithSemanticErrorShouldFormatButFailOnExecute() {
        // Syntactically valid code, but semantically invalid (reassigning a const variable)
        String inputCode =
                """
                const x: number = 5;
                x = 10;
                """;

        FormatService formatService = new FormatService();
        ByteArrayInputStream inFormat =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        Result<String> formatResult = formatService.format(Version.V_1_1, inFormat, writer);

        assertTrue(
                formatResult.isCorrect(),
                "Formatting syntactically valid code should succeed even if semantic checks fail");
        String formatted = writer.toString();
        assertTrue(formatted.contains("const x: number = 5;"));
        assertTrue(formatted.contains("x = 10;"));

        ExecuteService executeService = new ExecuteService();
        ByteArrayInputStream inExec =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        Result<String> execResult = executeService.execute(Version.V_1_1, null, null, inExec);

        assertFalse(
                execResult.isCorrect(),
                "Execution should fail during semantic checks due to constant re-assignment");
    }
}
