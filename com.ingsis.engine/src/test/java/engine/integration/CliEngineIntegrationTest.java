/*
 * My Project
 */

package engine.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.CliEngine;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import result.Result;
import version.Version;

class CliEngineIntegrationTest {

    @Test
    @DisplayName(
            "What: CliEngine direct methods | When: interpreting, validating, formatting, and"
                    + " analyzing | Then: executes pipeline successfully")
    void cliEngine_whenInvokingDirectMethods_thenExecutesPipeline() {
        // What: code and CliEngine
        String code =
                """
                let x: number = 42;
                println(x);
                """;

        List<String> output = new ArrayList<>();
        CliEngine engine = new CliEngine();

        // When: interpreting
        ByteArrayInputStream inInterp =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = engine.interpret(Version.V_1_0, output::add, null, inInterp);

        // Then: output contains "42"
        assertTrue(result.isCorrect(), "CLI Engine interpretation should succeed");
        assertEquals(1, output.size());
        assertEquals("42", output.get(0));

        // When & Then: validate, format, analyze
        ByteArrayInputStream inVal =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(engine.validate(Version.V_1_0, inVal).isCorrect());

        ByteArrayInputStream inFmt =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();
        assertTrue(engine.format(Version.V_1_0, inFmt, null, writer).isCorrect());

        ByteArrayInputStream inLint =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(engine.analyze(Version.V_1_0, inLint, null).isCorrect());
    }

    @Test
    @DisplayName(
            "What: CliEngine command-line interface | When: invoking via picocli with CLI arguments"
                    + " | Then: executes subcommands and exits with expected codes")
    void cliEngine_whenExecutingViaCommandLine_thenProcessesOperationsAndExitCodes()
            throws Exception {
        // What: temporary input, output, and config files
        File tempInput = File.createTempFile("printscript_input", ".prs");
        tempInput.deleteOnExit();
        try (FileWriter fw = new FileWriter(tempInput)) {
            fw.write("let x: number = 100;\nprintln(x);\n");
        }

        File tempOutput = File.createTempFile("printscript_out", ".txt");
        tempOutput.deleteOnExit();

        File tempConfig = File.createTempFile("printscript_config", ".yaml");
        tempConfig.deleteOnExit();
        try (FileWriter fw = new FileWriter(tempConfig)) {
            fw.write("space-before-colon: true\n");
        }

        CommandLine cmd = new CommandLine(new CliEngine());

        // When & Then: test execution and aliases
        assertEquals(0, cmd.execute("Execution", "-v", "1.0", "-i", tempInput.getAbsolutePath()));
        assertEquals(0, cmd.execute("interpret", "-v", "1.0", "-i", tempInput.getAbsolutePath()));
        assertEquals(0, cmd.execute("exec", "-v", "1.0", "-i", tempInput.getAbsolutePath()));

        // When & Then: test validation and aliases
        assertEquals(0, cmd.execute("Validation", "-v", "1.0", "-i", tempInput.getAbsolutePath()));
        assertEquals(0, cmd.execute("validate", "-v", "1.0", "-i", tempInput.getAbsolutePath()));

        // When & Then: test formatting and aliases
        assertEquals(
                0,
                cmd.execute(
                        "Formatting",
                        "-v",
                        "1.0",
                        "-i",
                        tempInput.getAbsolutePath(),
                        "-c",
                        tempConfig.getAbsolutePath(),
                        "-o",
                        tempOutput.getAbsolutePath()));
        assertEquals(
                0,
                cmd.execute(
                        "format",
                        "-v",
                        "1.0",
                        "-i",
                        tempInput.getAbsolutePath(),
                        "-o",
                        tempOutput.getAbsolutePath()));
        assertEquals(
                0,
                cmd.execute(
                        "fmt",
                        "-v",
                        "1.0",
                        "-i",
                        tempInput.getAbsolutePath(),
                        "-o",
                        tempOutput.getAbsolutePath()));

        // When & Then: test analyzing and aliases
        assertEquals(
                0,
                cmd.execute(
                        "Analyzing",
                        "-v",
                        "1.0",
                        "-i",
                        tempInput.getAbsolutePath(),
                        "-c",
                        tempConfig.getAbsolutePath()));
        assertEquals(0, cmd.execute("analyze", "-v", "1.0", "-i", tempInput.getAbsolutePath()));
        assertEquals(0, cmd.execute("lint", "-v", "1.0", "-i", tempInput.getAbsolutePath()));

        // Unknown operation returns 1
        assertEquals(1, cmd.execute("UnknownOp", "-v", "1.0", "-i", tempInput.getAbsolutePath()));

        // Syntax error file returns 1
        File tempBad = File.createTempFile("printscript_bad", ".prs");
        tempBad.deleteOnExit();
        try (FileWriter fw = new FileWriter(tempBad)) {
            fw.write("let x number = 100;\n");
        }
        assertEquals(1, cmd.execute("Execution", "-v", "1.0", "-i", tempBad.getAbsolutePath()));
    }
}
