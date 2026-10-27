/*
 * My Project
 */

package lexer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import iterator.SafeIterator;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import lexer.Lexer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

class LexerTokenRangeIntegrationTest {

    /** Outcome of lexing a whole source: tokens read before the first failure, and the failure. */
    private record Lexed(List<Token> tokens, String failure) {}

    private static Lexed lex(String source) {
        SafeIterator<Token> lexer =
                new Lexer(new CharStream(new StreamCharReader(new StringReader(source))));
        List<Token> tokens = new ArrayList<>();
        while (true) {
            Result<IterationStep<Token>> res = lexer.next();
            if (res instanceof IncorrectResult<IterationStep<Token>>(String err)) {
                return new Lexed(tokens, err);
            }
            IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) res).value();
            tokens.add(step.value());
            lexer = step.nextStream();
        }
    }

    private static List<TokenType> types(List<Token> tokens) {
        return tokens.stream().map(Token::type).toList();
    }

    @Test
    @DisplayName(
            "Given: 'let x: number = 5;' | When: lexing | Then: the 'number' token spans line 1"
                    + " column 8 to line 1 column 13")
    void lexer_whenLexingDeclaration_thenTypeKeywordHasInclusiveEnd() {
        // Given
        String source = "let x: number = 5;";

        // When
        Token numberKeyword = lex(source).tokens().get(3);

        // Then
        assertEquals(TokenType.NUMBER, numberKeyword.type());
        assertEquals(new Position(1, 8), numberKeyword.startPosition());
        assertEquals(new Position(1, 13), numberKeyword.endPosition());
        assertEquals(
                "from line 1, column 8 to line 1, column 13", numberKeyword.range().toString());
    }

    @Test
    @DisplayName(
            "Given: a source with '@' at column 19 | When: lexing | Then: fails with a lexical"
                    + " error naming the char and its position")
    void lexer_whenInputHasUnknownChar_thenReportsCharAndPosition() {
        // Given
        String source = "let x: number = 5 @;";

        // When
        Lexed lexed = lex(source);

        // Then
        assertEquals(
                "Lexical error: unrecognized character '@' at line 1, column 19", lexed.failure());
    }

    @Test
    @DisplayName(
            "Given: a source with an unknown char | When: lexing | Then: tokens before the bad char"
                    + " are still produced")
    void lexer_whenInputHasUnknownChar_thenPreviousTokensAreEmitted() {
        // Given
        String source = "let x: number = 5 @;";

        // When
        Lexed lexed = lex(source);

        // Then
        assertEquals(6, lexed.tokens().size());
    }

    @Test
    @DisplayName(
            "Given: an unknown char on the second line | When: lexing | Then: the error reports"
                    + " line 2")
    void lexer_whenUnknownCharOnSecondLine_thenReportsLineTwo() {
        // Given
        String source = "let a: number = 1;\n  #";

        // When
        String failure = lex(source).failure();

        // Then
        assertTrue(failure.contains("'#'"));
        assertTrue(failure.contains("line 2, column 3"));
    }

    @Test
    @DisplayName("Given: a valid source | When: lexing to the end | Then: finishes with 'EOF'")
    void lexer_whenSourceIsValid_thenEndsWithEof() {
        // Given
        String source = "println(1);";

        // When
        Lexed lexed = lex(source);

        // Then
        assertEquals("EOF", lexed.failure());
        assertEquals(5, lexed.tokens().size());
    }

    @Test
    @DisplayName(
            "Given: decimal literal '3.14' | When: lexing | Then: produces a single number"
                    + " literal with the full text")
    void lexer_whenNumberIsDecimal_thenSingleNumberLiteral() {
        // Given
        String source = "3.14;";

        // When
        Token number = lex(source).tokens().get(0);

        // Then
        assertEquals(TokenType.NUMBER_LITERAL, number.type());
        assertEquals("3.14", number.value());
        assertEquals(new Position(1, 4), number.endPosition());
    }

    @Test
    @DisplayName(
            "Given: strings with double and single quotes | When: lexing | Then: both are string"
                    + " literals keeping their quotes")
    void lexer_whenStringsUseBothQuoteStyles_thenBothAreStringLiterals() {
        // Given
        String source = "\"hola mundo\" 'chau'";

        // When
        List<Token> tokens = lex(source).tokens();

        // Then
        assertEquals(List.of(TokenType.STRING_LITERAL, TokenType.STRING_LITERAL), types(tokens));
        assertEquals("\"hola mundo\"", tokens.get(0).value());
        assertEquals("'chau'", tokens.get(1).value());
    }

    @Test
    @DisplayName(
            "Given: a string literal with a space inside | When: lexing | Then: its range covers"
                    + " both quotes")
    void lexer_whenStringHasSpaces_thenRangeCoversQuotes() {
        // Given
        String source = "x = \"a b\";";

        // When
        Token literal = lex(source).tokens().get(2);

        // Then
        assertEquals(new Position(1, 5), literal.startPosition());
        assertEquals(new Position(1, 9), literal.endPosition());
    }

    @Test
    @DisplayName(
            "Given: identifier 'letter' that starts like 'let' | When: lexing | Then: maximal"
                    + " munch yields one identifier")
    void lexer_whenIdentifierStartsWithKeyword_thenMaximalMunchYieldsIdentifier() {
        // Given
        String source = "letter";

        // When
        List<Token> tokens = lex(source).tokens();

        // Then
        assertEquals(List.of(TokenType.IDENTIFIER), types(tokens));
        assertEquals("letter", tokens.get(0).value());
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.1 keywords and literals | When: lexing | Then: each one gets its"
                    + " own token type")
    void lexer_whenSourceUsesV11Keywords_thenRecognizesThem() {
        // Given
        String source = "const b: boolean = true; if (b) {} else {}";

        // When
        List<TokenType> types = types(lex(source).tokens());

        // Then
        assertEquals(
                List.of(
                        TokenType.CONST,
                        TokenType.IDENTIFIER,
                        TokenType.COLON,
                        TokenType.BOOLEAN,
                        TokenType.EQUAL,
                        TokenType.BOOLEAN_LITERAL,
                        TokenType.SEMICOLON,
                        TokenType.IF,
                        TokenType.LPAREN,
                        TokenType.IDENTIFIER,
                        TokenType.RPAREN,
                        TokenType.LBRACE,
                        TokenType.RBRACE,
                        TokenType.ELSE,
                        TokenType.LBRACE,
                        TokenType.RBRACE),
                types);
    }

    @Test
    @DisplayName(
            "Given: an arithmetic expression without spaces | When: lexing | Then: each operator"
                    + " is a separate token")
    void lexer_whenOperatorsAreGlued_thenSplitsIntoOperatorTokens() {
        // Given
        String source = "1+2-3*4/5";

        // When
        List<TokenType> types = types(lex(source).tokens());

        // Then
        assertEquals(
                List.of(
                        TokenType.NUMBER_LITERAL,
                        TokenType.PLUS,
                        TokenType.NUMBER_LITERAL,
                        TokenType.MINUS,
                        TokenType.NUMBER_LITERAL,
                        TokenType.STAR,
                        TokenType.NUMBER_LITERAL,
                        TokenType.SLASH,
                        TokenType.NUMBER_LITERAL),
                types);
    }

    @Test
    @DisplayName(
            "Given: an unterminated string | When: lexing | Then: fails with a lexical error at"
                    + " the opening quote")
    void lexer_whenStringIsUnterminated_thenLexicalErrorAtOpeningQuote() {
        // Given
        String source = "x = \"abc";

        // When
        Lexed lexed = lex(source);

        // Then
        assertTrue(lexed.failure().startsWith("Lexical error"));
        assertTrue(lexed.failure().contains("line 1, column 5"));
        assertFalse(lexed.tokens().isEmpty());
    }
}
