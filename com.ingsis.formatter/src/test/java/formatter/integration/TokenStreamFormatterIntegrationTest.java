/*
 * My Project
 */

package formatter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.FormatContext;
import formatter.TokenStreamFormatter;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;

class TokenStreamFormatterIntegrationTest {

    @Test
    @DisplayName(
            "What: TokenStreamFormatter | When: formatting unspaced let statement | Then: inserts"
                    + " spaces around colons and equals")
    void tokenStreamFormatter_whenFormattingSimpleStream_thenEmitsFormattedCode() {
        // What: code without spaces "let x:number=42;"
        String code = "let x:number=42;";
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        ByteArrayInputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        StringWriter writer = new StringWriter();

        // When: formatting stream
        Result<String> res = formatter.format(in, writer);

        // Then: output contains normalized spaces
        assertTrue(res.isCorrect());
        String out = ((CorrectResult<String>) res).value();
        assertEquals("let x: number = 42;", out.trim());
        assertEquals(out, writer.toString());
    }

    @Test
    @DisplayName(
            "What: TokenStreamFormatter | When: formatting if statement with indentation and line"
                    + " breaks | Then: formats blocks appropriately")
    void tokenStreamFormatter_whenFormattingIfWithIndentationAndPrintln_thenEmitsFormattedCode() {
        // What: if statement and custom FormatContext
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

        // When: formatting stream
        Result<String> res = formatter.format(in, writer);

        // Then: produces formatted non-empty output
        assertTrue(res.isCorrect());
        assertFalse(((CorrectResult<String>) res).value().isEmpty());
    }

    @Test
    @DisplayName(
            "What: TokenStreamFormatter | When: formatting empty input stream | Then: returns empty"
                    + " string")
    void tokenStreamFormatter_whenStreamIsEmpty_thenReturnsEmptyString() {
        // What: TokenStreamFormatter and empty stream
        TokenStreamFormatter formatter = new TokenStreamFormatter();
        ByteArrayInputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        // When: formatting empty stream
        Result<String> res = formatter.format(in, new StringWriter());

        // Then: returns empty string
        assertTrue(res.isCorrect());
        assertEquals("", ((CorrectResult<String>) res).value());
    }

    @Test
    @DisplayName(
            "What: TokenStreamFormatter | When: invoking unsupported AST methods | Then: returns"
                    + " failure")
    void tokenStreamFormatter_whenInvokingUnsupportedASTMethods_thenReturnsFailure() {
        // What: TokenStreamFormatter
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        // When & Then: AST format methods return failure
        assertFalse(formatter.format(new ProgramNode(java.util.List.of(), 1, 1)).isCorrect());
        assertFalse(formatter.formatNode(new IdentifierNode("x", 1, 1)).isCorrect());
    }

    @Test
    @DisplayName(
            "What: TokenStreamFormatter with SpaceAfterCommaRule | When: formatting commas in calls"
                    + " | Then: enforces space after comma according to configuration")
    void tokenStreamFormatter_whenFormattingCommasInCalls_thenFormatsSpaceAfterComma() {
        // What: code with comma without space, and FormatContext with spaceAfterComma
        String code = "println(1,2);";
        FormatContext ctxEnabled =
                new FormatContext(
                        0, 4, false, true, true, true, false, 1, false, true, false, true);
        FormatContext ctxDisabled =
                new FormatContext(
                        0, 4, false, true, true, true, false, 1, false, true, false, false);
        TokenStreamFormatter formatterEnabled = new TokenStreamFormatter(ctxEnabled);
        TokenStreamFormatter formatterDisabled = new TokenStreamFormatter(ctxDisabled);

        ByteArrayInputStream in1 = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream in2 = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

        // When: formatting stream with enabled and disabled rule
        Result<String> resEnabled = formatterEnabled.format(in1, new StringWriter());
        Result<String> resDisabled = formatterDisabled.format(in2, new StringWriter());

        // Then: verify formatted output
        assertTrue(resEnabled.isCorrect());
        assertEquals("println(1, 2);", ((CorrectResult<String>) resEnabled).value().trim());
        assertTrue(resDisabled.isCorrect());
        assertEquals("println(1,2);", ((CorrectResult<String>) resDisabled).value().trim());
    }
}
