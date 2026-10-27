/*
 * My Project
 */

package parser.support;

import charstream.CharStream;
import charstream.StreamCharReader;
import java.io.StringReader;
import lexer.Lexer;
import position.Position;
import token.Token;
import token.TokenType;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;

/** Builders shared by parser tests: real token streams from source and hand-made tokens. */
public final class Sources {
    private Sources() {}

    /** Wires the real CharStream + Lexer + LazyTokenStream over a source string. */
    public static TokenStream tokens(String source) {
        return new LazyTokenStream(
                new Lexer(new CharStream(new StreamCharReader(new StringReader(source)))));
    }

    /** Token on line 1 whose end column is derived from the length of its text. */
    public static Token token(TokenType type, String value, int column) {
        return new Token(
                type, value, new Position(1, column), new Position(1, column + value.length() - 1));
    }
}
