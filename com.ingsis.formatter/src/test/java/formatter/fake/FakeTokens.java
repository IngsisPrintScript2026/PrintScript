/*
 * My Project
 */

package formatter.fake;

import position.Position;
import token.Token;
import token.TokenType;

/** Builds tokens with consistent start and (inclusive) end positions, as the lexer does. */
public final class FakeTokens {
    private FakeTokens() {}

    public static Token token(TokenType type, String value, int line, int column) {
        Position start = new Position(line, column);
        Position end = new Position(line, column + value.length() - 1);
        return new Token(type, value, start, end);
    }

    public static Token at(TokenType type, String value) {
        return token(type, value, 1, 1);
    }
}
