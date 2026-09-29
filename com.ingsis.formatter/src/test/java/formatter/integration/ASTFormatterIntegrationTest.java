/*
 * My Project
 */

package formatter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.ASTFormatter;
import formatter.FormatContext;
import formatter.Formatter;
import formatter.config.YamlFormatRulesLoader;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.factory.NodeFactory;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

class ASTFormatterIntegrationTest {
    private static final String PRINTLN = "println";

    @Test
    @DisplayName(
            "What: ASTFormatter | When: formatting declaration and assign with default rules |"
                    + " Then: emits correctly formatted source")
    void astFormatter_whenFormattingDeclarationAndAssignWithDefaults_thenEmitsFormattedCode() {
        // What: ProgramNode with let declaration and assignment
        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(
                        DeclarationType.LET,
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(42), 1, 1),
                        new Token(TokenType.LET, "let", new Position(1, 1)));

        AssignNode assign =
                NodeFactory.createAssign(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(100), 1, 1),
                        new Token(TokenType.EQUAL, "=", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(decl, assign));
        Formatter formatter = new ASTFormatter();

        // When: formatting program
        Result<String> result = formatter.format(program);

        // Then: output contains formatted let and assignment
        assertTrue(result.isCorrect(), "Formatting should succeed");
        String formatted = ((CorrectResult<String>) result).value();
        assertTrue(formatted.contains("let x: number = 42;"));
        assertTrue(formatted.contains("x = 100;"));
    }

    @Test
    @DisplayName(
            "What: ASTFormatter | When: formatting with YAML rules for consigna 1 and 2 | Then:"
                    + " applies colons, braces, and indents as specified")
    void astFormatter_whenFormattingWithYamlConfigConsigna1And2_thenAppliesAllRulesAccurately() {
        // What: YAML config and program with const and if-else
        String yamlConfig =
                """
                space-before-colon: true
                space-after-colon: true
                space-around-equals: false
                indent-inside-if: 2
                if-brace-same-line: false
                """;

        ByteArrayInputStream configStream =
                new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
        FormatContext context = YamlFormatRulesLoader.loadFromYaml(configStream);
        ASTFormatter formatter = new ASTFormatter(context);

        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(
                        DeclarationType.CONST,
                        new IdentifierNode("flag", 1, 1),
                        new BooleanLiteralNode(true, 1, 1),
                        new Token(TokenType.CONST, "const", new Position(1, 1)));

        CallFunctionNode callThen =
                NodeFactory.createCall(
                        PRINTLN,
                        List.of(new StringLiteralNode("Yes", 2, 5)),
                        new Token(TokenType.IDENTIFIER, PRINTLN, new Position(2, 5)));

        CallFunctionNode callElse =
                NodeFactory.createCall(
                        PRINTLN,
                        List.of(new StringLiteralNode("No", 3, 5)),
                        new Token(TokenType.IDENTIFIER, PRINTLN, new Position(3, 5)));

        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        new IdentifierNode("flag", 1, 1),
                        List.of(callThen),
                        List.of(callElse),
                        new Token(TokenType.IF, "if", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(decl, ifNode));

        // When: formatting program with YAML rules
        Result<String> result = formatter.format(program);

        // Then: formatting complies with colon spaces, brace positions, and indentation
        assertTrue(result.isCorrect());
        String formatted = ((CorrectResult<String>) result).value();

        assertTrue(
                formatted.contains("const flag : boolean=true;"),
                "Should respect space-before-colon and no space-around-equals");
        assertTrue(
                formatted.contains("if (flag)\n{"),
                "Should put brace below line when if-brace-same-line is false");
        assertTrue(
                formatted.contains("else\n{"),
                "Should put else brace below line when if-brace-same-line is false");
        assertTrue(
                formatted.contains("  println(\"Yes\");"),
                "Should use 2 spaces indentation inside if");
        assertTrue(
                formatted.contains("  println(\"No\");"),
                "Should use 2 spaces indentation inside else");
    }

    @Test
    @DisplayName(
            "What: ASTFormatter | When: formatting if statement with same-line brace and assignment"
                    + " without spaces | Then: applies styles accurately")
    void astFormatter_whenFormattingIfWithSameLineBraceAndNoSpacesAroundEquals_thenAppliesStyles() {
        // What: FormatContext with if-brace-same-line and no space around equals
        FormatContext ctx =
                new FormatContext(
                        0,
                        4,
                        false, // spaceBeforeColon
                        true, // spaceAfterColon
                        false, // spaceAroundEquals
                        true, // spaceAroundOperators
                        true, // lineBreakAfterStatement
                        1, // lineBreaksAfterPrintln
                        false, // singleSpaceSeparation
                        true, // ifBraceSameLine
                        false // ifBraceBelowLine
                        );
        ASTFormatter formatter = new ASTFormatter(ctx);

        CallFunctionNode callThen =
                NodeFactory.createCall(
                        PRINTLN,
                        List.of(new StringLiteralNode("Yes", 1, 1)),
                        new Token(TokenType.IDENTIFIER, PRINTLN, new Position(1, 1)));

        CallFunctionNode callElse =
                NodeFactory.createCall(
                        PRINTLN,
                        List.of(new StringLiteralNode("No", 1, 1)),
                        new Token(TokenType.IDENTIFIER, PRINTLN, new Position(1, 1)));

        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        new BooleanLiteralNode(true, 1, 1),
                        List.of(callThen),
                        List.of(callElse),
                        new Token(TokenType.IF, "if", new Position(1, 1)));

        // When: formatting statement
        String formatted = formatter.formatStatement(ifNode, ctx);

        // Then: braces stay on same line
        assertTrue(formatted.contains("if (true) {\n"));
        assertTrue(formatted.contains("} else {\n"));

        // When: formatting assignment
        AssignNode assign =
                new AssignNode(
                        new IdentifierNode("a", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);
        String formattedAssign = formatter.formatStatement(assign, ctx);

        // Then: no spaces around equals
        assertEquals("a=10;", formattedAssign);
    }

    @Test
    @DisplayName(
            "What: ASTFormatter with YAML space-after-comma | When: formatting call with multiple"
                + " args | Then: formats arguments separated by commas without space when false")
    void astFormatter_whenYamlConfigSetsSpaceAfterCommaFalse_thenFormatsWithoutSpaceAfterComma() {
        // What: YAML config disabling space after comma and call node with multiple arguments
        String yamlConfig = "space-after-comma: false\n";
        ByteArrayInputStream configStream =
                new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
        FormatContext context = YamlFormatRulesLoader.loadFromYaml(configStream);
        ASTFormatter formatter = new ASTFormatter(context);

        CallFunctionNode call =
                NodeFactory.createCall(
                        "func",
                        List.of(new StringLiteralNode("a", 1, 1), new StringLiteralNode("b", 1, 2)),
                        new Token(TokenType.IDENTIFIER, "func", new Position(1, 1)));

        // When: formatting call node
        Result<String> res = formatter.formatNode(call);

        // Then: arguments are separated by comma without space
        assertTrue(res.isCorrect());
        assertEquals("func(\"a\",\"b\");", ((CorrectResult<String>) res).value());
    }
}
