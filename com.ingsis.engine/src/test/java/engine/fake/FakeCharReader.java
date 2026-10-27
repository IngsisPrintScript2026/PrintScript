/*
 * My Project
 */

package engine.fake;

import charstream.CharReader;
import java.util.ArrayDeque;
import java.util.Deque;

/** In-memory CharReader over a string that supports unread, like the real pushback reader. */
public final class FakeCharReader implements CharReader {
    private final Deque<Integer> pending = new ArrayDeque<>();
    private boolean closed;

    public FakeCharReader(String content) {
        content.chars().forEach(pending::addLast);
    }

    @Override
    public int readNextChar() {
        return pending.isEmpty() ? -1 : pending.removeFirst();
    }

    @Override
    public void unread(int c) {
        pending.addFirst(c);
    }

    @Override
    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }
}
