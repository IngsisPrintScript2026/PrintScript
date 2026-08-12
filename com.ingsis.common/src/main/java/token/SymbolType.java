package token;

import java.util.Optional;

public enum SymbolType {
    EQUAL(TokenType.EQUAL, "="),
    COLON(TokenType.COLON, ":"),
    SEMICOLON(TokenType.SEMICOLON, ";"),
    LPAREN(TokenType.LPAREN, "("),
    RPAREN(TokenType.RPAREN, ")"),
    LBRACE(TokenType.LBRACE, "{"),
    RBRACE(TokenType.RBRACE, "}"),
    COMMA(TokenType.COMMA, ",");

    private final TokenType tokenType;
    private final String symbol;

    SymbolType(TokenType tokenType, String symbol) {
        this.tokenType = tokenType;
        this.symbol = symbol;
    }

    public TokenType tokenType() { return tokenType; }
    public String symbol() { return symbol; }

    public static boolean isSymbol(Token token, SymbolType symbolType) {
        if (token == null || symbolType == null) {
            return false;
        }
        return token.type() == symbolType.tokenType() || symbolType.symbol().equals(token.value());
    }

    public static Optional<SymbolType> fromToken(Token token) {
        if (token == null) {
            return Optional.empty();
        }
        for (SymbolType s : values()) {
            if (s.tokenType() == token.type() || s.symbol().equals(token.value())) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public static Optional<SymbolType> fromTokenType(TokenType tokenType) {
        if (tokenType == null) {
            return Optional.empty();
        }
        for (SymbolType s : values()) {
            if (s.tokenType() == tokenType) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
