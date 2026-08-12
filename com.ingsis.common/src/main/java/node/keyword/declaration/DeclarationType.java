package node.keyword.declaration;

import token.Token;
import token.TokenType;

import java.util.Optional;

public enum DeclarationType {
    LET("let", true),
    CONST("const", false);

    private final String keyword;
    private final boolean isMutable;

    DeclarationType(String keyword, boolean isMutable) {
        this.keyword = keyword;
        this.isMutable = isMutable;
    }

    public String keyword() { return keyword; }
    public boolean isMutable() { return isMutable; }

    public static boolean exists(Token token) {
        if (token == null) {
            return false;
        }
        return fromToken(token).isPresent();
    }

    public static boolean exists(TokenType tokenType) {
        return fromTokenType(tokenType).isPresent();
    }

    public static Optional<DeclarationType> fromToken(Token token) {
        if (token == null) {
            return Optional.empty();
        }
        return fromTokenType(token.type())
                .or(() -> fromKeyword(token.value()));
    }

    public static Optional<DeclarationType> fromTokenType(TokenType tokenType) {
        if (tokenType == null) {
            return Optional.empty();
        }
        return switch (tokenType) {
            case LET -> Optional.of(LET);
            case CONST -> Optional.of(CONST);
            default -> Optional.empty();
        };
    }

    public static Optional<DeclarationType> fromKeyword(String keyword) {
        if (keyword == null) {
            return Optional.empty();
        }
        for (DeclarationType type : values()) {
            if (type.keyword.equalsIgnoreCase(keyword)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
