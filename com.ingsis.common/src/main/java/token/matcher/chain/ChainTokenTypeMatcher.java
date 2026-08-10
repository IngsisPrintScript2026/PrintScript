package token.matcher.chain;

import token.matcher.BooleanMatcher;
import token.matcher.IdentifierMatcher;
import token.matcher.LexemeMatcher;
import token.matcher.NumberMatcher;
import token.matcher.StringMatcher;
import token.matcher.TokenTypeMatcher;

public final class ChainTokenTypeMatcher {

    private ChainTokenTypeMatcher() {}

    public static TokenTypeMatcher defaultChain() {
        LexemeMatcher lexemes = new LexemeMatcher();
        lexemes.linkWith(new NumberMatcher())
               .linkWith(new BooleanMatcher())
               .linkWith(new StringMatcher())
               .linkWith(new IdentifierMatcher());
        return lexemes;
    }
}
