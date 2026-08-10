package token.matcher;

import result.Result;
import token.TokenType;
import java.util.Map;

public class LexemeMatcher extends AbstractTokenTypeMatcher {
    private static final Map<String, TokenType> LEXEMES = Map.ofEntries(
            Map.entry("let", TokenType.LET),
            Map.entry("const", TokenType.CONST),
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("number", TokenType.NUMBER),
            Map.entry("string", TokenType.STRING),
            Map.entry("boolean", TokenType.BOOLEAN),
            Map.entry("println", TokenType.PRINTLN),
            Map.entry("+", TokenType.PLUS),
            Map.entry("-", TokenType.MINUS),
            Map.entry("*", TokenType.STAR),
            Map.entry("/", TokenType.SLASH),
            Map.entry("=", TokenType.EQUAL),
            Map.entry("(", TokenType.LPAREN),
            Map.entry(")", TokenType.RPAREN),
            Map.entry("{", TokenType.LBRACE),
            Map.entry("}", TokenType.RBRACE),
            Map.entry(":", TokenType.COLON),
            Map.entry(";", TokenType.SEMICOLON),
            Map.entry(",", TokenType.COMMA)
    );

    @Override
    protected Result<TokenType> doMatch(String input) {
        TokenType type = LEXEMES.get(input);
        if (type != null) {
            return Result.success(type);
        }
        return Result.failure("No es un lexema fijo");
    }
}
