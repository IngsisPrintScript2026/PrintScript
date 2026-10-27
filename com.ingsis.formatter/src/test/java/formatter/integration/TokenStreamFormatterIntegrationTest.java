/*
 * My Project
 */

package formatter.integration;

import static formatter.support.FormatterFixtures.format;
import static formatter.support.FormatterFixtures.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.TokenStreamFormatter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;

/** Real source → lexer → TokenStreamFormatter, configured from a YAML string. */
class TokenStreamFormatterIntegrationTest {
    private static final String NO_OPTIONS = "{}";
    private static final String IF_ELSE = "if(true){println(\"a\");}else{println(\"b\");}";

    @Test
    @DisplayName(
            "Given: the default formatter | When: formatting a glued declaration | Then: spaces"
                    + " after ':' and around '=' and ends with a newline")
    void formatter_whenDefaultContext_thenFormatsDeclaration() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();

        // When
        Result<String> result = formatter.format(stream("let x:number=42;"));

        // Then
        assertEquals("let x: number = 42;", ((CorrectResult<String>) result).value());
    }

    @Test
    @DisplayName(
            "Given: the default formatter and a writer | When: formatting a declaration | Then: the"
                    + " formatted text is written to the writer")
    void formatter_whenFormattingToWriter_thenWriterHasFormattedText() {
        // Given
        TokenStreamFormatter formatter = new TokenStreamFormatter();
        StringWriter writer = new StringWriter();

        // When
        Result<String> result = formatter.format(stream("let x:number=42;"), writer);

        // Then
        assertTrue(result.isCorrect());
        assertEquals("let x: number = 42;", writer.toString());
    }

    @Test
    @DisplayName(
            "Given: space before colon on and after colon off | When: formatting a declaration |"
                    + " Then: writes 'x :number'")
    void formatter_whenSpaceBeforeColonOnly_thenSpacesBeforeColon() {
        // Given
        String yaml =
                "enforce-spacing-before-colon-in-declaration: true\n"
                        + "enforce-spacing-after-colon-in-declaration: false";

        // When
        String output = format(yaml, "let x: number = 1;");

        // Then
        assertEquals("let x :number = 1;", output);
    }

    @Test
    @DisplayName(
            "Given: no spaces around the colon | When: formatting a declaration with spaced colon |"
                    + " Then: glues the colon")
    void formatter_whenColonSpacingDisabled_thenGluesColon() {
        // Given
        String yaml =
                "enforce-spacing-before-colon-in-declaration: false\n"
                        + "enforce-spacing-after-colon-in-declaration: false";

        // When
        String output = format(yaml, "let x : number = 1;");

        // Then
        assertEquals("let x:number = 1;", output);
    }

    @Test
    @DisplayName(
            "Given: spacing around equals enabled and colon unset | When: formatting a glued"
                    + " declaration | Then: spaces only the '='")
    void formatter_whenEqualsSpacingEnabled_thenSpacesOnlyEquals() {
        // Given
        String yaml = "enforce-spacing-around-equals: true";

        // When
        String output = format(yaml, "let x:number=1;");

        // Then
        assertEquals("let x:number = 1;", output);
    }

    @Test
    @DisplayName(
            "Given: spacing around equals disabled | When: formatting a spaced declaration | Then:"
                    + " glues the '='")
    void formatter_whenEqualsSpacingDisabled_thenGluesEquals() {
        // Given
        String yaml = "enforce-spacing-around-equals: false";

        // When
        String output = format(yaml, "let x: number = 1;");

        // Then
        assertEquals("let x: number=1;", output);
    }

    @Test
    @DisplayName(
            "Given: a config that tries to disable operator spaces | When: formatting glued"
                    + " arithmetic | Then: operators are always surrounded by one space")
    void formatter_whenOperatorsGlued_thenAlwaysSpacesThem() {
        // Given
        String yaml = "mandatory-space-surrounding-operations: false";

        // When
        String output = format(yaml, "let r:number=1*2+3/4-5;");

        // Then
        assertEquals("let r:number=1 * 2 + 3 / 4 - 5;", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: tokens are separated by runs of spaces | Then: collapses"
                    + " them to a single space")
    void formatter_whenSeveralSpaces_thenCollapsesToOne() {
        // Given
        String source = "let    x:    number   =   1;";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("let x: number = 1;", output);
    }

    @Test
    @DisplayName("Given: no options | When: there are spaces before ';' | Then: removes them")
    void formatter_whenSpacesBeforeSemicolon_thenRemovesThem() {
        // Given
        String source = "println(1)   ;";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("println(1);", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: two statements share a line | Then: each statement ends its"
                    + " line")
    void formatter_whenStatementsShareLine_thenBreaksAfterSemicolon() {
        // Given
        String source = "let a: number = 1;let b: number = 2;";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("let a: number = 1;\nlet b: number = 2;", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: statements are separated by an empty line | Then: keeps the"
                    + " empty line")
    void formatter_whenEmptyLineBetweenStatements_thenKeepsIt() {
        // Given
        String source = "let a: number = 1;\n\nlet b: number = 2;";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("let a: number = 1;\n\nlet b: number = 2;", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: formatting a single-quoted string | Then: keeps the quotes"
                    + " as written")
    void formatter_whenSingleQuotedString_thenKeepsIt() {
        // Given
        String source = "let a: string = 'hi';";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("let a: string = 'hi';", output);
    }

    @Test
    @DisplayName(
            "Given: space after comma enabled | When: formatting glued arguments | Then: one space"
                    + " after each comma")
    void formatter_whenSpaceAfterCommaEnabled_thenSpacesArguments() {
        // Given
        String yaml = "space-after-comma: true";

        // When
        String output = format(yaml, "println(1,2);");

        // Then
        assertEquals("println(1, 2);", output);
    }

    @Test
    @DisplayName(
            "Given: space after comma disabled | When: formatting spaced arguments | Then: glues"
                    + " them")
    void formatter_whenSpaceAfterCommaDisabled_thenGluesArguments() {
        // Given
        String yaml = "space-after-comma: false";

        // When
        String output = format(yaml, "println(1,   2);");

        // Then
        assertEquals("println(1,2);", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: formatting a compact if/else | Then: braces on the same"
                    + " line, '} else {' and the author's (empty) indentation kept")
    void formatter_whenIfElseWithDefaults_thenBracesOnSameLine() {
        // Given
        String source = IF_ELSE;

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("if(true) {\nprintln(\"a\");\n} else {\nprintln(\"b\");\n}", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: the '{' is on the line below the if | Then: moves it to the"
                    + " same line and keeps the author's indentation")
    void formatter_whenBraceBelowInSource_thenMovesItUp() {
        // Given
        String source = "if (true)\n{\n  println(1);\n}";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("if (true) {\n  println(1);\n}", output);
    }

    @Test
    @DisplayName(
            "Given: if-brace-below-line true | When: formatting a compact if/else | Then: each"
                    + " brace and the else go on their own line, with no indentation added")
    void formatter_whenBraceBelowLine_thenBracesOnOwnLines() {
        // Given
        String yaml = "if-brace-below-line: true";

        // When
        String output = format(yaml, IF_ELSE);

        // Then
        assertEquals("if(true)\n{\nprintln(\"a\");\n}\nelse\n{\nprintln(\"b\");\n}", output);
    }

    @Test
    @DisplayName(
            "Given: indent-inside-if 2 | When: formatting an if block | Then: indents the body"
                    + " with 2 spaces")
    void formatter_whenIndentTwo_thenIndentsTwoSpaces() {
        // Given
        String yaml = "indent-inside-if: 2";

        // When
        String output = format(yaml, "if(true){println(\"a\");}");

        // Then
        assertEquals("if(true) {\n  println(\"a\");\n}", output);
    }

    @Test
    @DisplayName(
            "Given: indent-inside-if 4 | When: formatting a badly indented if block | Then:"
                    + " re-indents the body with 4 spaces")
    void formatter_whenIndentFour_thenIndentsFourSpaces() {
        // Given
        String yaml = "indent-inside-if: 4";

        // When
        String output = format(yaml, "if(true){\n println(\"a\");\n          }");

        // Then
        assertEquals("if(true) {\n    println(\"a\");\n}", output);
    }

    @Test
    @DisplayName(
            "Given: indent-inside-if 2 | When: formatting nested ifs | Then: each level adds two"
                    + " spaces")
    void formatter_whenNestedIfs_thenIndentsEachLevel() {
        // Given
        String yaml = "indent-inside-if: 2";

        // When
        String output = format(yaml, "if(true){if(false){println(\"a\");}}");

        // Then
        assertEquals("if(true) {\n  if(false) {\n    println(\"a\");\n  }\n}", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: formatting an empty if block | Then: keeps '{}' together")
    void formatter_whenEmptyBlock_thenKeepsBracesTogether() {
        // Given
        String source = "if(true){}";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("if(true) {}", output);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-before-println 0 | When: println follows empty lines | Then: the"
                    + " empty lines are removed")
    void formatter_whenNoLinesBeforePrintln_thenRemovesEmptyLines() {
        // Given
        String yaml = "line-breaks-before-println: 0";

        // When
        String output = format(yaml, "let a: number = 1;\n\n\nprintln(a);");

        // Then
        assertEquals("let a: number = 1;\nprintln(a);", output);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-before-println 1 | When: println follows a statement | Then: one"
                    + " empty line before println")
    void formatter_whenOneLineBeforePrintln_thenLeavesOneEmptyLine() {
        // Given
        String yaml = "line-breaks-before-println: 1";

        // When
        String output = format(yaml, "let a: number = 1;println(a);");

        // Then
        assertEquals("let a: number = 1;\n\nprintln(a);", output);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-before-println 2 | When: println follows a statement | Then: two"
                    + " empty lines before println")
    void formatter_whenTwoLinesBeforePrintln_thenLeavesTwoEmptyLines() {
        // Given
        String yaml = "line-breaks-before-println: 2";

        // When
        String output = format(yaml, "let a: number = 1;println(a);");

        // Then
        assertEquals("let a: number = 1;\n\n\nprintln(a);", output);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-before-println 1 | When: println is the first statement | Then:"
                    + " nothing is added before it")
    void formatter_whenPrintlnIsFirstStatement_thenAddsNothingBefore() {
        // Given
        String yaml = "line-breaks-before-println: 1";

        // When
        String output = format(yaml, "println(1);");

        // Then
        assertEquals("println(1);", output);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-after-println 1 | When: a statement follows println | Then: one"
                    + " empty line after println")
    void formatter_whenOneLineAfterPrintln_thenLeavesOneEmptyLine() {
        // Given
        String yaml = "line-breaks-after-println: 1";

        // When
        String output = format(yaml, "println(1);let a: number = 1;");

        // Then
        assertEquals("println(1);\n\nlet a: number = 1;", output);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-after-println 0 | When: empty lines follow println | Then: they are"
                    + " removed")
    void formatter_whenNoLinesAfterPrintln_thenRemovesEmptyLines() {
        // Given
        String yaml = "line-breaks-after-println: 0";

        // When
        String output = format(yaml, "println(1);\n\n\nlet a: number = 1;");

        // Then
        assertEquals("println(1);\nlet a: number = 1;", output);
    }

    @Test
    @DisplayName(
            "Given: no options | When: the source has an unknown character | Then: fails with a"
                    + " formatting error wrapping the lexical error")
    void formatter_whenLexicalError_thenReturnsFormattingError() {
        // Given
        String source = "let a: number = 1 @ 2;";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertTrue(output.contains("Formatting error"));
        assertTrue(output.contains("Lexical error"));
        assertTrue(output.contains("'@'"));
    }

    @Test
    @DisplayName("Given: no options | When: the source is empty | Then: the output is empty")
    void formatter_whenSourceIsEmpty_thenOutputIsEmpty() {
        // Given
        String source = "";

        // When
        String output = format(NO_OPTIONS, source);

        // Then
        assertEquals("", output);
    }

    @Test
    @DisplayName(
            "Given: the assignment's sample configuration | When: formatting a small program |"
                    + " Then: every rule is applied together")
    void formatter_whenSampleConfiguration_thenAppliesAllRules() {
        // Given
        String yaml =
                "enforce-spacing-before-colon-in-declaration: true\n"
                        + "enforce-spacing-after-colon-in-declaration: true\n"
                        + "enforce-spacing-around-equals: true\n"
                        + "line-breaks-before-println: 1\n"
                        + "indent-inside-if: 2";
        String source = "let x:number=2*3;if(true){x=x+1;println(x);}";

        // When
        String output = format(yaml, source);

        // Then
        assertEquals(
                "let x : number = 2 * 3;\nif(true) {\n  x = x + 1;\n\n  println(x);\n}", output);
    }
}
