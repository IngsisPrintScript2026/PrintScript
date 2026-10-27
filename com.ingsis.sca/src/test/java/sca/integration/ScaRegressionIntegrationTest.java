/*
 * My Project
 */

package sca.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;
import sca.ScaContext;
import sca.TokenStreamSca;

class ScaRegressionIntegrationTest {
    private static final String READ_INPUT_RULE_ONLY =
            "mandatory-variable-or-literal-in-readInput: true\n";

    @SuppressWarnings("unchecked")
    private static List<String> violations(Result<List<String>> result) {
        return ((CorrectResult<List<String>>) result).value();
    }

    @Test
    @DisplayName(
            "Given: the readInput rule on | When: an assignment receives readInput of an"
                    + " expression | Then: the violation is reported, as it is for declarations")
    void astSca_whenAssigningReadInputOfExpression_thenReportsViolation() {
        // Given
        String source = "let n: string = \"x\";\nn = readInput(\"a\" + \"b\");\n";

        // When
        List<String> found = violations(LintRunner.lint(source, READ_INPUT_RULE_ONLY));

        // Then
        assertEquals(1, found.size(), found.toString());
        assertTrue(
                found.get(0).contains("Function 'readInput'") && found.get(0).contains("line 2"));
    }

    @Test
    @DisplayName(
            "Given: only the readInput rule on | When: the token-based analyzer sees println of"
                    + " an expression | Then: nothing is reported, println is not readInput")
    void tokenStreamSca_whenOnlyReadInputRule_thenIgnoresPrintln() {
        // Given
        TokenStreamSca sca = new TokenStreamSca(new ScaContext(null, false, true));
        ByteArrayInputStream in =
                new ByteArrayInputStream("println(1 + 2);".getBytes(StandardCharsets.UTF_8));

        // When
        Result<List<String>> result = sca.analyze(in);

        // Then
        assertTrue(violations(result).isEmpty(), violations(result).toString());
    }
}
