/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.parser.ParserFactory;
import syntactic.parser.literal.BooleanLiteralParser;
import syntactic.parser.literal.IdentifierParser;
import syntactic.parser.literal.NumberLiteralParser;
import syntactic.parser.literal.StringLiteralParser;
import syntactic.parser.root.FunctionParser;
import syntactic.strategy.WithoutElseStrategy;
import syntactic.version.Version10Strategy;
import syntactic.version.Version11Strategy;
import syntactic.version.VersionStrategyRegistry;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;
import version.Version;

class VersionStrategyUnitTest {

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
            "What: Version strategies and factory | When: retrieving and initializing strategies |"
                    + " Then: supplies version configurations")
    void versionStrategiesAndFactory_whenQueried_thenSuppliesCorrectConfigurations() {
        // What: VersionStrategyRegistry
        VersionStrategyRegistry registry = new VersionStrategyRegistry();

        // When & Then: registry resolves both 1.0 and 1.1
        assertNotNull(registry.getStrategy(Version.V_1_0));
        assertNotNull(registry.getStrategy(Version.V_1_1));

        // When & Then: ParserFactory creates parser instances
        Parser<Node> parser10 = ParserFactory.createParser(Version.V_1_0);
        assertNotNull(parser10);
        Parser<Node> parser11 = ParserFactory.createParser(Version.V_1_1);
        assertNotNull(parser11);

        // When & Then: Version 1.0 strategy checks
        Version10Strategy v10 = new Version10Strategy();
        assertEquals(Version.V_1_0, v10.version());
        assertNotNull(v10.declarationKeywords());
        assertNotNull(v10.supportedDataTypes());
        assertNotNull(
                v10.primaryParsers(
                        new NumberLiteralParser(),
                        new StringLiteralParser(),
                        new BooleanLiteralParser(),
                        new FunctionParser(
                                new IdentifierParser(), asExprParser(new NumberLiteralParser())),
                        new IdentifierParser()));

        // When & Then: Version 1.1 strategy checks
        Version11Strategy v11 = new Version11Strategy();
        assertEquals(Version.V_1_1, v11.version());
        assertNotNull(v11.declarationKeywords());
        assertNotNull(v11.supportedDataTypes());
        assertNotNull(
                v11.primaryParsers(
                        new NumberLiteralParser(),
                        new StringLiteralParser(),
                        new BooleanLiteralParser(),
                        new FunctionParser(
                                new IdentifierParser(), asExprParser(new NumberLiteralParser())),
                        new IdentifierParser()));
    }

    @Test
    @DisplayName(
            "What: WithoutElseStrategy | When: checking if stream has else token | Then: matches"
                    + " accurately")
    void withoutElseStrategy_whenCheckingStream_thenMatchesAccurately() {
        // What: WithoutElseStrategy and streams
        WithoutElseStrategy withoutElse = new WithoutElseStrategy();
        Position pos = new Position(1, 1);

        // When: stream is empty or has non-else token
        boolean emptyMatch = withoutElse.matches(new TokenStreamAdapter(List.of(), 0));
        boolean nonElseMatch =
                withoutElse.matches(
                        new TokenStreamAdapter(
                                List.of(new Token(TokenType.LET, "let", pos, pos)), 0));

        // Then: matches true
        assertTrue(emptyMatch);
        assertTrue(nonElseMatch);

        // When: stream starts with ELSE token
        boolean elseMatch =
                withoutElse.matches(
                        new TokenStreamAdapter(
                                List.of(new Token(TokenType.ELSE, "else", pos, pos)), 0));

        // Then: matches false
        assertFalse(elseMatch);
    }
}
