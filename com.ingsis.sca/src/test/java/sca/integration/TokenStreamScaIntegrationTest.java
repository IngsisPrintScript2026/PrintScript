/*
 * My Project
 */

package sca.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.SafeIterator;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;
import sca.ScaContext;
import sca.TokenStreamSca;
import token.Token;

class TokenStreamScaIntegrationTest {

    @Test
    @DisplayName(
            "What: TokenStreamSca | When: code adheres to camel case | Then: analysis passes with"
                    + " zero violations")
    void tokenStreamSca_whenCodeRespectsCamelCase_thenPassesWithoutViolations() {
        // What: code with camelCase identifier
        String code = "let myVar: number = 10;";
        TokenStreamSca sca = new TokenStreamSca(new ScaContext("camel case", false, false));

        // When: analyzing stream
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<List<String>> res = sca.analyze(in);

        // Then: zero violations
        assertTrue(res.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) res).value();
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "What: TokenStreamSca | When: code violates camel case | Then: emits violation message")
    void tokenStreamSca_whenCodeViolatesCamelCase_thenEmitsViolation() {
        // What: code with snake_case identifier
        String code = "let my_var: number = 10;";
        TokenStreamSca sca = new TokenStreamSca(new ScaContext("camel case", false, false));

        // When: analyzing stream
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<List<String>> res = sca.analyze(in);

        // Then: emits violation
        assertTrue(res.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) res).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("does not respect camel case"));
    }

    @Test
    @DisplayName(
            "What: TokenStreamSca | When: println contains binary expression | Then: emits"
                    + " violation message")
    void tokenStreamSca_whenPrintlnContainsExpression_thenEmitsViolation() {
        // What: code with expression in println
        String code = "println(10 + 20);";
        TokenStreamSca sca = new TokenStreamSca(new ScaContext(null, true, false));

        // When: analyzing stream
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<List<String>> res = sca.analyze(in);

        // Then: emits violation
        assertTrue(res.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) res).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("must be a literal or variable"));
    }

    @Test
    @DisplayName(
            "What: TokenStreamSca | When: println contains literal | Then: passes without"
                    + " violations")
    void tokenStreamSca_whenPrintlnContainsLiteral_thenPassesWithoutViolations() {
        // What: code with literal in println
        String code = "println(42);";
        TokenStreamSca sca = new TokenStreamSca(new ScaContext(null, true, false));

        // When: analyzing stream
        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        Result<List<String>> res = sca.analyze(in);

        // Then: zero violations
        assertTrue(res.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) res).value();
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("What: TokenStreamSca | When: stream iterator is null | Then: fails gracefully")
    void tokenStreamSca_whenStreamIsNull_thenReturnsFailure() {
        // What: TokenStreamSca and null SafeIterator
        TokenStreamSca sca = new TokenStreamSca();
        SafeIterator<Token> nullStream = null;

        // When: analyzing null stream
        Result<List<String>> res = sca.analyze(nullStream);

        // Then: returns failure
        assertFalse(res.isCorrect());
    }
}
