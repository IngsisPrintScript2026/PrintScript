/*
 * My Project
 */

package sca;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import iterator.SafeIterator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lexer.Lexer;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

public class TokenStreamSca {
    private final ScaContext defaultContext;

    public TokenStreamSca(ScaContext defaultContext) {
        this.defaultContext = defaultContext != null ? defaultContext : new ScaContext();
    }

    public TokenStreamSca() {
        this(new ScaContext());
    }

    public Result<List<String>> analyze(InputStream in) {
        return analyze(in, defaultContext);
    }

    public Result<List<String>> analyze(InputStream in, ScaContext context) {
        try {
            StreamCharReader reader =
                    new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            CharStream charStream = new CharStream(reader);
            SafeIterator<Token> lexer = new Lexer(charStream);
            return analyze(lexer, context);
        } catch (Exception e) {
            return Result.failure("Analysis error: " + e.getMessage());
        }
    }

    public Result<List<String>> analyze(SafeIterator<Token> iterator) {
        return analyze(iterator, defaultContext);
    }

    public Result<List<String>> analyze(SafeIterator<Token> iterator, ScaContext context) {
        if (iterator == null) {
            return Result.failure("Iterator cannot be null");
        }
        List<Token> tokens = new ArrayList<>();
        SafeIterator<Token> current = iterator;
        while (true) {
            Result<IterationStep<Token>> res = current.next();
            if (!res.isCorrect()) {
                break;
            }
            IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) res).value();
            tokens.add(step.value());
            current = step.nextStream();
        }
        return analyzeTokens(tokens, context != null ? context : defaultContext);
    }

    public Result<List<String>> analyzeTokens(List<Token> tokens, ScaContext ctx) {
        List<String> violations = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            checkDeclaration(tokens, i, ctx, violations);
            checkFunctionCall(tokens, i, ctx, violations);
        }
        return Result.success(violations);
    }

    private void checkDeclaration(
            List<Token> tokens, int i, ScaContext ctx, List<String> violations) {
        Token token = tokens.get(i);
        if (token.type() == TokenType.LET || token.type() == TokenType.CONST) {
            if (i + 1 < tokens.size() && tokens.get(i + 1).type() == TokenType.IDENTIFIER) {
                checkNamingConvention(tokens.get(i + 1), ctx.identifierFormat(), violations);
            }
        }
    }

    private void checkFunctionCall(
            List<Token> tokens, int i, ScaContext ctx, List<String> violations) {
        Token token = tokens.get(i);
        if (isCall(token, "println") && ctx.mandatoryLiteralOrIdentifierInPrintln()) {
            validateCallArgs(tokens, i, "println", violations);
        } else if (isCall(token, "readInput") && ctx.mandatoryLiteralOrIdentifierInReadInput()) {
            validateCallArgs(tokens, i, "readInput", violations);
        }
    }

    private boolean isCall(Token token, String name) {
        return token.type() == TokenType.PRINTLN
                || (token.type() == TokenType.IDENTIFIER && name.equalsIgnoreCase(token.value()));
    }

    private void validateCallArgs(
            List<Token> tokens, int callIndex, String name, List<String> violations) {
        if (callIndex + 1 >= tokens.size()
                || tokens.get(callIndex + 1).type() != TokenType.LPAREN) {
            return;
        }
        int idx = callIndex + 2;
        int argIndex = 0;
        boolean hasOperator = false;
        Token errorToken = null;

        while (idx < tokens.size() && tokens.get(idx).type() != TokenType.RPAREN) {
            Token t = tokens.get(idx);
            if (t.type() == TokenType.COMMA) {
                recordViolationIfAny(hasOperator, name, argIndex, errorToken, violations);
                argIndex++;
                hasOperator = false;
                errorToken = null;
            } else if (isOperatorToken(t)) {
                hasOperator = true;
                if (errorToken == null) errorToken = t;
            }
            idx++;
        }
        recordViolationIfAny(hasOperator, name, argIndex, errorToken, violations);
    }

    private boolean isOperatorToken(Token t) {
        TokenType type = t.type();
        return type == TokenType.PLUS
                || type == TokenType.MINUS
                || type == TokenType.STAR
                || type == TokenType.SLASH;
    }

    private void recordViolationIfAny(
            boolean hasOp, String name, int index, Token token, List<String> violations) {
        if (hasOp && token != null) {
            violations.add(
                    String.format(
                            "Function '%s' argument at index %d must be a literal or variable,"
                                    + " found expression at line %d, column %d",
                            name, index, token.line(), token.column()));
        }
    }

    private void checkNamingConvention(Token idToken, String format, List<String> violations) {
        if (format == null || format.isBlank()) {
            return;
        }
        if (!matchesConvention(idToken.value(), format.trim().toLowerCase(Locale.ROOT))) {
            violations.add(
                    String.format(
                            "Identifier '%s' does not respect %s naming convention at line %d,"
                                    + " column %d",
                            idToken.value(), format, idToken.line(), idToken.column()));
        }
    }

    private boolean matchesConvention(String name, String format) {
        return switch (format) {
            case "camel case", "camelcase" -> name.matches("^[a-z]+(?:[A-Z][a-z0-9]*)*$");
            case "snake case", "snake_case" -> name.matches("^[a-z]+(?:_[a-z0-9]+)*$");
            default -> true;
        };
    }
}
