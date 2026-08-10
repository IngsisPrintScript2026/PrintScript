package token;

import position.Position;

public record Token(
        TokenType type,
        String value,
        Position startPosition,
        Position endPosition
) implements TokenInterface {

    public Token(TokenType type, String value, Position startPosition) {
        this(type, value, startPosition, null);
    }
}
