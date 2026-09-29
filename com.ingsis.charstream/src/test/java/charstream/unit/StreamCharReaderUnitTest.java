/*
 * My Project
 */

package charstream.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import charstream.StreamCharReader;
import java.io.BufferedReader;
import java.io.PushbackReader;
import java.io.StringReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StreamCharReaderUnitTest {

    @Test
    @DisplayName(
            "What: StreamCharReader | When: reading and unreading characters | Then: characters are"
                    + " returned accurately")
    void streamCharReader_whenReadingAndUnreadingChars_thenReturnsExpectedCharacters()
            throws Exception {
        // What: StreamCharReader initialized with StringReader "ab"
        StringReader stringReader = new StringReader("ab");
        StreamCharReader charReader = new StreamCharReader(stringReader);

        // When: reading 'a', unreading 'a', then reading again
        int firstRead = charReader.readNextChar();
        charReader.unread('a');
        int secondRead = charReader.readNextChar();
        int thirdRead = charReader.readNextChar();
        int eofRead = charReader.readNextChar();
        charReader.unread(-1);

        // Then: verify character sequences and EOF
        assertEquals('a', firstRead);
        assertEquals('a', secondRead);
        assertEquals('b', thirdRead);
        assertEquals(-1, eofRead);

        charReader.close();
    }

    @Test
    @DisplayName(
            "What: StreamCharReader | When: wrapping BufferedReader and PushbackReader | Then:"
                    + " reads chars correctly")
    void streamCharReader_whenWrappingDifferentReaders_thenReadsCharsCorrectly() throws Exception {
        // What: BufferedReader and PushbackReader sources
        BufferedReader br = new BufferedReader(new StringReader("x"));
        PushbackReader pr = new PushbackReader(new StringReader("y"));

        // When: reading from StreamCharReader wrapping both
        int charFromBr;
        try (StreamCharReader r2 = new StreamCharReader(br)) {
            charFromBr = r2.readNextChar();
        }

        int charFromPr;
        try (StreamCharReader r3 = new StreamCharReader(pr)) {
            charFromPr = r3.readNextChar();
        }

        // Then: both characters match expected inputs
        assertEquals('x', charFromBr);
        assertEquals('y', charFromPr);
    }
}
