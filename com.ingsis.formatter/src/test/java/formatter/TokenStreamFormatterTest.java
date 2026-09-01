/*
 * My Project
 */

package formatter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;

class TokenStreamFormatterTest {

    @Test
    void testFormatSimpleStream() {
        String code = "let x:number=42;";
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        Result<String> res = formatter.format(in, writer);
        assertTrue(res.isCorrect());
        String out = ((CorrectResult<String>) res).value();
        assertEquals("let x: number = 42;", out.trim());
        assertEquals(out, writer.toString());
    }

    @Test
    void testFormatWithIfAndIndentationAndPrintln() {
        String code = "if(true){println(\"hi\");}else{x=10;}";
        FormatContext ctx =
                new FormatContext(
                        0,
                        4,
                        false, // spaceBeforeColon
                        true, // spaceAfterColon
                        true, // spaceAroundEquals
                        true, // spaceAroundOperators
                        true, // lineBreakAfterStatement
                        2, // lineBreaksAfterPrintln
                        false, // singleSpaceSeparation
                        false, // ifBraceSameLine
                        true // ifBraceBelowLine
                        );
        TokenStreamFormatter formatter = new TokenStreamFormatter(ctx);

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();
        Result<String> res = formatter.format(in, writer);
        assertTrue(res.isCorrect());
        assertFalse(((CorrectResult<String>) res).value().isEmpty());
    }

    @Test
    void testEmptyStream() {
        TokenStreamFormatter formatter = new TokenStreamFormatter();
        ByteArrayInputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        Result<String> res = formatter.format(in, new StringWriter());
        assertTrue(res.isCorrect());
        assertEquals("", ((CorrectResult<String>) res).value());
    }

    @Test
    void testUnsupportedASTMethods() {
        TokenStreamFormatter formatter = new TokenStreamFormatter();
        assertFalse(formatter.format(new ProgramNode(java.util.List.of(), 1, 1)).isCorrect());
        assertFalse(formatter.formatNode(new IdentifierNode("x", 1, 1)).isCorrect());
    }
}
