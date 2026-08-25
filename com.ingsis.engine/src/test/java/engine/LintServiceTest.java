package engine;

import org.junit.jupiter.api.Test;
import result.IncorrectResult;
import result.Result;
import service.LintService;
import version.Version;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class LintServiceTest {

    @Test
    void testLintServiceDetectsSnakeCaseViolation() {
        String yamlRules = """
                identifier_format: "camel case"
                """;

        String code = """
                let my_variable_name: number = 10;
                """;

        LintService lintService = new LintService();
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream config = new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8));

        Result<String> result = lintService.analyze(Version.V_1_0, in, config);

        assertFalse(result.isCorrect(), "SCA should detect snake_case naming violation");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("camel case"));
    }

    @Test
    void testLintServiceDetectsPrintlnExpressionViolation() {
        String yamlRules = """
                mandatory-variable-or-literal-in-println: true
                """;

        String code = """
                let x: number = 5;
                println(x + 10);
                """;

        LintService lintService = new LintService();
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream config = new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8));

        Result<String> result = lintService.analyze(Version.V_1_0, in, config);

        assertFalse(result.isCorrect(), "SCA should detect complex expression in println");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("must be a literal or variable"));
    }
}
