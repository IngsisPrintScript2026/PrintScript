package token.tokenize;

import token.Token;

public sealed interface TokenizeResult permits TokenizeResult.Complete, TokenizeResult.Prefix, TokenizeResult.Invalid {
    record Complete(Token token) implements TokenizeResult {}
    record Prefix() implements TokenizeResult {}
    record Invalid(String reason) implements TokenizeResult {}
}
