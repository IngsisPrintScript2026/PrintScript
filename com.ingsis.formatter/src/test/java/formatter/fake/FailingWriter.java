/*
 * My Project
 */

package formatter.fake;

import java.io.IOException;
import java.io.Writer;

/** Writer whose every operation fails, to simulate a broken output destination. */
public final class FailingWriter extends Writer {
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        throw new IOException("disk full");
    }

    @Override
    public void flush() throws IOException {
        throw new IOException("disk full");
    }

    @Override
    public void close() {
        // nothing to release
    }
}
