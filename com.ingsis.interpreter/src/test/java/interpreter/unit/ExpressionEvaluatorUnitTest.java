/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import builtin.DefaultFunctionRegistry;
import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

class ExpressionEvaluatorUnitTest {

    @Test
    @DisplayName(
            "What: DefaultExpressionEvaluator | When: evaluating literals and expressions | Then:"
                    + " produces correct evaluated objects")
    void expressionEvaluator_whenEvaluatingLiteralsAndExpressions_thenComputesValues() {
        // What: DefaultExpressionEvaluator configured with fake providers
        DefaultFunctionRegistry registry =
                new DefaultFunctionRegistry(prompt -> "inputVal", varName -> "envVal");
        List<String> output = new ArrayList<>();
        DefaultExpressionEvaluator evaluator =
                new DefaultExpressionEvaluator(registry, output::add);
        Environment env = new Environment();
        env.declare("x", new BigDecimal("10"), DataType.NUMBER, true);

        // When & Then: evaluate literals
        assertEquals(
                new BigDecimal("5"),
                ((CorrectResult<Object>)
                                evaluator.evaluate(
                                        new NumberLiteralNode(new BigDecimal("5"), 1, 1), env))
                        .value());
        assertEquals(
                "hello",
                ((CorrectResult<Object>)
                                evaluator.evaluate(new StringLiteralNode("hello", 1, 1), env))
                        .value());
        assertEquals(
                true,
                ((CorrectResult<Object>)
                                evaluator.evaluate(new BooleanLiteralNode(true, 1, 1), env))
                        .value());
        assertNull(
                ((CorrectResult<Object>) evaluator.evaluate(new NilExpressionNode(), env)).value());

        // When & Then: evaluate identifier
        assertEquals(
                new BigDecimal("10"),
                ((CorrectResult<Object>) evaluator.evaluate(new IdentifierNode("x", 1, 1), env))
                        .value());
        assertFalse(evaluator.evaluate(new IdentifierNode("unassigned", 1, 1), env).isCorrect());

        // Operators (+ - * / =)
        OperatorNode plusNum =
                new OperatorNode(
                        OperatorType.PLUS,
                        new NumberLiteralNode(new BigDecimal("10"), 1, 1),
                        new NumberLiteralNode(new BigDecimal("20"), 1, 1),
                        1,
                        1);
        assertEquals(
                new BigDecimal("30"),
                ((CorrectResult<Object>) evaluator.evaluate(plusNum, env)).value());

        OperatorNode plusStr =
                new OperatorNode(
                        OperatorType.PLUS,
                        new StringLiteralNode("foo", 1, 1),
                        new NumberLiteralNode(new BigDecimal("20"), 1, 1),
                        1,
                        1);
        assertEquals("foo20", ((CorrectResult<Object>) evaluator.evaluate(plusStr, env)).value());

        OperatorNode minus =
                new OperatorNode(
                        OperatorType.MINUS,
                        new NumberLiteralNode(new BigDecimal("20"), 1, 1),
                        new NumberLiteralNode(new BigDecimal("5"), 1, 1),
                        1,
                        1);
        assertEquals(
                new BigDecimal("15"),
                ((CorrectResult<Object>) evaluator.evaluate(minus, env)).value());

        OperatorNode star =
                new OperatorNode(
                        OperatorType.STAR,
                        new NumberLiteralNode(new BigDecimal("4"), 1, 1),
                        new NumberLiteralNode(new BigDecimal("5"), 1, 1),
                        1,
                        1);
        assertEquals(
                new BigDecimal("20"),
                ((CorrectResult<Object>) evaluator.evaluate(star, env)).value());

        OperatorNode slash =
                new OperatorNode(
                        OperatorType.SLASH,
                        new NumberLiteralNode(new BigDecimal("20"), 1, 1),
                        new NumberLiteralNode(new BigDecimal("5"), 1, 1),
                        1,
                        1);
        assertEquals(
                new BigDecimal("4"),
                ((CorrectResult<Object>) evaluator.evaluate(slash, env)).value());

        OperatorNode assignOp =
                new OperatorNode(
                        OperatorType.ASSIGNATION,
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(new BigDecimal("99"), 1, 1),
                        1,
                        1);
        assertEquals(
                new BigDecimal("99"),
                ((CorrectResult<Object>) evaluator.evaluate(assignOp, env)).value());

        // Function calls
        CallFunctionNode readEnvCall =
                new CallFunctionNode(
                        new IdentifierNode("readEnv", 1, 1),
                        List.of(new StringLiteralNode("FOO", 1, 1)),
                        1,
                        1);
        assertEquals(
                "envVal",
                ((CorrectResult<Object>) evaluator.evaluate(readEnvCall, env, DataType.STRING))
                        .value());

        CallFunctionNode readInputCall =
                new CallFunctionNode(
                        new IdentifierNode("readInput", 1, 1),
                        List.of(new StringLiteralNode("Prompt:", 1, 1)),
                        1,
                        1);
        assertEquals(
                "inputVal",
                ((CorrectResult<Object>) evaluator.evaluate(readInputCall, env, DataType.STRING))
                        .value());

        // Undefined function call
        CallFunctionNode unknownCall =
                new CallFunctionNode(new IdentifierNode("unknown", 1, 1), List.of(), 1, 1);
        assertFalse(evaluator.evaluate(unknownCall, env).isCorrect());

        // Unsupported custom expression
        ExpressionNode customExpr =
                new ExpressionNode() {
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
                        return "custom";
                    }

                    @Override
                    public List<ExpressionNode> children() {
                        return List.of();
                    }
                };
        assertFalse(evaluator.evaluate(customExpr, env).isCorrect());
    }
}
