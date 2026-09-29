/*
 * My Project
 */

package engine.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import environment.Environment;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import service.ExecuteService;
import version.Version;

class ExecuteServiceUnitTest {

    @Test
    @DisplayName(
            "What: ExecuteService | When: executing V1.0 arithmetic and print statements | Then:"
                    + " prints expected division result")
    void executeService_whenExecutingV10DivisionCode_thenOutputsExpectedResult() {
        // What: arithmetic program
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

        // When: executing code
        Result<String> result =
                executeService.execute(Version.V_1_0, output::add, null, inputStream);

        // Then: output contains "Result: 3"
        assertTrue(result.isCorrect(), "Execution should succeed");
        assertEquals(1, output.size());
        assertEquals("Result: 3", output.get(0));
    }

    @Test
    @DisplayName(
            "What: ExecuteService | When: running persistent REPL steps | Then: preserves"
                    + " environment across steps")
    void executeService_whenRunningPersistentReplSteps_thenPreservesState() {
        // What: ExecuteService and persistent environments
        ExecuteService executeService = new ExecuteService();
        SemanticEnvironment semanticEnv = new SemanticEnvironment();
        Environment runtimeEnv = new Environment();
        List<String> output = new ArrayList<>();

        // When: step 1 declares variable
        String declCode = "let a: string = \"Hola\";\n";
        ByteArrayInputStream in1 =
                new ByteArrayInputStream(declCode.getBytes(StandardCharsets.UTF_8));
        Result<SemanticEnvironment> res1 =
                executeService.execute(
                        new service.ExecutionContext(Version.V_1_0, output::add, null, in1),
                        semanticEnv,
                        runtimeEnv);

        // Then: step 1 succeeds and returns updated semantic environment
        assertTrue(res1.isCorrect(), "Declaration step should succeed");
        semanticEnv = ((CorrectResult<SemanticEnvironment>) res1).value();

        // When: step 2 uses variable from step 1
        String printCode = "println(a);\n";
        ByteArrayInputStream in2 =
                new ByteArrayInputStream(printCode.getBytes(StandardCharsets.UTF_8));
        Result<SemanticEnvironment> res2 =
                executeService.execute(
                        new service.ExecutionContext(Version.V_1_0, output::add, null, in2),
                        semanticEnv,
                        runtimeEnv);

        // Then: step 2 succeeds using persistent state and prints "Hola"
        assertTrue(res2.isCorrect(), "Print step should succeed using persistent state");
        assertEquals(1, output.size());
        assertEquals("Hola", output.get(0));
    }

    @Test
    @DisplayName(
            "What: ExecuteService | When: executing V1.1 conditionals and const | Then: executes"
                    + " appropriate branch")
    void executeService_whenExecutingV11ConditionalsAndConst_thenExecutesTargetBranch() {
        // What: V1.1 code with const and if-else
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

        // When: executing code
        Result<String> result =
                executeService.execute(Version.V_1_1, output::add, null, inputStream);

        // Then: true branch executed
        assertTrue(result.isCorrect(), "Execution V1.1 should succeed");
        assertEquals(1, output.size());
        assertEquals("Condition is true", output.get(0));
    }

    @Test
    @DisplayName(
            "What: ExecuteService | When: code contains syntactic error | Then: fails gracefully"
                    + " with syntax error message")
    void executeService_whenSyntacticErrorPresent_thenFailsGracefully() {
        // What: code missing colon
        String code = "let a number = 12;";

        ExecuteService executeService = new ExecuteService();
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

        // When: executing code with syntax error
        Result<String> result = executeService.execute(Version.V_1_0, null, null, inputStream);

        // Then: fails mentioning Syntactic error
        assertFalse(result.isCorrect(), "Execution should fail on syntax error");
        assertTrue(((IncorrectResult<String>) result).error().contains("Syntactic error"));
    }

    @Test
    @DisplayName(
            "What: ExecuteService | When: invoking delegated engine methods | Then: succeeds across"
                    + " validate, format, analyze, and interpret")
    void executeService_whenInvokingDelegatedEngineMethods_thenDelegatesSuccessfully() {
        // What: valid code
        ExecuteService executeService = new ExecuteService();
        String code = "let x: number = 42;\n";

        // When & Then: validate
        ByteArrayInputStream inVal =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(executeService.validate(Version.V_1_0, inVal).isCorrect());

        // When & Then: format
        ByteArrayInputStream inFmt =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();
        assertTrue(executeService.format(Version.V_1_0, inFmt, null, writer).isCorrect());

        // When & Then: analyze
        ByteArrayInputStream inLint =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(executeService.analyze(Version.V_1_0, inLint, null).isCorrect());

        // When & Then: interpret
        ByteArrayInputStream inInterp =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        assertTrue(executeService.interpret(Version.V_1_0, null, null, inInterp).isCorrect());
    }
}
