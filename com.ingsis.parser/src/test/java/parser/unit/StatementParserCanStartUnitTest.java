/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static parser.support.Sources.token;

import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import parser.fake.StubFailingParser;
import syntactic.parser.root.AssignParser;
import syntactic.parser.root.ConditionalParser;
import syntactic.parser.root.DeclarationParser;
import syntactic.parser.root.LineExpressionParser;
import syntactic.parser.root.StatementParser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;
import tokenstream.TokenStreamAdapter;
import tokenstream.rules.TokenMatchers;

class StatementParserCanStartUnitTest {

    private static final StubFailingParser<ExpressionNode> NO_EXPRESSION =
            new StubFailingParser<>("not used by canStart");
    private static final StubFailingParser<IdentifierNode> NO_IDENTIFIER =
            new StubFailingParser<>("not used by canStart");
    private static final StubFailingParser<Node> NO_STATEMENT =
            new StubFailingParser<>("not used by canStart");

    private static TokenStream streamOf(Token... tokens) {
        return new TokenStreamAdapter(List.of(tokens), 0);
    }

    private static DeclarationParser letOnlyDeclarationParser() {
        return new DeclarationParser(
                NO_IDENTIFIER,
                NO_EXPRESSION,
                TokenMatchers.isOneOf(TokenType.LET),
                TokenMatchers.isOneOf(TokenType.NUMBER, TokenType.STRING));
    }

    @Test
    @DisplayName(
            "Given: a declaration parser for 'let' | When: the stream starts with 'let' | Then:"
                    + " it can start the statement")
    void declarationParser_whenStreamStartsWithLet_thenCanStart() {
        // Given
        DeclarationParser parser = letOnlyDeclarationParser();

        // When
        boolean canStart = parser.canStart(streamOf(token(TokenType.LET, "let", 1)));

        // Then
        assertTrue(canStart);
    }

    @Test
    @DisplayName(
            "Given: a declaration parser for 'let' only (1.0) | When: the stream starts with"
                    + " 'const' | Then: it does not claim the statement")
    void declarationParser_whenKeywordNotInVersion_thenCannotStart() {
        // Given
        DeclarationParser parser = letOnlyDeclarationParser();

        // When
        boolean canStart = parser.canStart(streamOf(token(TokenType.CONST, "const", 1)));

        // Then
        assertFalse(canStart);
    }

    @Test
    @DisplayName(
            "Given: an assign parser | When: the stream is 'x =' | Then: it can start the"
                    + " statement")
    void assignParser_whenIdentifierFollowedByEqual_thenCanStart() {
        // Given
        AssignParser parser = new AssignParser(NO_IDENTIFIER, NO_EXPRESSION);

        // When
        boolean canStart =
                parser.canStart(
                        streamOf(
                                token(TokenType.IDENTIFIER, "x", 1),
                                token(TokenType.EQUAL, "=", 3)));

        // Then
        assertTrue(canStart);
    }

    @Test
    @DisplayName(
            "Given: an assign parser | When: the stream is 'println (' | Then: it does not claim"
                    + " the statement")
    void assignParser_whenIdentifierNotFollowedByEqual_thenCannotStart() {
        // Given
        AssignParser parser = new AssignParser(NO_IDENTIFIER, NO_EXPRESSION);

        // When
        boolean canStart =
                parser.canStart(
                        streamOf(
                                token(TokenType.IDENTIFIER, "println", 1),
                                token(TokenType.LPAREN, "(", 8)));

        // Then
        assertFalse(canStart);
    }

    @Test
    @DisplayName(
            "Given: an assign parser | When: the stream only has an identifier | Then: it does not"
                    + " claim the statement")
    void assignParser_whenOnlyIdentifier_thenCannotStart() {
        // Given
        AssignParser parser = new AssignParser(NO_IDENTIFIER, NO_EXPRESSION);

        // When
        boolean canStart = parser.canStart(streamOf(token(TokenType.IDENTIFIER, "x", 1)));

        // Then
        assertFalse(canStart);
    }

    @Test
    @DisplayName(
            "Given: a conditional parser | When: the stream starts with 'if' | Then: it can start"
                    + " the statement")
    void conditionalParser_whenStreamStartsWithIf_thenCanStart() {
        // Given
        ConditionalParser parser = new ConditionalParser(NO_EXPRESSION, NO_STATEMENT);

        // When
        boolean canStart = parser.canStart(streamOf(token(TokenType.IF, "if", 1)));

        // Then
        assertTrue(canStart);
    }

    @Test
    @DisplayName(
            "Given: a conditional parser | When: the stream starts with an identifier | Then: it"
                    + " does not claim the statement")
    void conditionalParser_whenStreamDoesNotStartWithIf_thenCannotStart() {
        // Given
        ConditionalParser parser = new ConditionalParser(NO_EXPRESSION, NO_STATEMENT);

        // When
        boolean canStart = parser.canStart(streamOf(token(TokenType.IDENTIFIER, "x", 1)));

        // Then
        assertFalse(canStart);
    }

    @Test
    @DisplayName(
            "Given: a line-expression parser | When: asked with an empty stream | Then: it always"
                    + " accepts (it is the fallback)")
    void lineExpressionParser_whenAnyStream_thenAlwaysCanStart() {
        // Given
        LineExpressionParser parser = new LineExpressionParser(NO_EXPRESSION);

        // When
        boolean canStart = parser.canStart(streamOf());

        // Then
        assertTrue(canStart);
    }

    @Test
    @DisplayName(
            "Given: an empty stream | When: checking peekMatches at offset 0 | Then: returns false"
                    + " instead of failing")
    void peekMatches_whenOffsetIsOutOfRange_thenReturnsFalse() {
        // Given
        TokenStream stream = streamOf();

        // When
        boolean matches = StatementParser.peekMatches(stream, 0, token -> true);

        // Then
        assertFalse(matches);
    }
}
