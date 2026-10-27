/*
 * My Project
 */

package formatter.integration;

import static formatter.support.FormatterFixtures.parse;
import static formatter.support.FormatterFixtures.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

import formatter.ASTFormatter;
import formatter.FormatContext;
import formatter.support.FormatterFixtures;
import node.ProgramNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;

/** Real source → lexer → parser (1.1) → ASTFormatter, configured from a YAML string. */
class ASTFormatterIntegrationTest {

    private static String formatWith(ASTFormatter formatter, String source) {
        Result<String> result = formatter.format(parse(source));
        return ((CorrectResult<String>) result).value();
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter with defaults | When: formatting a parsed declaration and"
                    + " assignment | Then: writes one normalized statement per line")
    void astFormatter_whenDeclarationAndAssignment_thenFormatsBoth() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String output = formatWith(formatter, "let x:number=42;x=100;");

        // Then
        assertEquals("let x: number = 42;\nx = 100;", output);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter with defaults | When: formatting a parsed arithmetic expression |"
                    + " Then: operators are spaced")
    void astFormatter_whenArithmetic_thenSpacesOperators() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String output = formatWith(formatter, "let r:number=1+2*3;");

        // Then
        assertEquals("let r: number = 1 + 2 * 3;", output);
    }

    @Test
    @DisplayName(
            "Given: a YAML config with colon spaces, no spaces around '=' and if-brace-same-line"
                + " false | When: formatting a parsed const and if/else | Then: applies them all")
    void astFormatter_whenYamlConfig_thenAppliesConfiguredRules() {
        // Given
        String yaml =
                "space-before-colon: true\nspace-after-colon: true\nspace-around-equals: false\n"
                        + "indent-inside-if: 2\nif-brace-same-line: false";
        ASTFormatter formatter = ASTFormatter.fromYamlConfig(stream(yaml));

        // When
        String output =
                formatWith(
                        formatter,
                        "const flag: boolean = true; if (flag) { println(\"Yes\"); } else {"
                                + " println(\"No\"); }");

        // Then
        assertEquals(
                "const flag : boolean=true;\nif (flag)\n{\n  println(\"Yes\");\n}\nelse\n{\n"
                        + "  println(\"No\");\n}",
                output);
    }

    @Test
    @DisplayName(
            "Given: indent-inside-if 2 | When: formatting parsed nested ifs | Then: each level is"
                    + " indented two more spaces")
    void astFormatter_whenNestedIfs_thenIndentsEachLevel() {
        // Given
        FormatContext ctx = FormatterFixtures.context("indent-inside-if: 2");
        ASTFormatter formatter = new ASTFormatter(ctx);

        // When
        String output = formatWith(formatter, "if(true){if(false){println(1);}}");

        // Then
        assertEquals("if (true) {\n  if (false) {\n    println(1);\n  }\n}", output);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter with defaults | When: formatting a parsed readInput declaration |"
                    + " Then: keeps the call as the initial value")
    void astFormatter_whenReadInputDeclaration_thenFormatsCall() {
        // Given
        ASTFormatter formatter = new ASTFormatter();
        ProgramNode program = parse("let name:string=readInput(\"Name:\");");

        // When
        Result<String> result = formatter.format(program);

        // Then
        assertEquals(
                "let name: string = readInput(\"Name:\");",
                ((CorrectResult<String>) result).value());
    }
}
