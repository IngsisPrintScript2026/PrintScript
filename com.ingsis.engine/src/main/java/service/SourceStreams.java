/*
 * My Project
 */

package service;

import charstream.CharReader;
import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.SafeIterator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lexer.Lexer;
import progress.ProgressCharReader;
import progress.ProgressListener;
import token.Token;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;

/** Builds the lazy char → token pipeline over a source, reporting progress as it is consumed. */
final class SourceStreams {
    private SourceStreams() {}

    static SafeIterator<Token> lexer(InputStream in, ProgressListener progress) {
        CharReader reader = new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        if (progress != ProgressListener.NONE) {
            reader = new ProgressCharReader(reader, progress);
        }
        return new Lexer(new CharStream(reader));
    }

    static TokenStream tokens(InputStream in, ProgressListener progress) {
        return new LazyTokenStream(lexer(in, progress));
    }
}
