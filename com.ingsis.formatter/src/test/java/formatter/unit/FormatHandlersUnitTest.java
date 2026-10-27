/*
 * My Project
 */

package formatter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import formatter.ASTFormatter;
import formatter.FormatContext;
import formatter.handler.AssignNodeFormatHandler;
import formatter.handler.CallFunctionNodeFormatHandler;
import formatter.handler.DeclarationNodeFormatHandler;
import formatter.handler.IfNodeFormatHandler;
import formatter.handler.ProgramNodeFormatHandler;
import formatter.support.FormatContextBuilder;
import java.math.BigDecimal;
import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormatHandlersUnitTest {
    private static final ASTFormatter FORMATTER = new ASTFormatter();
    private static final FormatContext DEFAULTS = new FormatContext();

    private static IdentifierNode id(String name) {
        return new IdentifierNode(name, 1, 1);
    }

    private static NumberLiteralNode one() {
        return new NumberLiteralNode(BigDecimal.ONE, 1, 1);
    }

    private static DeclarationKeywordNode untyped(DeclarationType kind, ExpressionNode value) {
        return new DeclarationKeywordNode(kind, id("v"), value, null, 1, 1);
    }

    private static CallFunctionNode println(ExpressionNode arg) {
        return new CallFunctionNode(id("println"), List.of(arg), 1, 1);
    }

    private static IfKeywordNode ifElse(List<Node> thenBody, List<Node> elseBody) {
        return new IfKeywordNode(new BooleanLiteralNode(true, 1, 1), thenBody, elseBody, 1, 1);
    }

    @Test
    @DisplayName(
            "Given: a declaration with a declared type | When: formatting it | Then: writes the"
                    + " keyword, name, lowercase type and value")
    void declarationHandler_whenTypeDeclared_thenWritesLowercaseType() {
        // Given
        DeclarationKeywordNode decl =
                new DeclarationKeywordNode(
                        DeclarationType.CONST,
                        id("ok"),
                        new BooleanLiteralNode(true, 1, 1),
                        DataType.BOOLEAN,
                        1,
                        1);

        // When
        String text = new DeclarationNodeFormatHandler().format(decl, DEFAULTS, FORMATTER);

        // Then
        assertEquals("const ok: boolean = true;", text);
    }

    @Test
    @DisplayName(
            "Given: declarations without declared type | When: formatting them | Then: the type is"
                    + " inferred from the literal (string, boolean, otherwise number)")
    void declarationHandler_whenTypeMissing_thenInfersItFromLiteral() {
        // Given
        DeclarationNodeFormatHandler handler = new DeclarationNodeFormatHandler();

        // When
        String text =
                handler.format(
                                untyped(DeclarationType.LET, new StringLiteralNode("a", 1, 1)),
                                DEFAULTS,
                                FORMATTER)
                        + handler.format(
                                untyped(DeclarationType.LET, new BooleanLiteralNode(true, 1, 1)),
                                DEFAULTS,
                                FORMATTER)
                        + handler.format(
                                untyped(DeclarationType.LET, id("x")), DEFAULTS, FORMATTER);

        // Then
        assertEquals("let v: string = \"a\";let v: boolean = true;let v: number = x;", text);
    }

    @Test
    @DisplayName(
            "Given: a declaration without initial value | When: formatting it | Then: writes no '='"
                    + " part")
    void declarationHandler_whenNoInitialValue_thenOmitsAssignment() {
        // Given
        DeclarationKeywordNode withNil =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        id("n"),
                        new NilExpressionNode(),
                        DataType.NUMBER,
                        1,
                        1);
        DeclarationKeywordNode withNull =
                new DeclarationKeywordNode(
                        DeclarationType.LET, id("n"), null, DataType.NUMBER, 1, 1);
        DeclarationNodeFormatHandler handler = new DeclarationNodeFormatHandler();

        // When
        String nil = handler.format(withNil, DEFAULTS, FORMATTER);
        String missing = handler.format(withNull, DEFAULTS, FORMATTER);

        // Then
        assertEquals("let n: number;", nil);
        assertEquals("let n: number;", missing);
    }

    @Test
    @DisplayName(
            "Given: space before colon on, after colon off and no spaces around '=' | When:"
                    + " formatting a declaration | Then: applies all three options")
    void declarationHandler_whenSpacingOptionsSet_thenAppliesThem() {
        // Given
        FormatContext ctx =
                FormatContextBuilder.unset()
                        .spaceBeforeColon(true)
                        .spaceAfterColon(false)
                        .spaceAroundEquals(false)
                        .build();
        DeclarationKeywordNode decl =
                new DeclarationKeywordNode(
                        DeclarationType.LET, id("x"), one(), DataType.NUMBER, 1, 1);

        // When
        String text = new DeclarationNodeFormatHandler().format(decl, ctx, FORMATTER);

        // Then
        assertEquals("let x :number=1;", text);
    }

    @Test
    @DisplayName(
            "Given: an assignment inside a block | When: formatting it with default rules | Then:"
                    + " indents it and spaces the '='")
    void assignHandler_whenNested_thenIndentsAndSpacesEquals() {
        // Given
        AssignNode assign = new AssignNode(id("x"), one(), 1, 1);

        // When
        String text =
                new AssignNodeFormatHandler().format(assign, DEFAULTS.incrementIndent(), FORMATTER);

        // Then
        assertEquals("    x = 1;", text);
    }

    @Test
    @DisplayName(
            "Given: no spaces around '=' | When: formatting an assignment | Then: glues the '='")
    void assignHandler_whenEqualsSpacingDisabled_thenGluesEquals() {
        // Given
        FormatContext ctx = FormatContextBuilder.unset().spaceAroundEquals(false).build();

        // When
        String text =
                new AssignNodeFormatHandler()
                        .format(new AssignNode(id("x"), one(), 1, 1), ctx, FORMATTER);

        // Then
        assertEquals("x=1;", text);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-after-println 3 | When: formatting a println statement | Then: adds"
                    + " two extra line breaks after it")
    void callHandler_whenPrintlnWithSeveralBreaks_thenAppendsExtraBreaks() {
        // Given
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksAfterPrintln(3).build();

        // When
        String text = new CallFunctionNodeFormatHandler().format(println(one()), ctx, FORMATTER);

        // Then
        assertEquals("println(1);\n\n", text);
    }

    @Test
    @DisplayName(
            "Given: line-breaks-after-println 3 | When: formatting a call to another function |"
                    + " Then: adds no extra line breaks")
    void callHandler_whenOtherFunction_thenAddsNoExtraBreaks() {
        // Given
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksAfterPrintln(3).build();
        CallFunctionNode call = new CallFunctionNode(id("readInput"), List.of(one(), one()), 1, 1);

        // When
        String text = new CallFunctionNodeFormatHandler().format(call, ctx, FORMATTER);

        // Then
        assertEquals("readInput(1, 1);", text);
    }

    @Test
    @DisplayName(
            "Given: default rules | When: formatting if/else | Then: braces on the same line, body"
                    + " indented and '} else {'")
    void ifHandler_whenBraceSameLine_thenWritesCompactIfElse() {
        // Given
        IfKeywordNode node = ifElse(List.of(println(one())), List.of(println(id("x"))));

        // When
        String text = new IfNodeFormatHandler().format(node, DEFAULTS, FORMATTER);

        // Then
        assertEquals("if (true) {\n    println(1);\n} else {\n    println(x);\n}", text);
    }

    @Test
    @DisplayName(
            "Given: if-brace-same-line false | When: formatting if/else | Then: braces and else go"
                    + " on their own lines")
    void ifHandler_whenBraceBelowLine_thenBracesOnOwnLines() {
        // Given
        FormatContext ctx = FormatContextBuilder.unset().ifBraceSameLine(false).build();
        IfKeywordNode node = ifElse(List.of(println(one())), List.of(println(one())));

        // When
        String text = new IfNodeFormatHandler().format(node, ctx, FORMATTER);

        // Then
        assertEquals("if (true)\n{\n    println(1);\n}\nelse\n{\n    println(1);\n}", text);
    }

    @Test
    @DisplayName("Given: an if without else | When: formatting it | Then: writes no else clause")
    void ifHandler_whenNoElse_thenOmitsElseClause() {
        // Given
        IfKeywordNode node = ifElse(List.of(new AssignNode(id("x"), one(), 1, 1)), List.of());

        // When
        String text = new IfNodeFormatHandler().format(node, DEFAULTS, FORMATTER);

        // Then
        assertEquals("if (true) {\n    x = 1;\n}", text);
    }

    @Test
    @DisplayName(
            "Given: a program with two statements | When: formatting it | Then: one statement per"
                    + " line")
    void programHandler_whenSeveralStatements_thenOnePerLine() {
        // Given
        ProgramNode program =
                new ProgramNode(
                        List.of(new AssignNode(id("a"), one(), 1, 1), println(id("a"))), 1, 1);

        // When
        String text = new ProgramNodeFormatHandler().format(program, DEFAULTS, FORMATTER);

        // Then
        assertEquals("a = 1;\nprintln(a);", text);
    }
}
