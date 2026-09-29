/*
 * My Project
 */

package engine.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import service.LintService;
import version.Version;

class LintServiceUnitTest {

    @Test
    @DisplayName(
            "What: LintService | When: identifier format violates camel case | Then: reports camel"
                    + " case violation")
    void lintService_whenCodeViolatesSnakeCaseNamingRule_thenReportsCamelCaseViolation() {
        // What: YAML rule for camel case and code with snake_case
        String yamlRules = "identifier_format: \"camel case\"\n";
        String code = "let my_variable_name: number = 10;\n";

        LintService lintService = new LintService();
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream config =
                new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8));

        // When: analyzing code with rules
        Result<String> result = lintService.analyze(Version.V_1_0, in, config);

        // Then: fails reporting camel case violation
        assertFalse(result.isCorrect(), "SCA should detect snake_case naming violation");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("camel case"));
    }

    @Test
    @DisplayName(
            "What: LintService | When: println contains complex expression | Then: reports println"
                    + " expression violation")
    void lintService_whenPrintlnContainsBinaryExpression_thenReportsExpressionViolation() {
        // What: YAML rule requiring literal/variable in println and offending code
        String yamlRules = "mandatory-variable-or-literal-in-println: true\n";
        String code =
                """
                let x: number = 5;
                println(x + 10);
                """;

        LintService lintService = new LintService();
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream config =
                new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8));

        // When: analyzing code with rules
        Result<String> result = lintService.analyze(Version.V_1_0, in, config);

        // Then: fails reporting literal or variable requirement
        assertFalse(result.isCorrect(), "SCA should detect complex expression in println");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("must be a literal or variable"));
    }

    @Test
    @DisplayName(
            "What: LintService | When: analyzing valid code and error conditions | Then: handles"
                    + " success and errors accordingly")
    void lintService_whenAnalyzingValidAndErrorScenarios_thenHandlesCorrectly() {
        // What: LintService
        LintService lintService = new LintService();

        // When: valid code with no config
        String code = "let validName: number = 10;\n";
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> res = lintService.analyze(Version.V_1_0, in);

        // Then: passes with 0 violations
        assertTrue(res.isCorrect());
        assertEquals(
                "SCA analysis passed with 0 violations", ((CorrectResult<String>) res).value());

        // When: syntactic error during lint
        String badCode = "let a number = 10;";
        ByteArrayInputStream inBad =
                new ByteArrayInputStream(badCode.getBytes(StandardCharsets.UTF_8));
        Result<String> resBad = lintService.analyze(Version.V_1_0, inBad);

        // Then: fails with syntactic error
        assertFalse(resBad.isCorrect());
        assertTrue(((IncorrectResult<String>) resBad).error().contains("Syntactic error"));

        // When: semantic error during lint
        String semBadCode = "println(undeclaredVar);";
        ByteArrayInputStream inSemBad =
                new ByteArrayInputStream(semBadCode.getBytes(StandardCharsets.UTF_8));
        Result<String> resSemBad = lintService.analyze(Version.V_1_0, inSemBad);

        // Then: fails with semantic error
        assertFalse(resSemBad.isCorrect());
        assertTrue(((IncorrectResult<String>) resSemBad).error().contains("Semantic error"));
    }
}
