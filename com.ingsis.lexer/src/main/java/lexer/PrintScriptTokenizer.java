package lexer;

import metaChar.MetaCharStringBuilder;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import token.matcher.TokenTypeMatcher;
import token.matcher.chain.ChainTokenTypeMatcher;
import token.tokenize.TokenizeResult;
import token.tokenizer.Tokenizer;

public class PrintScriptTokenizer implements Tokenizer {
    private final TokenTypeMatcher tokenMatcher;
    private TokenizeResult tokenizeResult;

    public PrintScriptTokenizer(TokenTypeMatcher tokenMatcher) {
        this.tokenMatcher = tokenMatcher;
    }

    public PrintScriptTokenizer() {
        this(ChainTokenTypeMatcher.defaultChain());
    }

    @Override
    public TokenizeResult tokenize(MetaCharStringBuilder sb) {
        String text = sb.buildString();
        if (text.isEmpty()) {
            return new TokenizeResult.Invalid("Texto vacío");
        }
        return evaluateMatch(tokenMatcher.match(text), text, sb);
    }

    private TokenizeResult evaluateMatch(
            Result<TokenType> matchResult,
            String text,
            MetaCharStringBuilder sb) {

        return switch (matchResult) {
            case CorrectResult<TokenType>(TokenType value) -> {
                Token token = new Token(value, text, sb.getStartPosition());
                yield new TokenizeResult.Complete(token);
            }
            case IncorrectResult<TokenType> failure -> evaluateFailure(text, failure.error());
        };
    }

    private TokenizeResult evaluateFailure(String text, String error) {
        if (isPrefix(text)) {
            return new TokenizeResult.Prefix();
        }
        return new TokenizeResult.Invalid(error);
    }

    private boolean isPrefix(String text) {
        if (text.startsWith("\"") || text.startsWith("'")) {
            return isUnfinishedString(text);
        }
        return text.matches("^\\d+\\.$");
    }

    private boolean isUnfinishedString(String text) {
        char quote = text.charAt(0);
        String quoteStr = String.valueOf(quote);
        return text.length() == 1 || !text.endsWith(quoteStr) || text.endsWith("\\" + quoteStr);
    }
}
