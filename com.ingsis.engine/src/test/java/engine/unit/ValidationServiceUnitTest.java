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
import service.ValidationService;
import version.Version;

class ValidationServiceUnitTest {

    private final ValidationService validationService = new ValidationService();

    @Test
    @DisplayName(
            "What: ValidationService | When: validating valid V1.0 code | Then: returns successful"
                    + " validation")
    void validationService_whenValidatingValidV10Code_thenSucceeds() {
        // What: valid V1.0 source code
        String code =
                """
                let x: number = 10;
                let y: string = "hello";
                println(y + " world");
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

        // When: validating code
        Result<String> result = validationService.validate(Version.V_1_0, in);

        // Then: validation succeeds
        assertTrue(result.isCorrect(), "Valid 1.0 code should pass validation");
        assertEquals(
                "Validation successful: Syntax and semantics are valid.",
                ((CorrectResult<String>) result).value());
    }

    @Test
    @DisplayName(
            "What: ValidationService | When: validating valid V1.1 code | Then: returns successful"
                    + " validation")
    void validationService_whenValidatingValidV11Code_thenSucceeds() {
        // What: valid V1.1 source code with conditionals and const
        String code =
                """
                const isReady: boolean = true;
                if (isReady) {
                    println("Ready!");
                } else {
                    println("Waiting...");
                }
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

        // When: validating code
        Result<String> result = validationService.validate(Version.V_1_1, in);

        // Then: validation succeeds
        assertTrue(result.isCorrect(), "Valid 1.1 code should pass validation");
        assertEquals(
                "Validation successful: Syntax and semantics are valid.",
                ((CorrectResult<String>) result).value());
    }

    @Test
    @DisplayName(
            "What: ValidationService | When: syntax error is present | Then: reports syntactic"
                    + " error with line and column range")
    void validationService_whenSyntacticErrorPresent_thenReportsSyntaxErrorWithRange() {
        // What: code missing colon
        String code = "let a number = 5;";

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

        // When: validating code
        Result<String> result = validationService.validate(Version.V_1_0, in);

        // Then: fails with syntactic error location
        assertFalse(result.isCorrect(), "Should fail with syntactic error");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("Syntactic error"), "Error should mention syntactic error");
        assertTrue(
                errorMsg.contains("Line") && errorMsg.contains("Column"),
                "Error should mention Line and Column range");
    }

    @Test
    @DisplayName(
            "What: ValidationService | When: undeclared variable is referenced | Then: reports"
                    + " semantic error")
    void validationService_whenUndeclaredVariableUsed_thenReportsSemanticError() {
        // What: code referencing undeclared variable
        String code =
                """
                let x: number = 5;
                println(z);
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

        // When: validating code
        Result<String> result = validationService.validate(Version.V_1_0, in);

        // Then: fails with semantic error
        assertFalse(result.isCorrect(), "Should fail with semantic error on undeclared variable");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("Semantic error"), "Error should mention semantic error");
        assertTrue(
                errorMsg.contains("Line") && errorMsg.contains("Column"),
                "Error should report location range");
    }

    @Test
    @DisplayName(
            "What: ValidationService | When: reassigning constant variable | Then: reports semantic"
                    + " error")
    void validationService_whenReassigningConstVariable_thenReportsSemanticError() {
        // What: code reassigning const
        String code =
                """
                const x: number = 10;
                x = 20;
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

        // When: validating code
        Result<String> result = validationService.validate(Version.V_1_1, in);

        // Then: fails with semantic error
        assertFalse(result.isCorrect(), "Should fail with semantic error when reassigning const");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("Semantic error"), "Error should mention semantic error");
    }
}
