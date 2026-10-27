/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;
import version.Version;

class ExpressionTypeInferenceVersionUnitTest {

    private static CallFunctionNode call(String name, ExpressionNode... args) {
        return new CallFunctionNode(new IdentifierNode(name, 1, 1), List.of(args), 1, 1);
    }

    private static NumberLiteralNode number(int value) {
        return new NumberLiteralNode(BigDecimal.valueOf(value), 1, 1);
    }

    private static StringLiteralNode string(String value) {
        return new StringLiteralNode(value, 1, 1);
    }

    private static String errorOf(Result<DataType> result) {
        assertFalse(result.isCorrect());
        return ((IncorrectResult<DataType>) result).error();
    }

    private static DataType typeOf(Result<DataType> result) {
        assertTrue(result.isCorrect());
        return ((CorrectResult<DataType>) result).value();
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 inference | When: inferring readInput(\"x\") | Then: fails"
                    + " saying the function is not supported in 1.0")
    void inference_whenReadInputInV10_thenFunctionNotSupported() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_0);

        // When
        Result<DataType> result =
                inference.inferType(call("readInput", string("x")), new SemanticEnvironment());

        // Then
        assertEquals("Function 'readInput' is not supported in PrintScript 1.0", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 inference | When: inferring readEnv(\"HOME\") | Then: fails as"
                    + " unsupported in 1.0")
    void inference_whenReadEnvInV10_thenFunctionNotSupported() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_0);

        // When
        Result<DataType> result =
                inference.inferType(call("readEnv", string("HOME")), new SemanticEnvironment());

        // Then
        assertTrue(errorOf(result).contains("'readEnv'"));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.1 inference | When: inferring an unknown function foo() | Then:"
                    + " fails saying foo is not supported in 1.1")
    void inference_whenUnknownFunction_thenNotSupportedInV11() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_1);

        // When
        Result<DataType> result = inference.inferType(call("foo"), new SemanticEnvironment());

        // Then
        assertEquals("Function 'foo' is not supported in PrintScript 1.1", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: default inference (1.1) | When: inferring readInput(\"x\") | Then: infers null,"
                    + " meaning it adapts to the target type")
    void inference_whenReadInputInDefaultVersion_thenInfersAdaptableNull() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference();

        // When
        Result<DataType> result =
                inference.inferType(call("readInput", string("x")), new SemanticEnvironment());

        // Then
        assertNull(typeOf(result));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.1 inference | When: inferring readEnv(\"HOME\") | Then: infers"
                    + " null (adapts to target)")
    void inference_whenReadEnvInV11_thenInfersAdaptableNull() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_1);

        // When
        Result<DataType> result =
                inference.inferType(call("readEnv", string("HOME")), new SemanticEnvironment());

        // Then
        assertNull(typeOf(result));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 inference | When: inferring println(1) | Then: infers STRING")
    void inference_whenPrintlnCall_thenInfersString() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_0);

        // When
        Result<DataType> result =
                inference.inferType(call("println", number(1)), new SemanticEnvironment());

        // Then
        assertEquals(DataType.STRING, typeOf(result));
    }

    @Test
    @DisplayName(
            "Given: an empty environment | When: inferring println(y) with undeclared y | Then:"
                    + " the argument error is reported")
    void inference_whenCallArgumentIsUndeclared_thenReportsArgumentError() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_0);

        // When
        Result<DataType> result =
                inference.inferType(
                        call("println", new IdentifierNode("y", 3, 9)), new SemanticEnvironment());

        // Then
        assertTrue(errorOf(result).contains("Variable 'y' is not declared"));
    }

    @Test
    @DisplayName(
            "Given: a call whose argument is 1 - \"a\" | When: inferring | Then: the nested"
                    + " numeric operator error is reported")
    void inference_whenCallArgumentHasInvalidOperation_thenReportsNestedError() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_1);
        OperatorNode badArg = new OperatorNode(OperatorType.MINUS, number(1), string("a"), 1, 5);

        // When
        Result<DataType> result =
                inference.inferType(call("println", badArg), new SemanticEnvironment());

        // Then
        assertTrue(errorOf(result).contains("Numeric operator requires number operands"));
    }

    @Test
    @DisplayName(
            "Given: string and number operands | When: inferring \"a\" + 1 | Then: the result is"
                    + " STRING (concatenation)")
    void inference_whenStringPlusNumber_thenInfersString() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_0);
        OperatorNode concat = new OperatorNode(OperatorType.PLUS, string("a"), number(1), 1, 1);

        // When
        Result<DataType> result = inference.inferType(concat, new SemanticEnvironment());

        // Then
        assertEquals(DataType.STRING, typeOf(result));
    }

    @Test
    @DisplayName(
            "Given: a variable declared as number | When: inferring its identifier | Then: infers"
                    + " NUMBER from the environment")
    void inference_whenIdentifierDeclared_thenInfersDeclaredType() {
        // Given
        ExpressionTypeInference inference = new ExpressionTypeInference(Version.V_1_0);
        SemanticEnvironment env =
                new SemanticEnvironment().define("x", DataType.NUMBER, true, false);

        // When
        Result<DataType> result = inference.inferType(new IdentifierNode("x", 1, 1), env);

        // Then
        assertEquals(DataType.NUMBER, typeOf(result));
    }
}
