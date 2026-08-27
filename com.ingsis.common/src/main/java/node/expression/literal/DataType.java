/*
 * My Project
 */

package node.expression.literal;

import java.util.Optional;
import token.Token;
import token.TokenType;

public enum DataType {
    BOOLEAN,
    STRING,
    NUMBER;

    public static boolean exists(Token token) {
        if (token == null) {
            return false;
        }
        return fromTokenType(token.type()).isPresent() || fromKeyword(token.value()).isPresent();
    }

    public static boolean exists(TokenType tokenType) {
        return fromTokenType(tokenType).isPresent();
    }

    public static Optional<DataType> fromTokenType(TokenType tokenType) {
        if (tokenType == null) {
            return Optional.empty();
        }
        return switch (tokenType) {
            case BOOLEAN -> Optional.of(BOOLEAN);
            case STRING -> Optional.of(STRING);
            case NUMBER -> Optional.of(NUMBER);
            default -> Optional.empty();
        };
    }

    public static Optional<DataType> fromKeyword(String keyword) {
        if (keyword == null) {
            return Optional.empty();
        }
        for (DataType type : values()) {
            if (type.name().equalsIgnoreCase(keyword)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
