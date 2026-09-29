/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.keyword.IfKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.parser.ParserFactory;
import syntactic.parser.literal.BooleanLiteralParser;
import syntactic.parser.root.ConditionalParser;
import syntactic.strategy.WithElseStrategy;
import syntactic.strategy.WithoutElseStrategy;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;
import version.Version;

class ConditionalParserIntegrationTest {

    private final Position pos = new Position(1, 1);

    private static <T extends ExpressionNode> Parser<ExpressionNode> asExprParser(
            Parser<T> parser) {
        return stream -> {
            Result<IterationStep<T>> res = parser.parse(stream);
            if (res.isCorrect()) {
                IterationStep<T> step = ((CorrectResult<IterationStep<T>>) res).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }
            return Result.failure(((IncorrectResult<IterationStep<T>>) res).error());
        };
    }

    @Test
    @DisplayName(
            "What: ConditionalParser | When: parsing if with and without else clause | Then:"
                    + " creates IfKeywordNode with matching bodies")
    void
            conditionalParser_whenParsingIfWithAndWithoutElse_thenCreatesIfKeywordNodeWithMatchingBodies() {
        // What: ConditionalParser configured for V1.1
        Parser<Node> stmtParser = ParserFactory.createParser(Version.V_1_1);
        Parser<ExpressionNode> boolParser = asExprParser(new BooleanLiteralParser());
        ConditionalParser parserWithElse =
                new ConditionalParser(
                        boolParser,
                        stmtParser,
                        List.of(new WithElseStrategy(), new WithoutElseStrategy()));
        ConditionalParser parserWithoutElse = new ConditionalParser(boolParser, stmtParser);

        // Tokens for: if (true) { println("yes"); } else { println("no"); }
        List<Token> tokensWithElse =
                List.of(
                        new Token(TokenType.IF, "if", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.BOOLEAN_LITERAL, "true", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.LBRACE, "{", pos, pos),
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"yes\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.RBRACE, "}", pos, pos),
                        new Token(TokenType.ELSE, "else", pos, pos),
                        new Token(TokenType.LBRACE, "{", pos, pos),
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"no\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.RBRACE, "}", pos, pos));

        // When: parsing if statement with else
        Result<IterationStep<IfKeywordNode>> resWithElse =
                parserWithElse.parse(new TokenStreamAdapter(tokensWithElse, 0));

        // Then: both thenBody and elseBody have 1 statement
        assertTrue(resWithElse.isCorrect());
        IfKeywordNode node =
                ((CorrectResult<IterationStep<IfKeywordNode>>) resWithElse).value().value();
        assertEquals(1, node.thenBody().size());
        assertEquals(1, node.elseBody().size());

        // Tokens for: if (true) { println("yes"); }
        List<Token> tokensWithoutElse =
                List.of(
                        new Token(TokenType.IF, "if", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.BOOLEAN_LITERAL, "true", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.LBRACE, "{", pos, pos),
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"yes\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.RBRACE, "}", pos, pos));

        // When: parsing if statement without else
        Result<IterationStep<IfKeywordNode>> resWithoutElse =
                parserWithoutElse.parse(new TokenStreamAdapter(tokensWithoutElse, 0));

        // Then: succeeds with thenBody and empty elseBody
        assertTrue(resWithoutElse.isCorrect());

        // When & Then: parserWithElse also parses statement when else is absent
        Result<IterationStep<IfKeywordNode>> resWithElseAbsent =
                parserWithElse.parse(new TokenStreamAdapter(tokensWithoutElse, 0));
        assertTrue(resWithElseAbsent.isCorrect());
    }
}
