/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import java.math.BigDecimal;
import node.Node;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.keyword.DeclarationKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import parser.support.Sources;
import pipeline.StatementAction;
import pipeline.StatementPipeline;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import syntactic.parser.ParserFactory;
import version.Version;

/** Language rules of PrintScript 1.0, checked through the real lexer, parser and checker. */
class PrintScript10RulesIntegrationTest {

    private static Result<SemanticEnvironment> check(String source) {
        return new StatementPipeline(Version.V_1_0)
                .run(Sources.tokens(source), new SemanticEnvironment(), StatementAction.NONE);
    }

    private static String errorOf(String source) {
        Result<SemanticEnvironment> result = check(source);
        assertFalse(result.isCorrect());
        return ((IncorrectResult<SemanticEnvironment>) result).error();
    }

    private static Node firstStatement(String source) {
        Result<IterationStep<Node>> parsed =
                ParserFactory.createParser(Version.V_1_0).parse(Sources.tokens(source));
        assertTrue(parsed.isCorrect());
        return ((CorrectResult<IterationStep<Node>>) parsed).value().value();
    }

    private static DeclarationKeywordNode declarationIn(String source) {
        return assertInstanceOf(DeclarationKeywordNode.class, firstStatement(source));
    }

    @Test
    @DisplayName(
            "Given: 'let x: number = 5;' | When: parsing | Then: a declaration of x typed NUMBER"
                    + " with value 5")
    void v10_whenLetWithTypeAndValue_thenDeclarationNode() {
        // Given
        String source = "let x: number = 5;";

        // When
        DeclarationKeywordNode decl = declarationIn(source);

        // Then
        assertEquals("x", decl.identifierNode().name());
        assertEquals(DataType.NUMBER, decl.dataType());
        assertTrue(decl.isMutable());
    }

    @Test
    @DisplayName(
            "Given: 'let x = 5;' without type | When: checking | Then: syntactic error at the"
                    + " '=' because the type is mandatory")
    void v10_whenTypeIsMissing_thenUnexpectedEqual() {
        // Given
        String source = "let x = 5;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.startsWith("Syntactic error"));
        assertTrue(error.contains("Unexpected token '='"));
        assertTrue(error.contains("line 1, column 7"));
    }

    @Test
    @DisplayName(
            "Given: two variables in one declaration | When: checking | Then: syntactic error"
                    + " (one variable per statement)")
    void v10_whenTwoVariablesInOneDeclaration_thenSyntacticError() {
        // Given
        String source = "let x: number, y: number;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.startsWith("Syntactic error"));
        assertTrue(error.contains(","));
    }

    @Test
    @DisplayName(
            "Given: 'let x: number;' then 'x = 3;' | When: checking | Then: declaring first and"
                    + " assigning later is valid")
    void v10_whenDeclareThenAssign_thenValid() {
        // Given
        String source = "let x: number;\nx = 3;";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: a declared variable | When: reassigning a value of the same type | Then: it"
                    + " is valid")
    void v10_whenReassigningSameType_thenValid() {
        // Given
        String source = "let s: string = \"a\";\ns = \"b\";";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: a statement without ';' at the end of the file | When: checking | Then:"
                    + " syntactic error 'Unexpected end of file'")
    void v10_whenSemicolonMissingAtEnd_thenUnexpectedEndOfFile() {
        // Given
        String source = "println(\"hola\")";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Unexpected end of file"));
    }

