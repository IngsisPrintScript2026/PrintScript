/*
 * My Project
 */

package engine.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.CliEngine;
import engine.Engine;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.Result;
import service.ExecuteService;
import service.FormatService;
import version.Version;

class FormatServiceUnitTest {

    @Test
    @DisplayName(
            "What: FormatService | When: formatting V1.0 pipeline | Then: writes formatted output"
                    + " to writer")
    void formatService_whenFormattingV10Pipeline_thenEmitsFormattedResult() {
        // What: unformatted V1.0 source code
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

        // When: formatting stream
        Result<String> result = formatService.format(Version.V_1_0, in, writer);

        // Then: output contains formatted statements
        assertTrue(result.isCorrect(), "FormatService pipeline should succeed");
        String formatted = writer.toString();
        assertTrue(formatted.contains("let a: number = 12;"));
        assertTrue(formatted.contains("let b: number = 4;"));
        assertTrue(formatted.contains("println(a + b);"));
    }

    @Test
    @DisplayName(
            "What: CliEngine format method | When: formatting code | Then: formats output to"
                    + " writer")
    void cliEngine_whenInvokingFormatMethod_thenFormatsSuccessfully() {
        // What: unformatted source code
        String inputCode = "let x: string = \"hello\";\nprintln(x);\n";
        Engine engine = new CliEngine();
        ByteArrayInputStream in =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        // When: formatting with CliEngine
        Result<String> result = engine.format(Version.V_1_0, in, null, writer);

        // Then: format succeeds
        assertTrue(result.isCorrect());
        String formatted = writer.toString();
        assertTrue(formatted.contains("let x: string = \"hello\";"));
        assertTrue(formatted.contains("println(x);"));
    }

    @Test
    @DisplayName(
            "What: FormatService with YAML rules | When: formatting with custom rules | Then:"
                    + " applies rules to output")
    void formatService_whenFormattingWithYamlRules_thenAppliesConfiguredRules() {
        // What: YAML config and input code
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

        // When: formatting with config
        Result<String> result = formatService.format(Version.V_1_1, in, config, writer);

        // Then: formatting complies with YAML rules
        assertTrue(result.isCorrect());
        String formatted = writer.toString();
        assertTrue(formatted.contains("const flag : boolean=true;"));
        assertTrue(formatted.contains("  println(\"OK\");"));
    }

    @Test
    @DisplayName(
            "What: FormatService vs ExecuteService | When: code is semantically invalid | Then:"
                    + " formatting succeeds but execution fails")
    void formatService_whenCodeIsSemanticallyInvalid_thenFormattingSucceedsButExecutionFails() {
        // What: syntactically valid code with semantic error (reassigning const)
        String inputCode =
                """
                const x: number = 5;
                x = 10;
                """;

        FormatService formatService = new FormatService();
        ByteArrayInputStream inFormat =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        // When: formatting code
        Result<String> formatResult = formatService.format(Version.V_1_1, inFormat, writer);

        // Then: formatting succeeds
        assertTrue(
                formatResult.isCorrect(),
                "Formatting syntactically valid code should succeed even if semantic checks fail");
        String formatted = writer.toString();
        assertTrue(formatted.contains("const x: number = 5;"));
        assertTrue(formatted.contains("x = 10;"));

        // When: executing same code
        ExecuteService executeService = new ExecuteService();
        ByteArrayInputStream inExec =
                new ByteArrayInputStream(inputCode.getBytes(StandardCharsets.UTF_8));
        Result<String> execResult = executeService.execute(Version.V_1_1, null, null, inExec);

        // Then: execution fails on semantic check
        assertFalse(
                execResult.isCorrect(),
                "Execution should fail during semantic checks due to constant re-assignment");
    }
}
