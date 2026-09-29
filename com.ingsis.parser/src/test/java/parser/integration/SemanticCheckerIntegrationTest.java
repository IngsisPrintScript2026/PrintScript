/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.SemanticStep;
import semantic.environment.SemanticEnvironment;

class SemanticCheckerIntegrationTest {

    @Test
    @DisplayName(
            "What: SemanticChecker | When: validating declarations, assignments, and scoping |"
                    + " Then: catches semantic errors and verifies valid operations")
    void
            semanticChecker_whenValidatingDeclarationsAndAssignments_thenCatchesErrorsAndPassesValid() {
        // What: SemanticChecker and declaration nodes
        SemanticChecker checker = new SemanticChecker();

        // Valid let declaration: let x: number = 10;
        DeclarationKeywordNode letDecl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        DataType.NUMBER,
                        1,
                        1);

        // When: checking valid let declaration
        Result<SemanticEnvironment> res1 = checker.checkNode(letDecl, new SemanticEnvironment());

        // Then: passes and updates environment
        assertTrue(res1.isCorrect());
        SemanticEnvironment env1 = ((CorrectResult<SemanticEnvironment>) res1).value();

        // When: redeclaring same variable "x"
        Result<SemanticEnvironment> reDeclRes = checker.checkNode(letDecl, env1);

        // Then: fails with redeclaration error
        assertFalse(reDeclRes.isCorrect());

        // When: declaration with type mismatch (number variable assigned string)
        DeclarationKeywordNode badTypeDecl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("y", 1, 1),
                        new StringLiteralNode("text", 1, 1),
                        DataType.NUMBER,
                        1,
                        1);
        Result<SemanticEnvironment> badTypeRes = checker.checkNode(badTypeDecl, env1);

        // Then: fails with type mismatch error
        assertFalse(badTypeRes.isCorrect());

        // When: uninitialized declaration (let z: number;)
        DeclarationKeywordNode uninitDecl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("z", 1, 1),
                        new NilExpressionNode(),
                        DataType.NUMBER,
                        1,
                        1);
        Result<SemanticEnvironment> uninitRes = checker.checkNode(uninitDecl, env1);

        // Then: succeeds
        assertTrue(uninitRes.isCorrect());
        SemanticEnvironment env2 = ((CorrectResult<SemanticEnvironment>) uninitRes).value();

        // When: valid assignment x = 99;
        AssignNode validAssign =
                new AssignNode(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(99), 1, 1),
                        1,
                        1);
        Result<SemanticEnvironment> validAssignRes = checker.checkNode(validAssign, env2);

        // Then: succeeds
        assertTrue(validAssignRes.isCorrect());

        // When: assignment to undeclared variable unknown = 99;
        AssignNode undeclAssign =
                new AssignNode(
                        new IdentifierNode("unknown", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(99), 1, 1),
                        1,
                        1);
        Result<SemanticEnvironment> undeclRes = checker.checkNode(undeclAssign, env2);

        // Then: fails
        assertFalse(undeclRes.isCorrect());

        // When: assigning to const variable PI = 3.1415;
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
        Result<SemanticEnvironment> reassignRes = checker.checkNode(reassignConst, envWithConst);

        // Then: fails because variable is immutable
        assertFalse(reassignRes.isCorrect());
    }

    @Test
    @DisplayName(
            "What: SemanticChecker | When: validating if statements, function calls, and programs |"
                    + " Then: validates conditions and expressions")
    void semanticChecker_whenValidatingIfAndFunctionCalls_thenValidatesConditionsAndExpressions() {
        // What: SemanticChecker
        SemanticChecker checker = new SemanticChecker();

        // When: checking valid if statement with boolean condition
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
        Result<SemanticEnvironment> ifRes = checker.checkNode(ifNode, new SemanticEnvironment());

        // Then: succeeds
        assertTrue(ifRes.isCorrect());

        // When: checking if statement with non-boolean condition
        IfKeywordNode badIf =
                new IfKeywordNode(
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1), List.of(), List.of(), 1, 1);
        Result<SemanticEnvironment> badIfRes = checker.checkNode(badIf, new SemanticEnvironment());

        // Then: fails
        assertFalse(badIfRes.isCorrect());

        // When: checking valid println call
        CallFunctionNode call =
                new CallFunctionNode(
                        new IdentifierNode("println", 1, 1),
                        List.of(new StringLiteralNode("hi", 1, 1)),
                        1,
                        1);
        Result<SemanticEnvironment> callRes = checker.checkNode(call, new SemanticEnvironment());

        // Then: succeeds
        assertTrue(callRes.isCorrect());

        // When: checking function call with unresolved argument
        CallFunctionNode badCall =
                new CallFunctionNode(
                        new IdentifierNode("println", 1, 1),
                        List.of(new IdentifierNode("unresolved", 1, 1)),
                        1,
                        1);
        Result<SemanticEnvironment> badCallRes =
                checker.checkNode(badCall, new SemanticEnvironment());

        // Then: fails
        assertFalse(badCallRes.isCorrect());

        // When: checking whole ProgramNode
        ProgramNode program = new ProgramNode(List.of(call, ifNode), 1, 1);
        Result<SemanticEnvironment> progRes = checker.check(program);

        // Then: passes semantic validation
        assertTrue(progRes.isCorrect());

        // When & Then: SemanticStep fields
        SemanticStep step = new SemanticStep(call, new SemanticEnvironment(), null);
        assertEquals(call, step.node());
        assertNotNull(step.updatedEnv());
        assertNull(step.nextStream());
    }
}
