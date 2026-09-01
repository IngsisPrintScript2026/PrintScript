/*
 * My Project
 */

package engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import service.LintService;
import version.Version;

public class LintServiceTest {

    @Test
    void testLintServiceDetectsSnakeCaseViolation() {
        String yamlRules =
                """
                identifier_format: "camel case"
                """;

        String code =
                """
                let my_variable_name: number = 10;
                """;

        LintService lintService = new LintService();
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream config =
                new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8));

        Result<String> result = lintService.analyze(Version.V_1_0, in, config);

        assertFalse(result.isCorrect(), "SCA should detect snake_case naming violation");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("camel case"));
    }

    @Test
    void testLintServiceDetectsPrintlnExpressionViolation() {
        String yamlRules =
                """
                mandatory-variable-or-literal-in-println: true
                """;

        String code =
                """
                let x: number = 5;
                println(x + 10);
                """;

        LintService lintService = new LintService();
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream config =
                new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8));

        Result<String> result = lintService.analyze(Version.V_1_0, in, config);

        assertFalse(result.isCorrect(), "SCA should detect complex expression in println");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("must be a literal or variable"));
    }

    @Test
    void testLintServiceSuccessAndErrors() {
        LintService lintService = new LintService();

        // Valid code with no config
        String code = "let validName: number = 10;\n";
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> res = lintService.analyze(Version.V_1_0, in);
        assertTrue(res.isCorrect());
        assertEquals(
                "SCA analysis passed with 0 violations", ((CorrectResult<String>) res).value());

        // Syntax error during lint
        String badCode = "let a number = 10;";
        ByteArrayInputStream inBad =
                new ByteArrayInputStream(badCode.getBytes(StandardCharsets.UTF_8));
        Result<String> resBad = lintService.analyze(Version.V_1_0, inBad);
        assertFalse(resBad.isCorrect());
        assertTrue(((IncorrectResult<String>) resBad).error().contains("Syntactic error"));

        // Semantic error during lint
        String semBadCode = "println(undeclaredVar);";
        ByteArrayInputStream inSemBad =
                new ByteArrayInputStream(semBadCode.getBytes(StandardCharsets.UTF_8));
        Result<String> resSemBad = lintService.analyze(Version.V_1_0, inSemBad);
        assertFalse(resSemBad.isCorrect());
        assertTrue(((IncorrectResult<String>) resSemBad).error().contains("Semantic error"));
    }
}
