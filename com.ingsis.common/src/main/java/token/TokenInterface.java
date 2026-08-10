package token;

import position.Position;

public interface TokenInterface {
    TokenType type();
    Position startPosition();
    Position endPosition();
    String value();

    default Integer line() {
        return startPosition() != null ? startPosition().line() : -1;
    }

    default Integer column() {
        return startPosition() != null ? startPosition().column() : -1;
    }

    default boolean isNull() {
        return false;
    }
}
