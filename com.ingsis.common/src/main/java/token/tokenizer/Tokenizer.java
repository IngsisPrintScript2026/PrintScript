package token.tokenizer;

import metaChar.MetaCharStringBuilder;
import token.tokenize.TokenizeResult;

public interface Tokenizer {
    TokenizeResult tokenize(MetaCharStringBuilder sb);
}
