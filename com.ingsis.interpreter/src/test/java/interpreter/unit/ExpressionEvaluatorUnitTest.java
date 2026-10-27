/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import builtin.DefaultFunctionRegistry;
import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import interpreter.fake.FakeEnvProvider;
import interpreter.fake.FakeInputProvider;
import interpreter.fake.SpyOutput;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;

class ExpressionEvaluatorUnitTest {

    private static NumberLiteralNode num(String value) {
        return new NumberLiteralNode(new BigDecimal(value), 1, 1);
    }

    private static StringLiteralNode str(String value) {
        return new StringLiteralNode(value, 1, 1);
    }

    private static OperatorNode op(OperatorType type, ExpressionNode left, ExpressionNode right) {
        return new OperatorNode(type, left, right, 1, 1);
    }

    private static CallFunctionNode call(String name, ExpressionNode... args) {
        return new CallFunctionNode(new IdentifierNode(name, 1, 1), List.of(args), 1, 1);
    }

    private static Object valueOf(Result<Object> result) {
        assertTrue(result.isCorrect(), () -> "expected success but was " + result);
        return ((CorrectResult<Object>) result).value();
    }

    private static String errorOf(Result<Object> result) {
        assertFalse(result.isCorrect(), () -> "expected failure but was " + result);
        return ((IncorrectResult<Object>) result).error();
    }

    private static DefaultExpressionEvaluator evaluatorReading(
            SpyOutput output, String... inputLines) {
        DefaultFunctionRegistry registry =
                new DefaultFunctionRegistry(
                        new FakeInputProvider(inputLines),
                        new FakeEnvProvider(Map.of("FLAG", "true", "PORT", "8080")));
        return new DefaultExpressionEvaluator(registry, output);
    }

    @Test
    @DisplayName("Given: a number literal | When: evaluating | Then: returns its value")
    void evaluator_whenEvaluatingNumberLiteral_thenReturnsNumber() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result = evaluator.evaluate(num("5"), new Environment());

