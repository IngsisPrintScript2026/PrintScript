/*
 * My Project
 */

package formatter.unit;

import static formatter.support.FormatterFixtures.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import formatter.ASTFormatter;
import formatter.FormatContext;
import formatter.fake.FakeNode;
import formatter.support.FormatContextBuilder;
import java.math.BigDecimal;
import java.util.List;
import node.ProgramNode;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;

class ASTFormatterUnitTest {

    private static NumberLiteralNode num(String value) {
        return new NumberLiteralNode(value == null ? null : new BigDecimal(value), 1, 1);
    }

    private static CallFunctionNode call(String name, ExpressionNode... args) {
        return new CallFunctionNode(new IdentifierNode(name, 1, 1), List.of(args), 1, 1);
    }

    private static DeclarationKeywordNode declaration(String name, ExpressionNode value) {
        return new DeclarationKeywordNode(
                DeclarationType.LET, new IdentifierNode(name, 1, 1), value, DataType.NUMBER, 1, 1);
    }

    @Test
    @DisplayName("Given: ASTFormatter | When: formatting a null program | Then: returns failure")
    void astFormatter_whenProgramIsNull_thenReturnsFailure() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        Result<String> result = formatter.format(null);

        // Then
        assertFalse(result.isCorrect());
    }

    @Test
    @DisplayName("Given: ASTFormatter | When: formatting a null node | Then: returns failure")
    void astFormatter_whenNodeIsNull_thenReturnsFailure() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        Result<String> result = formatter.formatNode(null);

        // Then
        assertFalse(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter with a null default context | When: formatting a program with a"
                    + " null context | Then: falls back to the default rules")
    void astFormatter_whenContextsAreNull_thenUsesDefaultRules() {
        // Given
        ASTFormatter formatter = new ASTFormatter((FormatContext) null);
        ProgramNode program = new ProgramNode(List.of(declaration("x", num("1"))), 1, 1);

        // When
        Result<String> result = formatter.format(program, null);

        // Then
        assertEquals("let x: number = 1;", ((CorrectResult<String>) result).value());
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: formatting a single node with a null context | Then: uses"
                    + " the default context")
    void astFormatter_whenFormattingNodeWithNullContext_thenUsesDefaults() {
        // Given
        ASTFormatter formatter = new ASTFormatter();
        AssignNode assign = new AssignNode(new IdentifierNode("x", 1, 1), num("2"), 1, 1);

        // When
        Result<String> result = formatter.formatNode(assign, null);

        // Then
        assertEquals("x = 2;", ((CorrectResult<String>) result).value());
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter with default rules | When: formatting a binary operation | Then:"
                    + " surrounds the operator with spaces")
    void astFormatter_whenFormattingOperation_thenSpacesOperator() {
        // Given
        ASTFormatter formatter = new ASTFormatter();
        OperatorNode op = new OperatorNode(OperatorType.STAR, num("2"), num("3"), 1, 1);

        // When
        String text = formatter.formatExpression(op);

        // Then
        assertEquals("2 * 3", text);
    }

    @Test
    @DisplayName(
            "Given: a context disabling operator spaces | When: formatting a binary operation |"
                    + " Then: glues the operator to its operands")
    void astFormatter_whenOperatorSpacesDisabled_thenGluesOperator() {
        // Given
        ASTFormatter formatter = new ASTFormatter();
        FormatContext ctx =
                new FormatContext(0, 4, null, null, null, false, null, null, null, null, null);
        OperatorNode op = new OperatorNode(OperatorType.MINUS, num("2"), num("3"), 1, 1);

        // When
        String text = formatter.formatExpression(op, ctx);

        // Then
        assertEquals("2-3", text);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: formatting a call expression with two arguments | Then:"
                    + " separates arguments with comma and space")
    void astFormatter_whenFormattingCallExpression_thenSeparatesArguments() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String text =
                formatter.formatExpression(call("f", num("1"), new IdentifierNode("y", 1, 1)));

        // Then
        assertEquals("f(1, y)", text);
    }

    @Test
    @DisplayName(
            "Given: a context without space after comma | When: formatting a call expression |"
                    + " Then: glues the arguments")
    void astFormatter_whenSpaceAfterCommaDisabled_thenGluesArguments() {
        // Given
        ASTFormatter formatter = new ASTFormatter();
        FormatContext ctx = FormatContextBuilder.unset().spaceAfterComma(false).build();

        // When
        String text = formatter.formatExpression(call("f", num("1"), num("2")), ctx);

        // Then
        assertEquals("f(1,2)", text);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: formatting a string literal | Then: wraps it in double"
                    + " quotes")
    void astFormatter_whenFormattingString_thenUsesDoubleQuotes() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String text = formatter.formatExpression(new StringLiteralNode("hi", 1, 1));

        // Then
        assertEquals("\"hi\"", text);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: formatting a number with trailing zeros | Then: writes the"
                    + " plain normalized number")
    void astFormatter_whenFormattingNumberWithZeros_thenNormalizesIt() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String decimal = formatter.formatExpression(num("1.50"));
        String hundred = formatter.formatExpression(num("100"));

        // Then
        assertEquals("1.5", decimal);
        assertEquals("100", hundred);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: formatting a number literal without value | Then: writes"
                    + " 0")
    void astFormatter_whenNumberValueIsNull_thenWritesZero() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String text = formatter.formatExpression(num(null));

        // Then
        assertEquals("0", text);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: formatting booleans, identifiers and nil | Then: writes"
                    + " their source text")
    void astFormatter_whenFormattingSimpleExpressions_thenWritesSourceText() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String bool = formatter.formatExpression(new BooleanLiteralNode(false, 1, 1));
        String id = formatter.formatExpression(new IdentifierNode("total", 1, 1));
        String nil = formatter.formatExpression(new NilExpressionNode());

        // Then
        assertEquals("false", bool);
        assertEquals("total", id);
        assertEquals("NIL", nil);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: an expression is used as a statement | Then: writes it"
                    + " indented and ending with ';'")
    void astFormatter_whenExpressionStatement_thenEndsWithSemicolon() {
        // Given
        ASTFormatter formatter = new ASTFormatter();
        FormatContext nested = new FormatContext().incrementIndent();

        // When
        String text = formatter.visitDefault(new IdentifierNode("x", 1, 1), nested);

        // Then
        assertEquals("    x;", text);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter | When: formatting a node it has no handler for | Then: writes the"
                    + " node symbol")
    void astFormatter_whenUnknownStatement_thenWritesSymbol() {
        // Given
        ASTFormatter formatter = new ASTFormatter();

        // When
        String text = formatter.formatStatement(new FakeNode("custom"), new FormatContext());

        // Then
        assertEquals("custom", text);
    }

    @Test
    @DisplayName(
            "Given: ASTFormatter without handlers | When: formatting every statement kind | Then:"
                    + " each produces an empty text")
    void astFormatter_whenNoHandlers_thenStatementsFormatToEmpty() {
        // Given
        ASTFormatter formatter = new ASTFormatter(List.of(), null);
        FormatContext ctx = new FormatContext();
        IfKeywordNode ifNode =
                new IfKeywordNode(new BooleanLiteralNode(true, 1, 1), List.of(), List.of(), 1, 1);

        // When
        String text =
                formatter.formatStatement(declaration("x", num("1")), ctx)
                        + formatter.formatStatement(
                                new AssignNode(new IdentifierNode("x", 1, 1), num("1"), 1, 1), ctx)
                        + formatter.formatStatement(ifNode, ctx)
                        + formatter.formatStatement(call("println"), ctx)
                        + formatter.formatStatement(new ProgramNode(List.of(), 1, 1), ctx);

        // Then
        assertEquals("", text);
    }

    @Test
    @DisplayName(
            "Given: a YAML config with space before colon | When: building the formatter from it"
                    + " and formatting a declaration | Then: the config is applied")
    void astFormatter_whenBuiltFromYaml_thenAppliesConfig() {
        // Given
        ASTFormatter formatter = ASTFormatter.fromYamlConfig(stream("space-before-colon: true"));

        // When
        Result<String> result = formatter.formatNode(declaration("x", num("1")));

        // Then
        assertEquals("let x : number = 1;", ((CorrectResult<String>) result).value());
    }
}
