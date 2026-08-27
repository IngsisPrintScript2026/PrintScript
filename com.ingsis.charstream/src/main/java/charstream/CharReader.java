/*
 * My Project
 */

package charstream;

import java.io.IOException;

public interface CharReader extends AutoCloseable {
    int readNextChar() throws IOException;

    default void unread(int c) throws IOException {}
}
