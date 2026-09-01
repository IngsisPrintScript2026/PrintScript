/*
 * My Project
 */

package semantic;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import node.ProgramNode;
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
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

class SemanticCheckerTest {

    @Test
    void testSemanticEnvironment() {
        SemanticEnvironment env = new SemanticEnvironment();
        SemanticEnvironment env1 = env.define("x", DataType.NUMBER, true, true);

        assertTrue(env1.lookup("x").isPresent());
        assertEquals(DataType.NUMBER, env1.lookup("x").get().type());
        assertTrue(env1.lookup("x").get().isMutable());
        assertTrue(env1.lookup("x").get().isInitialized());

        SemanticEnvironment child = new SemanticEnvironment(env1);
        assertTrue(child.lookup("x").isPresent());
        assertFalse(child.lookup("nonExistent").isPresent());
    }

    @Test
    void testExpressionTypeInference() {
        ExpressionTypeInference inferencer = new ExpressionTypeInference();
        SemanticEnvironment env = new SemanticEnvironment();
        env = env.define("numVar", DataType.NUMBER, true, true);
        env = env.define("strVar", DataType.STRING, true, true);
        env = env.define("boolVar", DataType.BOOLEAN, true, true);

        // Literals
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

        // Identifiers
        assertEquals(
                DataType.NUMBER,
                ((CorrectResult<DataType>)
                                inferencer.inferType(new IdentifierNode("numVar", 1, 1), env))
                        .value());
        assertFalse(inferencer.inferType(new IdentifierNode("unknown", 1, 1), env).isCorrect());

        // Operators: PLUS with String and Number
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

        // Numeric operators (-, *, /)
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

        // Call functions (readInput, readEnv, custom)
        CallFunctionNode readInput =
                new CallFunctionNode(new IdentifierNode("readInput", 1, 1), List.of(), 1, 1);
        assertNull(((CorrectResult<DataType>) inferencer.inferType(readInput, env)).value());

        CallFunctionNode customFn =
                new CallFunctionNode(new IdentifierNode("println", 1, 1), List.of(), 1, 1);
        assertEquals(
                DataType.STRING,
                ((CorrectResult<DataType>) inferencer.inferType(customFn, env)).value());
    }

    @Test
    void testSemanticCheckerDeclarationsAndAssignments() {
        SemanticChecker checker = new SemanticChecker();

        // Valid let declaration
        DeclarationKeywordNode letDecl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        DataType.NUMBER,
                        1,
                        1);
        Result<SemanticEnvironment> res1 = checker.checkNode(letDecl, new SemanticEnvironment());
        assertTrue(res1.isCorrect());
        SemanticEnvironment env1 = ((CorrectResult<SemanticEnvironment>) res1).value();

        // Redeclaration error
        Result<SemanticEnvironment> reDeclRes = checker.checkNode(letDecl, env1);
        assertFalse(reDeclRes.isCorrect());

        // Type mismatch in declaration
        DeclarationKeywordNode badTypeDecl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("y", 1, 1),
                        new StringLiteralNode("text", 1, 1),
                        DataType.NUMBER,
                        1,
                        1);
        assertFalse(checker.checkNode(badTypeDecl, env1).isCorrect());

        // Uninitialized declaration
        DeclarationKeywordNode uninitDecl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("z", 1, 1),
                        new NilExpressionNode(),
                        DataType.NUMBER,
                        1,
                        1);
        Result<SemanticEnvironment> uninitRes = checker.checkNode(uninitDecl, env1);
        assertTrue(uninitRes.isCorrect());
        SemanticEnvironment env2 = ((CorrectResult<SemanticEnvironment>) uninitRes).value();

        // Valid assignment
        AssignNode validAssign =
                new AssignNode(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(99), 1, 1),
                        1,
                        1);
        assertTrue(checker.checkNode(validAssign, env2).isCorrect());

        // Assign to undeclared
        AssignNode undeclAssign =
                new AssignNode(
                        new IdentifierNode("unknown", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(99), 1, 1),
                        1,
                        1);
        assertFalse(checker.checkNode(undeclAssign, env2).isCorrect());

        // Const declaration and reassignment error
        DeclarationKeywordNode constDecl =
                new DeclarationKeywordNode(
                        DeclarationType.CONST,
                        new IdentifierNode("PI", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(3.14), 1, 1),
                        DataType.NUMBER,
                        1,
                        1);
        SemanticEnvironment envWithConst =
                ((CorrectResult<SemanticEnvironment>)
                                checker.checkNode(constDecl, new SemanticEnvironment()))
                        .value();
        AssignNode reassignConst =
                new AssignNode(
                        new IdentifierNode("PI", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(3.1415), 1, 1),
                        1,
                        1);
        assertFalse(checker.checkNode(reassignConst, envWithConst).isCorrect());
    }

    @Test
    void testSemanticCheckerIfAndCallFunction() {
        SemanticChecker checker = new SemanticChecker();

        // Valid if statement
        IfKeywordNode ifNode =
                new IfKeywordNode(
                        new BooleanLiteralNode(true, 1, 1),
                        List.of(
                                new DeclarationKeywordNode(
                                        DeclarationType.LET,
                                        new IdentifierNode("inner", 1, 1),
                                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                                        DataType.NUMBER,
                                        1,
                                        1)),
                        List.of(),
                        1,
                        1);
        assertTrue(checker.checkNode(ifNode, new SemanticEnvironment()).isCorrect());

        // Non-boolean condition if error
        IfKeywordNode badIf =
                new IfKeywordNode(
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1), List.of(), List.of(), 1, 1);
        assertFalse(checker.checkNode(badIf, new SemanticEnvironment()).isCorrect());

        // Call function node
        CallFunctionNode call =
                new CallFunctionNode(
                        new IdentifierNode("println", 1, 1),
                        List.of(new StringLiteralNode("hi", 1, 1)),
                        1,
                        1);
        assertTrue(checker.checkNode(call, new SemanticEnvironment()).isCorrect());

        // Call function with invalid arg (unresolved var)
        CallFunctionNode badCall =
                new CallFunctionNode(
                        new IdentifierNode("println", 1, 1),
                        List.of(new IdentifierNode("unresolved", 1, 1)),
                        1,
                        1);
        assertFalse(checker.checkNode(badCall, new SemanticEnvironment()).isCorrect());

        // Program node checking
        ProgramNode program = new ProgramNode(List.of(call, ifNode), 1, 1);
        assertTrue(checker.check(program).isCorrect());

        // SemanticStep
        SemanticStep step = new SemanticStep(call, new SemanticEnvironment(), null);
        assertEquals(call, step.node());
        assertNotNull(step.updatedEnv());
        assertNull(step.nextStream());
    }
}
