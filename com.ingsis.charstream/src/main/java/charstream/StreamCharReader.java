package charstream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

public class StreamCharReader implements CharReader {
    private final PushbackReader reader;

    public StreamCharReader(Reader reader) {
        Reader buffered = (reader instanceof BufferedReader) ? reader : new BufferedReader(reader, 8192);
        this.reader = (buffered instanceof PushbackReader p) ? p : new PushbackReader(buffered, 1024);
    }

    @Override
    public int readNextChar() throws IOException {
        return reader.read();
    }

    @Override
    public void unread(int c) throws IOException {
        if (c != -1) {
            reader.unread(c);
        }
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
