package engine;

import environment.Environment;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import service.ExecuteService;
import version.Version;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EngineTest {

    @Test
    void testExecuteServiceInterpretationV1_0() {
        String code = """
                let a: number = 12;
                let b: number = 4;
                let c: number = a / b;
                println("Result: " + c);
                """;

        List<String> output = new ArrayList<>();
        ExecuteService executeService = new ExecuteService();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = executeService.execute(Version.V_1_0, output::add, null, inputStream);

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
        ByteArrayInputStream in1 = new ByteArrayInputStream(declCode.getBytes(StandardCharsets.UTF_8));
        Result<SemanticEnvironment> res1 = executeService.execute(Version.V_1_0, output::add, null, in1, semanticEnv, runtimeEnv);
        assertTrue(res1.isCorrect(), "Declaration step should succeed");
        semanticEnv = ((CorrectResult<SemanticEnvironment>) res1).value();

        String printCode = "println(a);\n";
        ByteArrayInputStream in2 = new ByteArrayInputStream(printCode.getBytes(StandardCharsets.UTF_8));
        Result<SemanticEnvironment> res2 = executeService.execute(Version.V_1_0, output::add, null, in2, semanticEnv, runtimeEnv);
        if (!res2.isCorrect()) {
            System.err.println("res2 error: " + ((IncorrectResult<SemanticEnvironment>) res2).error());
        }
        assertTrue(res2.isCorrect(), "Print step should succeed using persistent state");
        assertEquals(1, output.size());
        assertEquals("Hola", output.get(0));
    }

    @Test
    void testExecuteServiceInterpretationV1_1ConditionalsAndConst() {
        String code = """
                const flag: boolean = true;
                if (flag) {
                    println("Condition is true");
                } else {
                    println("Condition is false");
                }
                """;

        List<String> output = new ArrayList<>();
        ExecuteService executeService = new ExecuteService();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = executeService.execute(Version.V_1_1, output::add, null, inputStream);

        assertTrue(result.isCorrect(), "Execution V1.1 should succeed");
        assertEquals(1, output.size());
        assertEquals("Condition is true", output.get(0));
    }

    @Test
    void testExecuteServiceSyntacticError() {
        String code = "let a number = 12;"; // missing colon

        ExecuteService executeService = new ExecuteService();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = executeService.execute(Version.V_1_0, null, null, inputStream);

        assertFalse(result.isCorrect(), "Execution should fail on syntax error");
        assertTrue(((IncorrectResult<String>) result).error().contains("Syntactic error"));
    }

    @Test
    void testCliEngineInterpretation() {
        String code = """
                let x: number = 42;
                println(x);
                """;

        List<String> output = new ArrayList<>();
        Engine engine = new CliEngine();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = engine.interpret(Version.V_1_0, output::add, null, inputStream);

        assertTrue(result.isCorrect(), "CLI Engine interpretation should succeed");
        assertEquals(1, output.size());
        assertEquals("42", output.get(0));
    }
}
