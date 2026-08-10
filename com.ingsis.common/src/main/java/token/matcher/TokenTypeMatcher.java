package token.matcher;

import result.Result;
import token.TokenType;

public interface TokenTypeMatcher {
    Result<TokenType> match(String input);
}
