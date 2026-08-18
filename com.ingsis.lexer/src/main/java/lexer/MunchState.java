package lexer;

import iterator.SafeIterator;
import metaChar.MetaCharacter;
import token.Token;

import java.util.ArrayList;
import java.util.List;

public class MunchState {
    public Token lastValidToken;
    public SafeIterator<MetaCharacter> nextStreamAfterToken;
    public final List<MetaCharacter> lookaheadBuffer;

    public MunchState() {
        this(null, null, new ArrayList<>());
    }

    public MunchState(
            Token lastValidToken,
            SafeIterator<MetaCharacter> nextStreamAfterToken,
            List<MetaCharacter> lookaheadBuffer) {
        this.lastValidToken = lastValidToken;
        this.nextStreamAfterToken = nextStreamAfterToken;
        this.lookaheadBuffer = lookaheadBuffer;
    }
}