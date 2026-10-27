/*
 * My Project
 */

package progress;

import charstream.CharReader;
import java.io.IOException;

/**
 * Decorator that counts the characters actually consumed by the lexer. Because tokens are pulled
 * lazily by the parser, this count follows the parsing progress, not just the file reads.
 */
public final class ProgressCharReader implements CharReader {
    private final CharReader delegate;
    private final ProgressListener listener;
    private long charactersRead;

    public ProgressCharReader(CharReader delegate, ProgressListener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override
    public int readNextChar() throws IOException {
        int c = delegate.readNextChar();
        if (c != -1) {
            charactersRead++;
            listener.onCharactersRead(charactersRead);
        }
        return c;
    }

    @Override
    public void unread(int c) throws IOException {
        delegate.unread(c);
        if (c != -1) {
            charactersRead--;
        }
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
