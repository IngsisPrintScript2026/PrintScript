/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import syntactic.strategy.WithoutElseStrategy;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;

class WithoutElseStrategyUnitTest {

    @Test
    @DisplayName(
            "Given: WithoutElseStrategy | When: checking if stream has else token | Then: matches"
                    + " accurately")
    void withoutElseStrategy_whenCheckingStream_thenMatchesAccurately() {
        // Given: WithoutElseStrategy and streams
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