    @Test
    @DisplayName(
            "Given: 'const' in 1.0 | When: checking | Then: unexpected token 'const', not valid in"
                    + " PrintScript 1.0")
    void v10_whenConstUsed_thenNotValidInV10() {
        // Given
        String source = "const x: number = 1;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Unexpected token 'const'"));
        assertTrue(error.contains("from line 1, column 1 to line 1, column 5"));
        assertTrue(error.contains("(not valid in PrintScript 1.0)"));
    }

    @Test
    @DisplayName(
            "Given: type 'boolean' in 1.0 | When: checking | Then: unexpected token 'boolean' with"
                    + " its range")
    void v10_whenBooleanTypeUsed_thenUnexpectedToken() {
        // Given
        String source = "let b: boolean = 1;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Unexpected token 'boolean'"));
        assertTrue(error.contains("from line 1, column 8 to line 1, column 14"));
    }

    @Test
    @DisplayName(
            "Given: boolean literal 'true' in 1.0 | When: checking | Then: unexpected token"
                    + " 'true', not valid in PrintScript 1.0")
    void v10_whenBooleanLiteralUsed_thenNotValidInV10() {
        // Given
        String source = "let x: number = true;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Unexpected token 'true'"));
        assertTrue(error.contains("(not valid in PrintScript 1.0)"));
    }

    @Test
    @DisplayName(
            "Given: an 'if' in 1.0 | When: checking | Then: unexpected token 'if', not valid in"
                    + " PrintScript 1.0")
    void v10_whenIfUsed_thenNotValidInV10() {
        // Given
        String source = "if (x) { println(1); }";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Unexpected token 'if'"));
        assertTrue(error.contains("(not valid in PrintScript 1.0)"));
    }

    @Test
    @DisplayName(
            "Given: readInput in 1.0 | When: checking | Then: semantic error, function not"
                    + " supported in PrintScript 1.0")
    void v10_whenReadInputUsed_thenFunctionNotSupported() {
        // Given
        String source = "let n: number = readInput(\"n?\");";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.startsWith("Semantic error"));
        assertTrue(error.contains("Function 'readInput' is not supported in PrintScript 1.0"));
    }

    @Test
    @DisplayName(
            "Given: a decimal literal 3.14 | When: parsing a declaration | Then: the value is the"
                    + " decimal number")
    void v10_whenDecimalNumber_thenParsedAsDecimal() {
        // Given
        String source = "let d: number = 3.14;";

        // When
        DeclarationKeywordNode decl = declarationIn(source);

        // Then
        NumberLiteralNode literal =
                assertInstanceOf(NumberLiteralNode.class, decl.expressionNode());
        assertEquals(0, new BigDecimal("3.14").compareTo(literal.rawValue()));
    }

    @Test
    @DisplayName(
            "Given: a single-quoted string | When: checking a string declaration | Then: it is a"
                    + " valid string literal")
    void v10_whenSingleQuotedString_thenValidString() {
        // Given
        String source = "let s: string = 'hola';";

        // When
        DeclarationKeywordNode decl = declarationIn(source);

        // Then
        assertInstanceOf(StringLiteralNode.class, decl.expressionNode());
        assertTrue(check(source).isCorrect());
    }

    @Test
    @DisplayName(
            "Given: '1 + 2 * 3' | When: parsing | Then: '*' binds tighter, so the tree is 1 + (2 *"
                    + " 3)")
    void v10_whenMixingPlusAndStar_thenStarHasHigherPrecedence() {
        // Given
        String source = "let r: number = 1 + 2 * 3;";

        // When
        OperatorNode root =
                assertInstanceOf(OperatorNode.class, declarationIn(source).expressionNode());

        // Then
        assertEquals(OperatorType.PLUS, root.operatorType());
        assertEquals(
                OperatorType.STAR,
                assertInstanceOf(OperatorNode.class, root.right()).operatorType());
    }

    @Test
    @DisplayName(
            "Given: '8 / 4 - 1' | When: parsing | Then: '/' binds tighter, so the tree is (8 / 4)"
                    + " - 1")
    void v10_whenMixingSlashAndMinus_thenSlashHasHigherPrecedence() {
        // Given
        String source = "let r: number = 8 / 4 - 1;";

        // When
        OperatorNode root =
                assertInstanceOf(OperatorNode.class, declarationIn(source).expressionNode());

        // Then
        assertEquals(OperatorType.MINUS, root.operatorType());
        assertEquals(
                OperatorType.SLASH,
                assertInstanceOf(OperatorNode.class, root.left()).operatorType());
    }

    @Test
    @DisplayName(
            "Given: string + number assigned to a string | When: checking | Then: valid, the"
                    + " concatenation is a string")
    void v10_whenConcatenatingStringAndNumberIntoString_thenValid() {
        // Given
        String source = "let s: string = \"total: \" + 5;";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: string + number assigned to a number | When: checking | Then: semantic error"
                    + " 'Cannot assign type STRING to variable 'n' of type NUMBER'")
    void v10_whenConcatenationAssignedToNumber_thenTypeMismatch() {
        // Given
        String source = "let n: number = \"a\" + 1;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Cannot assign type STRING to variable 'n' of type NUMBER"));
    }

    @Test
    @DisplayName(
            "Given: a subtraction with a string operand | When: checking | Then: semantic error,"
                    + " numeric operator needs numbers")
    void v10_whenSubtractingString_thenNumericOperatorError() {
        // Given
        String source = "let n: number = 5 - \"a\";";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Numeric operator requires number operands"));
    }

    @Test
    @DisplayName(
            "Given: println with an expression | When: checking | Then: it is a valid statement")
    void v10_whenPrintlnWithExpression_thenValid() {
        // Given
        String source = "let a: number = 2;\nprintln(\"Result: \" + a * 3);";

        // When
        Result<SemanticEnvironment> result = check(source);

        // Then
        assertTrue(result.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: a variable declared twice | When: checking | Then: semantic error on the"
                    + " second declaration")
    void v10_whenVariableRedeclared_thenSemanticError() {
        // Given
        String source = "let x: number = 1;\nlet x: number = 2;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("'x' is already declared"));
        assertTrue(error.contains("from line 2, column 1 to line 2, column 18"));
    }

    @Test
    @DisplayName(
            "Given: an assignment to an undeclared variable | When: checking | Then: semantic"
                    + " error naming the variable")
    void v10_whenAssigningUndeclared_thenSemanticError() {
        // Given
        String source = "y = 2;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("undeclared variable 'y'"));
    }

    @Test
    @DisplayName(
            "Given: a number variable assigned from a string variable | When: checking | Then:"
                    + " semantic type mismatch")
    void v10_whenDeclaringWithVariableOfOtherType_thenTypeMismatch() {
        // Given
        String source = "let n: number = 1;\nlet s: string = n;";

        // When
        String error = errorOf(source);

        // Then
        assertTrue(error.contains("Cannot assign type NUMBER to variable 's' of type STRING"));
    }
}
