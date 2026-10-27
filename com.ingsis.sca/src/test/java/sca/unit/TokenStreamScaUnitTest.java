/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.SafeIterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import sca.ScaContext;
import sca.TokenStreamSca;
import sca.fake.FakeTokenIterator;
import token.Token;
import token.TokenType;

class TokenStreamScaUnitTest {
    private static final ScaContext SNAKE = new ScaContext("snake case", false, false);
    private static final ScaContext PRINTLN_RULE = new ScaContext(null, true, false);
    private static final ScaContext READ_INPUT_RULE = new ScaContext(null, false, true);

    private static Token tok(TokenType type, String value, int column) {
        return new Token(type, value, new Position(1, column));
    }

    private static List<Token> declaration(TokenType keyword, String name) {
        return List.of(
                tok(keyword, keyword == TokenType.LET ? "let" : "const", 1),
                tok(TokenType.IDENTIFIER, name, 5),
                tok(TokenType.COLON, ":", 5 + name.length()),
                tok(TokenType.NUMBER, "number", 7 + name.length()),
                tok(TokenType.SEMICOLON, ";", 13 + name.length()));
    }

    /** println(1 + 2); with the '+' at column 11. */
    private static List<Token> printlnOfSum() {
        return List.of(
                tok(TokenType.PRINTLN, "println", 1),
                tok(TokenType.LPAREN, "(", 8),
                tok(TokenType.NUMBER_LITERAL, "1", 9),
                tok(TokenType.PLUS, "+", 11),
                tok(TokenType.NUMBER_LITERAL, "2", 13),
                tok(TokenType.RPAREN, ")", 14),
                tok(TokenType.SEMICOLON, ";", 15));
    }

    private static List<Token> readInputOf(TokenType operator, String symbol) {
        return List.of(
                tok(TokenType.IDENTIFIER, "readInput", 1),
                tok(TokenType.LPAREN, "(", 10),
                tok(TokenType.STRING_LITERAL, "\"a\"", 11),
                tok(operator, symbol, 15),
                tok(TokenType.STRING_LITERAL, "\"b\"", 17),
                tok(TokenType.RPAREN, ")", 20));
    }

    private static List<String> violations(Result<List<String>> result) {
        return ((CorrectResult<List<String>>) result).value();
    }

    private static List<String> analyze(List<Token> tokens, ScaContext context) {
        return violations(new TokenStreamSca().analyzeTokens(tokens, context));
    }

