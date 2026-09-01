/*
 * My Project
 */

package engine;

import static org.junit.jupiter.api.Assertions.*;

import environment.Environment;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import service.ExecuteService;
import version.Version;

public class EngineTest {

    @Test
    void testExecuteServiceInterpretationV1_0() {
        String code =
                """
                let a: number = 12;
                let b: number = 4;
                let c: number = a / b;
                println("Result: " + c);
                """;

        List<String> output = new ArrayList<>();
        ExecuteService executeService = new ExecuteService();

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result =
                executeService.execute(Version.V_1_0, output::add, null, inputStream);

        assertTrue(result.isCorrect(), "Execution should succeed");
        assertEquals(1, output.size());
        assertEquals("Result: 3", output.get(0));
    }

    @Test
    void testPersistentReplState() {
        ExecuteService executeService = new ExecuteService();
        SemanticEnvironment semanticEnv = new SemanticEnvironment();
        Environment runtimeEnv = new Environment();
        List<String> output = new ArrayList<>();

        String declCode = "let a: string = \"Hola\";\n";
        ByteArrayInputStream in1 =
                new ByteArrayInputStream(declCode.getBytes(StandardCharsets.UTF_8));
        Result<SemanticEnvironment> res1 =
                executeService.execute(
                        Version.V_1_0, output::add, null, in1, semanticEnv, runtimeEnv);
        assertTrue(res1.isCorrect(), "Declaration step should succeed");
        semanticEnv = ((CorrectResult<SemanticEnvironment>) res1).value();

        String printCode = "println(a);\n";
        ByteArrayInputStream in2 =
                new ByteArrayInputStream(printCode.getBytes(StandardCharsets.UTF_8));
        Result<SemanticEnvironment> res2 =
                executeService.execute(
                        Version.V_1_0, output::add, null, in2, semanticEnv, runtimeEnv);
        if (!res2.isCorrect()) {
            System.err.println(
                    "res2 error: " + ((IncorrectResult<SemanticEnvironment>) res2).error());
        }
        assertTrue(res2.isCorrect(), "Print step should succeed using persistent state");
        assertEquals(1, output.size());
        assertEquals("Hola", output.get(0));
    }

    @Test
    void testExecuteServiceInterpretationV1_1ConditionalsAndConst() {
        String code =
                """
                const flag: boolean = true;
                if (flag) {
                    println("Condition is true");
                } else {
                    println("Condition is false");
                }
                """;

        List<String> output = new ArrayList<>();
        ExecuteService executeService = new ExecuteService();

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result =
                executeService.execute(Version.V_1_1, output::add, null, inputStream);

        assertTrue(result.isCorrect(), "Execution V1.1 should succeed");
        assertEquals(1, output.size());
        assertEquals("Condition is true", output.get(0));
    }

    @Test
    void testExecuteServiceSyntacticError() {
        String code = "let a number = 12;"; // missing colon

        ExecuteService executeService = new ExecuteService();
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = executeService.execute(Version.V_1_0, null, null, inputStream);

        assertFalse(result.isCorrect(), "Execution should fail on syntax error");
        assertTrue(((IncorrectResult<String>) result).error().contains("Syntactic error"));
    }

    @Test
    void testExecuteServiceDelegatedEngineMethods() {
        ExecuteService executeService = new ExecuteService();
        String code = "let x: number = 42;\n";

        ByteArrayInputStream inVal =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(executeService.validate(Version.V_1_0, inVal).isCorrect());

        ByteArrayInputStream inFmt =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();
        assertTrue(executeService.format(Version.V_1_0, inFmt, null, writer).isCorrect());

        ByteArrayInputStream inLint =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(executeService.analyze(Version.V_1_0, inLint, null).isCorrect());

        ByteArrayInputStream inInterp =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(executeService.interpret(Version.V_1_0, null, null, inInterp).isCorrect());
    }

    @Test
    void testCliEngineInterpretationAndDelegatedMethods() {
        String code =
                """
                let x: number = 42;
                println(x);
                """;

        List<String> output = new ArrayList<>();
        CliEngine engine = new CliEngine();

        ByteArrayInputStream inInterp =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = engine.interpret(Version.V_1_0, output::add, null, inInterp);

        assertTrue(result.isCorrect(), "CLI Engine interpretation should succeed");
        assertEquals(1, output.size());
        assertEquals("42", output.get(0));

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
    void testCliEngineCommandLineInvocation() throws Exception {
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

        // Operations and aliases
        assertEquals(0, cmd.execute("Execution", "-v", "1.0", "-i", tempInput.getAbsolutePath()));
        assertEquals(0, cmd.execute("interpret", "-v", "1.0", "-i", tempInput.getAbsolutePath()));
        assertEquals(0, cmd.execute("exec", "-v", "1.0", "-i", tempInput.getAbsolutePath()));

        assertEquals(0, cmd.execute("Validation", "-v", "1.0", "-i", tempInput.getAbsolutePath()));
        assertEquals(0, cmd.execute("validate", "-v", "1.0", "-i", tempInput.getAbsolutePath()));

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

        assertEquals(1, cmd.execute("UnknownOp", "-v", "1.0", "-i", tempInput.getAbsolutePath()));

        // Failure during execution
        File tempBad = File.createTempFile("printscript_bad", ".prs");
        tempBad.deleteOnExit();
        try (FileWriter fw = new FileWriter(tempBad)) {
            fw.write("let x number = 100;\n");
        }
        assertEquals(1, cmd.execute("Execution", "-v", "1.0", "-i", tempBad.getAbsolutePath()));
    }

    @Test
    void testCliEngineReplAndStdin() throws Exception {
        InputStream origIn = System.in;
        try {
            // Test interactive REPL with custom stream where available() returns 0
            String replInput = "let a: number = 10;\n\nlet b number = 20;\n\nexit\n";
            InputStream replStream =
                    new InputStream() {
                        private final ByteArrayInputStream inner =
                                new ByteArrayInputStream(
                                        replInput.getBytes(StandardCharsets.UTF_8));

                        @Override
                        public int read() {
                            return inner.read();
                        }

                        @Override
                        public int available() {
                            return 0;
                        }
                    };
            System.setIn(replStream);
            CliEngine cli = new CliEngine();
            int exitCode = cli.call();
            assertEquals(0, exitCode);

            // Test non-empty STDIN executing operation
            String code = "let x: number = 42;\nprintln(x);\n";
            System.setIn(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)));
            CommandLine cmd = new CommandLine(new CliEngine());
            int exitCodeStdin = cmd.execute("Execution", "-v", "1.0");
            assertEquals(0, exitCodeStdin);
        } finally {
            System.setIn(origIn);
        }
    }
}
