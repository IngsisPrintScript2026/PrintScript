/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
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
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

class ExpressionTypeInferenceUnitTest {

    @Test
    @DisplayName(
            "What: ExpressionTypeInference | When: inferring literals, operators, and functions |"
                    + " Then: resolves correct DataType or fails appropriately")
    void typeInference_whenInferringExpressions_thenResolvesExpectedDataTypes() {
        // What: ExpressionTypeInference and SemanticEnvironment with typed variables
        ExpressionTypeInference inferencer = new ExpressionTypeInference();
        SemanticEnvironment env = new SemanticEnvironment();
        env = env.define("numVar", DataType.NUMBER, true, true);
        env = env.define("strVar", DataType.STRING, true, true);
        env = env.define("boolVar", DataType.BOOLEAN, true, true);

        // When & Then: inferring literal types
        assertEquals(
                DataType.NUMBER,
                ((CorrectResult<DataType>)
                                inferencer.inferType(
                                        new NumberLiteralNode(BigDecimal.TEN, 1, 1), env))
                        .value());
        assertEquals(
                DataType.STRING,
                ((CorrectResult<DataType>)
                                inferencer.inferType(new StringLiteralNode("s", 1, 1), env))
                        .value());
        assertEquals(
                DataType.BOOLEAN,
                ((CorrectResult<DataType>)
                                inferencer.inferType(new BooleanLiteralNode(true, 1, 1), env))
                        .value());
        assertFalse(inferencer.inferType(new NilExpressionNode(), env).isCorrect());

        // When & Then: inferring identifier types
        assertEquals(
                DataType.NUMBER,
                ((CorrectResult<DataType>)
                                inferencer.inferType(new IdentifierNode("numVar", 1, 1), env))
                        .value());
        assertFalse(inferencer.inferType(new IdentifierNode("unknown", 1, 1), env).isCorrect());

        // When & Then: PLUS operator with string concatenation and numeric addition
        OperatorNode strPlusNum =
                new OperatorNode(
                        OperatorType.PLUS,
                        new StringLiteralNode("a", 1, 1),
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        1,
                        1);
        assertEquals(
                DataType.STRING,
                ((CorrectResult<DataType>) inferencer.inferType(strPlusNum, env)).value());

        OperatorNode numPlusNum =
                new OperatorNode(
                        OperatorType.PLUS,
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);
        assertEquals(
                DataType.NUMBER,
                ((CorrectResult<DataType>) inferencer.inferType(numPlusNum, env)).value());

        // Incompatible operands
        OperatorNode badPlus =
                new OperatorNode(
                        OperatorType.PLUS,
                        new BooleanLiteralNode(true, 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);
        assertFalse(inferencer.inferType(badPlus, env).isCorrect());

        // Numeric minus
        OperatorNode minus =
                new OperatorNode(
                        OperatorType.MINUS,
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        1,
                        1);
        assertEquals(
                DataType.NUMBER,
                ((CorrectResult<DataType>) inferencer.inferType(minus, env)).value());

        OperatorNode badMinus =
                new OperatorNode(
                        OperatorType.MINUS,
                        new StringLiteralNode("a", 1, 1),
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        1,
                        1);
        assertFalse(inferencer.inferType(badMinus, env).isCorrect());

        // Function calls
        CallFunctionNode readInput =
                new CallFunctionNode(new IdentifierNode("readInput", 1, 1), List.of(), 1, 1);
        assertNull(((CorrectResult<DataType>) inferencer.inferType(readInput, env)).value());

        CallFunctionNode customFn =
                new CallFunctionNode(new IdentifierNode("println", 1, 1), List.of(), 1, 1);
        assertEquals(
                DataType.STRING,
                ((CorrectResult<DataType>) inferencer.inferType(customFn, env)).value());
    }
}
