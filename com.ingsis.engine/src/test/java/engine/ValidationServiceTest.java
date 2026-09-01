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
import service.ValidationService;
import version.Version;

public class ValidationServiceTest {

    private final ValidationService validationService = new ValidationService();

    @Test
    void testValidationV1_0Success() {
        String code =
                """
                let x: number = 10;
                let y: string = "hello";
                println(y + " world");
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = validationService.validate(Version.V_1_0, in);

        assertTrue(result.isCorrect(), "Valid 1.0 code should pass validation");
        assertEquals(
                "Validation successful: Syntax and semantics are valid.",
                ((CorrectResult<String>) result).value());
    }

    @Test
    void testValidationV1_1Success() {
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
        Result<String> result = validationService.validate(Version.V_1_1, in);

        assertTrue(result.isCorrect(), "Valid 1.1 code should pass validation");
        assertEquals(
                "Validation successful: Syntax and semantics are valid.",
                ((CorrectResult<String>) result).value());
    }

    @Test
    void testValidationSyntacticErrorIncludesLineAndColumnRange() {
        String code = "let a number = 5;"; // Missing colon ':'

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = validationService.validate(Version.V_1_0, in);

        assertFalse(result.isCorrect(), "Should fail with syntactic error");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("Syntactic error"), "Error should mention syntactic error");
        assertTrue(
                errorMsg.contains("Line") && errorMsg.contains("Column"),
                "Error should mention Line and Column range");
    }

    @Test
    void testValidationSemanticErrorUndeclaredVariable() {
        String code =
                """
                let x: number = 5;
                println(z);
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = validationService.validate(Version.V_1_0, in);

        assertFalse(result.isCorrect(), "Should fail with semantic error on undeclared variable");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("Semantic error"), "Error should mention semantic error");
        assertTrue(
                errorMsg.contains("Line") && errorMsg.contains("Column"),
                "Error should report location range");
    }

    @Test
    void testValidationSemanticErrorReassigningConst() {
        String code =
                """
                const x: number = 10;
                x = 20;
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<String> result = validationService.validate(Version.V_1_1, in);

        assertFalse(result.isCorrect(), "Should fail with semantic error when reassigning const");
        String errorMsg = ((IncorrectResult<String>) result).error();
        assertTrue(errorMsg.contains("Semantic error"), "Error should mention semantic error");
    }
}