    @Test
    @DisplayName(
            "Given: snake case | When: a let declares 'myVar' | Then: reports the name, convention"
                    + " and position")
    void tokenSca_whenLetNameViolatesSnakeCase_thenReportsViolation() {
        // Given
        List<Token> tokens = declaration(TokenType.LET, "myVar");

        // When
        List<String> violations = analyze(tokens, SNAKE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'myVar'"));
        assertTrue(violations.get(0).contains("snake case"));
        assertTrue(violations.get(0).contains("line 1, column 5"));
    }

    @Test
    @DisplayName(
            "Given: snake case | When: a const declares 'maxValue' | Then: constants are checked"
                    + " too")
    void tokenSca_whenConstNameViolates_thenReportsViolation() {
        // Given
        List<Token> tokens = declaration(TokenType.CONST, "maxValue");

        // When
        List<String> violations = analyze(tokens, SNAKE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'maxValue'"));
    }

    @Test
    @DisplayName("Given: camel case | When: a let declares 'myVar' | Then: reports no violation")
    void tokenSca_whenLetNameRespectsCamelCase_thenNoViolation() {
        // Given
        List<Token> tokens = declaration(TokenType.LET, "myVar");

        // When
        List<String> violations = analyze(tokens, new ScaContext("camelcase", false, false));

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: an unknown or missing convention | When: any name is declared | Then: reports"
                    + " no violation")
    void tokenSca_whenConventionUnknownOrMissing_thenNoViolation() {
        // Given
        List<Token> tokens = declaration(TokenType.LET, "Any_Name");

        // When
        List<String> unknown = analyze(tokens, new ScaContext("pascal case", false, false));
        List<String> missing = analyze(tokens, new ScaContext(" ", false, false));

        // Then
        assertTrue(unknown.isEmpty());
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: snake case | When: the stream ends right after 'let' | Then: reports no"
                    + " violation")
    void tokenSca_whenLetIsLastToken_thenNoViolation() {
        // Given
        List<Token> tokens = List.of(tok(TokenType.LET, "let", 1));

        // When
        List<String> violations = analyze(tokens, SNAKE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println(1 + 2) | Then: reports the operator"
                    + " position and argument index 0")
    void tokenSca_whenPrintlnHasExpressionAndRuleOn_thenReportsViolation() {
        // Given
        List<Token> tokens = printlnOfSum();

        // When
        List<String> violations = analyze(tokens, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'println'"));
        assertTrue(violations.get(0).contains("index 0"));
        assertTrue(violations.get(0).contains("line 1, column 11"));
    }

    @Test
    @DisplayName("Given: println rule disabled | When: println(1 + 2) | Then: reports no violation")
    void tokenSca_whenPrintlnHasExpressionAndRulesOff_thenNoViolation() {
        // Given
        List<Token> tokens = printlnOfSum();

        // When
        List<String> violations = analyze(tokens, new ScaContext());

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println(x, 1 * 2) | Then: reports argument index"
                    + " 1")
    void tokenSca_whenSecondArgumentIsExpression_thenReportsIndexOne() {
        // Given
        List<Token> tokens =
                List.of(
                        tok(TokenType.PRINTLN, "println", 1),
                        tok(TokenType.LPAREN, "(", 8),
                        tok(TokenType.IDENTIFIER, "x", 9),
                        tok(TokenType.COMMA, ",", 10),
                        tok(TokenType.NUMBER_LITERAL, "1", 12),
                        tok(TokenType.STAR, "*", 14),
                        tok(TokenType.NUMBER_LITERAL, "2", 16),
                        tok(TokenType.RPAREN, ")", 17));

        // When
        List<String> violations = analyze(tokens, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("index 1"));
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println(1 - 2, x) | Then: reports argument index"
                    + " 0 only")
    void tokenSca_whenFirstOfTwoArgumentsIsExpression_thenReportsIndexZero() {
        // Given
        List<Token> tokens =
                List.of(
                        tok(TokenType.PRINTLN, "println", 1),
                        tok(TokenType.LPAREN, "(", 8),
                        tok(TokenType.NUMBER_LITERAL, "1", 9),
                        tok(TokenType.MINUS, "-", 11),
                        tok(TokenType.NUMBER_LITERAL, "2", 13),
                        tok(TokenType.COMMA, ",", 14),
                        tok(TokenType.IDENTIFIER, "x", 16),
                        tok(TokenType.RPAREN, ")", 17));

        // When
        List<String> violations = analyze(tokens, PRINTLN_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("index 0"));
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println is not followed by '(' | Then: reports no"
                    + " violation")
    void tokenSca_whenPrintlnNotFollowedByParen_thenNoViolation() {
        // Given
        List<Token> tokens =
                List.of(tok(TokenType.PRINTLN, "println", 1), tok(TokenType.PLUS, "+", 9));

        // When
        List<String> violations = analyze(tokens, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println is the last token | Then: reports no"
                    + " violation")
    void tokenSca_whenPrintlnIsLastToken_thenNoViolation() {
        // Given
        List<Token> tokens = List.of(tok(TokenType.PRINTLN, "println", 1));

        // When
        List<String> violations = analyze(tokens, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: readInput rule enabled | When: readInput(\"a\" + \"b\") | Then: reports a"
                    + " readInput violation")
    void tokenSca_whenReadInputHasExpressionAndRuleOn_thenReportsViolation() {
        // Given
        List<Token> tokens = readInputOf(TokenType.PLUS, "+");

        // When
        List<String> violations = analyze(tokens, READ_INPUT_RULE);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'readInput'"));
        assertTrue(violations.get(0).contains("column 15"));
    }

    @Test
    @DisplayName(
            "Given: readInput rule enabled | When: readInput argument uses '/' | Then: division is"
                    + " also an expression")
    void tokenSca_whenReadInputHasDivision_thenReportsViolation() {
        // Given
        List<Token> tokens = readInputOf(TokenType.SLASH, "/");

        // When
        List<String> violations = analyze(tokens, READ_INPUT_RULE);

        // Then
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName(
            "Given: only the println rule enabled | When: readInput(\"a\" + \"b\") | Then: reports"
                    + " no violation")
    void tokenSca_whenReadInputRuleOff_thenNoViolation() {
        // Given
        List<Token> tokens = readInputOf(TokenType.PLUS, "+");

        // When
        List<String> violations = analyze(tokens, PRINTLN_RULE);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: a list-backed token iterator | When: analyzing it | Then: every token is"
                    + " consumed and checked")
    void tokenSca_whenAnalyzingIterator_thenChecksAllTokens() {
        // Given
        SafeIterator<Token> iterator = FakeTokenIterator.of(declaration(TokenType.LET, "badName"));

        // When
        Result<List<String>> result = new TokenStreamSca(SNAKE).analyze(iterator);

        // Then
        assertEquals(1, violations(result).size());
    }

    @Test
    @DisplayName(
            "Given: an iterator that ends with a lexical error | When: analyzing it | Then: tokens"
                    + " read before the error are still checked")
    void tokenSca_whenIteratorEndsWithError_thenChecksTokensReadSoFar() {
        // Given
        SafeIterator<Token> iterator =
                FakeTokenIterator.endingWith(
                        declaration(TokenType.LET, "badName"), "Lexical error at 1:20");

        // When
        Result<List<String>> result = new TokenStreamSca().analyze(iterator, SNAKE);

        // Then
        assertTrue(result.isCorrect());
        assertEquals(1, violations(result).size());
    }

    @Test
    @DisplayName(
            "Given: TokenStreamSca with snake case | When: analyzing with a null context | Then:"
                    + " the default context is used")
    void tokenSca_whenContextIsNull_thenUsesDefault() {
        // Given
        SafeIterator<Token> iterator = FakeTokenIterator.of(declaration(TokenType.LET, "badName"));

        // When
        Result<List<String>> result = new TokenStreamSca(SNAKE).analyze(iterator, null);

        // Then
        assertEquals(1, violations(result).size());
    }

    @Test
    @DisplayName(
            "Given: TokenStreamSca built with a null context | When: analyzing any name | Then: no"
                    + " rule is enforced")
    void tokenSca_whenBuiltWithNullContext_thenNoRules() {
        // Given
        SafeIterator<Token> iterator = FakeTokenIterator.of(declaration(TokenType.LET, "Bad_Name"));

        // When
        Result<List<String>> result = new TokenStreamSca(null).analyze(iterator);

        // Then
        assertTrue(violations(result).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: TokenStreamSca | When: analyzing a null iterator | Then: fails saying the"
                    + " iterator cannot be null")
    void tokenSca_whenIteratorIsNull_thenFails() {
        // Given
        SafeIterator<Token> iterator = null;

        // When
        Result<List<String>> result = new TokenStreamSca().analyze(iterator);

        // Then
        assertFalse(result.isCorrect());
        assertTrue(((IncorrectResult<List<String>>) result).error().contains("cannot be null"));
    }
}