        // Then
        assertEquals(new BigDecimal("5"), valueOf(result));
    }

    @Test
    @DisplayName("Given: a string literal | When: evaluating | Then: returns the text")
    void evaluator_whenEvaluatingStringLiteral_thenReturnsText() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result = evaluator.evaluate(str("hello"), new Environment());

        // Then
        assertEquals("hello", valueOf(result));
    }

    @Test
    @DisplayName("Given: a boolean literal | When: evaluating | Then: returns the boolean")
    void evaluator_whenEvaluatingBooleanLiteral_thenReturnsBoolean() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(new BooleanLiteralNode(true, 1, 1), new Environment());

        // Then
        assertEquals(Boolean.TRUE, valueOf(result));
    }

    @Test
    @DisplayName("Given: a nil expression | When: evaluating | Then: returns null")
    void evaluator_whenEvaluatingNil_thenReturnsNull() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result = evaluator.evaluate(new NilExpressionNode(), new Environment());

        // Then
        assertNull(valueOf(result));
    }

    @Test
    @DisplayName("Given: x declared as 10 | When: evaluating identifier x | Then: returns 10")
    void evaluator_whenEvaluatingDeclaredIdentifier_thenReturnsItsValue() {
        // Given
        Environment env = new Environment();
        env.declare("x", new BigDecimal("10"), DataType.NUMBER, true);
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result = evaluator.evaluate(new IdentifierNode("x", 1, 1), env);

        // Then
        assertEquals(new BigDecimal("10"), valueOf(result));
    }

    @Test
    @DisplayName(
            "Given: no variables | When: evaluating an unknown identifier | Then: fails with"
                    + " 'Variable error'")
    void evaluator_whenEvaluatingUndeclaredIdentifier_thenFails() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(new IdentifierNode("ghost", 1, 1), new Environment());

        // Then
        String error = errorOf(result);
        assertTrue(error.contains("Variable error"));
        assertTrue(error.contains("ghost"));
    }

    @Test
    @DisplayName("Given: 10 + 20 | When: evaluating | Then: returns 30")
    void evaluator_whenAddingNumbers_thenReturnsSum() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.PLUS, num("10"), num("20")), new Environment());

        // Then
        assertEquals(new BigDecimal("30"), valueOf(result));
    }

    @Test
    @DisplayName("Given: 10 - 4 | When: evaluating | Then: returns 6")
    void evaluator_whenSubtractingNumbers_thenReturnsDifference() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.MINUS, num("10"), num("4")), new Environment());

        // Then
        assertEquals(new BigDecimal("6"), valueOf(result));
    }

    @Test
    @DisplayName("Given: 1.5 * 2 | When: evaluating | Then: returns 3 normalized (not 3.0)")
    void evaluator_whenMultiplyingDecimals_thenReturnsNormalizedProduct() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.STAR, num("1.5"), num("2")), new Environment());

        // Then
        assertEquals("3", ((BigDecimal) valueOf(result)).toPlainString());
    }

    @Test
    @DisplayName("Given: 10 / 3 | When: evaluating | Then: returns 3.333333333333333")
    void evaluator_whenDividingTenByThree_thenReturnsDecimal() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.SLASH, num("10"), num("3")), new Environment());

        // Then
        assertEquals(new BigDecimal("3.333333333333333"), valueOf(result));
    }

    @Test
    @DisplayName("Given: 12 / 4 | When: evaluating | Then: returns exactly 3")
    void evaluator_whenDividingExactly_thenReturnsInteger() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.SLASH, num("12"), num("4")), new Environment());

        // Then
        assertEquals("3", ((BigDecimal) valueOf(result)).toPlainString());
    }

    @Test
    @DisplayName("Given: 1 / 0 | When: evaluating | Then: fails with 'Division by zero'")
    void evaluator_whenDividingByZero_thenFails() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.SLASH, num("1"), num("0")), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Division by zero"));
    }

    @Test
    @DisplayName("Given: (2 + 3) * 4 as a tree | When: evaluating | Then: returns 20")
    void evaluator_whenEvaluatingNestedOperators_thenRespectsTreeShape() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
        OperatorNode tree =
                op(OperatorType.STAR, op(OperatorType.PLUS, num("2"), num("3")), num("4"));

        // When
        Result<Object> result = evaluator.evaluate(tree, new Environment());

        // Then
        assertEquals(new BigDecimal("20"), valueOf(result));
    }

    @Test
    @DisplayName("Given: \"Result: \" + 3.0 | When: evaluating | Then: returns \"Result: 3\"")
    void evaluator_whenConcatenatingStringAndNumber_thenUsesDisplayForm() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(
                        op(OperatorType.PLUS, str("Result: "), num("3.0")), new Environment());

        // Then
        assertEquals("Result: 3", valueOf(result));
    }

    @Test
    @DisplayName("Given: 1E+2 + \" apples\" | When: evaluating | Then: returns \"100 apples\"")
    void evaluator_whenConcatenatingNumberAndString_thenPrintsPlainNumber() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(
                        op(OperatorType.PLUS, num("1E+2"), str(" apples")), new Environment());

        // Then
        assertEquals("100 apples", valueOf(result));
    }

    @Test
    @DisplayName("Given: \"a\" + \"b\" | When: evaluating | Then: returns \"ab\"")
    void evaluator_whenConcatenatingStrings_thenJoinsThem() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.PLUS, str("a"), str("b")), new Environment());

        // Then
        assertEquals("ab", valueOf(result));
    }

    @Test
    @DisplayName(
            "Given: \"a\" - 1 | When: evaluating | Then: fails with 'Operator evaluation error'")
    void evaluator_whenSubtractingFromString_thenFails() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(op(OperatorType.MINUS, str("a"), num("1")), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Operator evaluation error"));
    }

    @Test
    @DisplayName(
            "Given: an operator whose left operand is undeclared | When: evaluating | Then: the"
                    + " operand error is returned")
    void evaluator_whenLeftOperandFails_thenPropagatesError() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
        OperatorNode tree = op(OperatorType.PLUS, new IdentifierNode("nope", 1, 1), num("1"));

        // When
        Result<Object> result = evaluator.evaluate(tree, new Environment());

        // Then
        assertTrue(errorOf(result).contains("nope"));
    }

    @Test
    @DisplayName(
            "Given: an operator whose right operand is undeclared | When: evaluating | Then: the"
                    + " operand error is returned")
    void evaluator_whenRightOperandFails_thenPropagatesError() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
        OperatorNode tree = op(OperatorType.PLUS, num("1"), new IdentifierNode("nope", 1, 1));

        // When
        Result<Object> result = evaluator.evaluate(tree, new Environment());

        // Then
        assertTrue(errorOf(result).contains("nope"));
    }

    @Test
    @DisplayName(
            "Given: an assignation operator node | When: evaluating | Then: returns right side")
    void evaluator_whenEvaluatingAssignationOperator_thenReturnsRightValue() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result =
                evaluator.evaluate(
                        op(OperatorType.ASSIGNATION, num("1"), num("7")), new Environment());

        // Then
        assertEquals(new BigDecimal("7"), valueOf(result));
    }

    @Test
    @DisplayName(
            "Given: input \"41\" | When: evaluating readInput with number target | Then: returns"
                    + " the number 41")
    void evaluator_whenReadInputTargetsNumber_thenCoercesInput() {
        // Given
        DefaultExpressionEvaluator evaluator = evaluatorReading(new SpyOutput(), "41");

        // When
        Result<Object> result =
                evaluator.evaluate(call("readInput", str("n")), new Environment(), DataType.NUMBER);

        // Then
        assertEquals(new BigDecimal("41"), valueOf(result));
    }

    @Test
    @DisplayName(
            "Given: input \"41\" | When: evaluating readInput without target type | Then: returns"
                    + " the text \"41\"")
    void evaluator_whenReadInputHasNoTarget_thenReturnsString() {
        // Given
        DefaultExpressionEvaluator evaluator = evaluatorReading(new SpyOutput(), "41");

        // When
        Result<Object> result = evaluator.evaluate(call("readInput", str("n")), new Environment());

        // Then
        assertEquals("41", valueOf(result));
    }

    @Test
    @DisplayName(
            "Given: a readInput call with prompt | When: evaluating | Then: the prompt is emitted")
    void evaluator_whenReadInputHasPrompt_thenEmitsPrompt() {
        // Given
        SpyOutput output = new SpyOutput();
        DefaultExpressionEvaluator evaluator = evaluatorReading(output, "Joe");

        // When
        evaluator.evaluate(call("readInput", str("Name:")), new Environment(), DataType.STRING);

        // Then
        assertEquals(List.of("Name:"), output.lines());
    }

    @Test
    @DisplayName(
            "Given: input \"abc\" | When: evaluating readInput with number target | Then: fails"
                    + " with 'Function execution error' and the parse problem")
    void evaluator_whenReadInputCannotBeCoerced_thenFails() {
        // Given
        DefaultExpressionEvaluator evaluator = evaluatorReading(new SpyOutput(), "abc");

        // When
        Result<Object> result =
                evaluator.evaluate(call("readInput", str("n")), new Environment(), DataType.NUMBER);

        // Then
        String error = errorOf(result);
        assertTrue(error.contains("Function execution error"));
        assertTrue(error.contains("Cannot parse input 'abc' as number"));
    }

    @Test
    @DisplayName(
            "Given: env FLAG=true | When: evaluating readEnv with boolean target | Then: returns"
                    + " true")
    void evaluator_whenReadEnvTargetsBoolean_thenCoercesValue() {
        // Given
        DefaultExpressionEvaluator evaluator = evaluatorReading(new SpyOutput());

        // When
        Result<Object> result =
                evaluator.evaluate(
                        call("readEnv", str("FLAG")), new Environment(), DataType.BOOLEAN);

        // Then
        assertEquals(Boolean.TRUE, valueOf(result));
    }

    @Test
    @DisplayName(
            "Given: a readInput call whose argument is undeclared | When: evaluating | Then: the"
                    + " argument error is returned")
    void evaluator_whenCallArgumentFails_thenPropagatesError() {
        // Given
        DefaultExpressionEvaluator evaluator = evaluatorReading(new SpyOutput(), "x");

        // When
        Result<Object> result =
                evaluator.evaluate(
                        call("readInput", new IdentifierNode("missing", 1, 1)), new Environment());

        // Then
        assertTrue(errorOf(result).contains("missing"));
    }

    @Test
    @DisplayName(
            "Given: an evaluator without function registry | When: evaluating a call | Then: fails"
                    + " with 'Unsupported function call'")
    void evaluator_whenNoRegistry_thenCallIsUnsupported() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        // When
        Result<Object> result = evaluator.evaluate(call("readInput"), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Unsupported function call: readInput"));
    }

    @Test
    @DisplayName(
            "Given: a registry without 'foo' | When: evaluating foo() | Then: fails with"
                    + " 'Unsupported function call'")
    void evaluator_whenFunctionIsUnknown_thenFails() {
        // Given
        DefaultExpressionEvaluator evaluator = evaluatorReading(new SpyOutput());

        // When
        Result<Object> result = evaluator.evaluate(call("foo"), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Unsupported function call: foo"));
    }

    @Test
    @DisplayName(
            "Given: println used as an expression | When: evaluating | Then: fails because it"
                    + " returns no value")
    void evaluator_whenPrintlnUsedAsExpression_thenFails() {
        // Given
        DefaultExpressionEvaluator evaluator = evaluatorReading(new SpyOutput());

        // When
        Result<Object> result = evaluator.evaluate(call("println", str("x")), new Environment());

        // Then
        assertTrue(errorOf(result).contains("Unsupported function call: println"));
    }

    @Test
    @DisplayName(
            "Given: an unknown expression node type | When: evaluating | Then: fails with"
                    + " 'Unsupported expression node'")
    void evaluator_whenNodeTypeIsUnknown_thenFails() {
        // Given
        DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
        ExpressionNode unknown = new UnknownExpressionNode();

        // When
        Result<Object> result = evaluator.evaluate(unknown, new Environment());

        // Then
        assertTrue(errorOf(result).contains("Unsupported expression node"));
    }

    /** Expression node the evaluator does not know about. */
    private static final class UnknownExpressionNode implements ExpressionNode {
        @Override
        public Integer line() {
            return 1;
        }

        @Override
        public Integer column() {
            return 1;
        }

        @Override
        public String symbol() {
            return "?";
        }

        @Override
        public List<ExpressionNode> children() {
            return List.of();
        }
    }
}
