/*
 * My Project
 */

package formatter.unit;

import static formatter.fake.FakeTokens.token;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.FormatContext;
import formatter.TokenStreamFormatter;
import formatter.fake.FailingWriter;
import formatter.fake.FakeTokenIterator;
import formatter.fake.FixedSeparatorRule;
import formatter.rule.FormattingRule;
import formatter.support.FormatContextBuilder;
import java.io.StringWriter;
import java.util.List;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

class TokenStreamFormatterUnitTest {
    private static final List<FormattingRule> NO_RULES = List.of();

    /** Tokens of {@code let x:number=1;} written without spaces on line 1. */
    private static List<Token> gluedDeclaration() {
        return List.of(
                token(TokenType.LET, "let", 1, 1),
                token(TokenType.IDENTIFIER, "x", 1, 5),
                token(TokenType.COLON, ":", 1, 6),
                token(TokenType.NUMBER, "number", 1, 7),
                token(TokenType.EQUAL, "=", 1, 13),
                token(TokenType.NUMBER_LITERAL, "1", 1, 14),
                token(TokenType.SEMICOLON, ";", 1, 15));
    }

    private static String formatTokens(TokenStreamFormatter formatter, List<Token> tokens) {
        StringWriter writer = new StringWriter();
        formatter.format(new FakeTokenIterator(tokens), writer);
        return writer.toString();
    }

    @Test
    @DisplayName(
            "Given: the default formatter | When: formatting a glued declaration token stream |"
                    + " Then: writes it with the default spacing and a trailing newline")
    void formatter_whenDefaultContext_thenWritesDefaultSpacing() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        // When
        String output = formatTokens(formatter, gluedDeclaration());

        // Then
        assertEquals("let x: number = 1;\n", output);
    }

    @Test
    @DisplayName(
            "Given: a token stream | When: formatting to a writer | Then: returns success with"
                    + " 'Formatting completed'")
    void formatter_whenFormattingToWriter_thenReturnsCompletedMessage() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        // When
        Result<String> result =
                formatter.format(new FakeTokenIterator(gluedDeclaration()), new StringWriter());

        // Then
        assertEquals("Formatting completed", ((CorrectResult<String>) result).value());
    }

    @Test
    @DisplayName(
            "Given: no rules | When: two tokens are three columns apart on the same line | Then:"
                    + " the original spaces are rebuilt from their positions")
    void formatter_whenNoRulesAndSameLine_thenRebuildsSpacesFromPositions() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter(new FormatContext(), NO_RULES);
        List<Token> tokens =
                List.of(token(TokenType.LET, "let", 1, 1), token(TokenType.IDENTIFIER, "x", 1, 7));

        // When
        String output = formatTokens(formatter, tokens);

        // Then
        assertEquals("let   x\n", output);
    }

    @Test
    @DisplayName(
            "Given: no rules | When: the next token is two lines below | Then: keeps the empty line"
                    + " between them")
    void formatter_whenNoRulesAndTokensOnDistantLines_thenKeepsLineBreaks() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter(new FormatContext(), NO_RULES);
        List<Token> tokens =
                List.of(token(TokenType.SEMICOLON, ";", 1, 1), token(TokenType.LET, "let", 3, 5));

        // When
        String output = formatTokens(formatter, tokens);

        // Then
        assertEquals(";\n\nlet\n", output);
    }

    @Test
    @DisplayName(
            "Given: no rules | When: a multi-character token is followed by a glued token | Then:"
                    + " no space is inserted (uses the end position of the previous token)")
    void formatter_whenNoRulesAndGluedTokens_thenWritesNoSpace() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter(new FormatContext(), NO_RULES);
        List<Token> tokens =
                List.of(
                        token(TokenType.PRINTLN, "println", 1, 1),
                        token(TokenType.LPAREN, "(", 1, 8));

        // When
        String output = formatTokens(formatter, tokens);

        // Then
        assertEquals("println(\n", output);
    }

    @Test
    @DisplayName(
            "Given: two stub rules that always apply | When: formatting | Then: the last rule in"
                    + " the list decides the separator")
    void formatter_whenSeveralRulesApply_thenLastRuleWins() {
        // Given
        List<FormattingRule> rules =
                List.of(new FixedSeparatorRule("#"), new FixedSeparatorRule("_"));
        TokenStreamFormatter formatter = new TokenStreamFormatter(null, rules);
        List<Token> tokens =
                List.of(token(TokenType.LET, "let", 1, 1), token(TokenType.IDENTIFIER, "x", 1, 5));

        // When
        String output = formatTokens(formatter, tokens);

        // Then
        assertEquals("let_x\n", output);
    }

    @Test
    @DisplayName(
            "Given: an empty token stream | When: formatting | Then: writes nothing (no trailing"
                    + " newline) and succeeds")
    void formatter_whenNoTokens_thenWritesNothing() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();
        StringWriter writer = new StringWriter();

        // When
        Result<String> result = formatter.format(new FakeTokenIterator(List.of()), writer);

        // Then
        assertTrue(result.isCorrect());
        assertEquals("", writer.toString());
    }

    @Test
    @DisplayName(
            "Given: a token stream that fails after one token | When: formatting | Then: fails with"
                    + " 'Formatting error' and the error text")
    void formatter_whenTokenStreamFails_thenReturnsFormattingError() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();
        FakeTokenIterator tokens =
                new FakeTokenIterator(
                        List.of(token(TokenType.LET, "let", 1, 1)), "Lexical error: bad char");

        // When
        Result<String> result = formatter.format(tokens, new StringWriter());

        // Then
        String error = ((IncorrectResult<String>) result).error();
        assertTrue(error.contains("Formatting error"));
        assertTrue(error.contains("bad char"));
    }

    @Test
    @DisplayName(
            "Given: a token stream that fails after one token | When: formatting | Then: the tokens"
                    + " read before the failure were already written (streaming)")
    void formatter_whenTokenStreamFails_thenAlreadyReadTokensWereWritten() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();
        StringWriter writer = new StringWriter();
        FakeTokenIterator tokens =
                new FakeTokenIterator(List.of(token(TokenType.LET, "let", 1, 1)), "broken");

        // When
        formatter.format(tokens, writer);

        // Then
        assertEquals("let", writer.toString());
    }

    @Test
    @DisplayName(
            "Given: a writer that cannot be written | When: formatting | Then: fails with"
                    + " 'Formatting error' and the I/O message")
    void formatter_whenWriterFails_thenReturnsFormattingError() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        // When
        Result<String> result =
                formatter.format(new FakeTokenIterator(gluedDeclaration()), new FailingWriter());

        // Then
        String error = ((IncorrectResult<String>) result).error();
        assertTrue(error.contains("Formatting error"));
        assertTrue(error.contains("disk full"));
    }

    @Test
    @DisplayName(
            "Given: line-breaks-after-println 2 | When: a statement follows a println statement |"
                    + " Then: leaves two empty lines after the println")
    void formatter_whenLinesAfterPrintlnConfigured_thenAddsEmptyLines() {
        // Given
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksAfterPrintln(2).build();
        TokenStreamFormatter formatter = new TokenStreamFormatter(ctx);
        List<Token> tokens =
                List.of(
                        token(TokenType.PRINTLN, "println", 1, 1),
                        token(TokenType.LPAREN, "(", 1, 8),
                        token(TokenType.NUMBER_LITERAL, "1", 1, 9),
                        token(TokenType.RPAREN, ")", 1, 10),
                        token(TokenType.SEMICOLON, ";", 1, 11),
                        token(TokenType.IDENTIFIER, "x", 1, 12));

        // When
        String output = formatTokens(formatter, tokens);

        // Then
        assertEquals("println(1);\n\n\nx\n", output);
    }

    @Test
    @DisplayName(
            "Given: the streaming formatter | When: asked to format an AST program or node | Then:"
                    + " fails because it only formats token streams")
    void formatter_whenFormattingAst_thenReturnsFailure() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        // When
        Result<String> program = formatter.format(new ProgramNode(List.of(), 1, 1));
        Result<String> node = formatter.formatNode(new IdentifierNode("x", 1, 1));

        // Then
        assertFalse(program.isCorrect());
        assertFalse(node.isCorrect());
    }
}
