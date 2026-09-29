/*
 * My Project
 */

package interpreter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import environment.Environment;
import executor.DefaultStatementExecutor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.Result;

class StatementExecutorUnitTest {

    @Test
    @DisplayName(
            "What: DefaultStatementExecutor | When: executing declarations, assignments,"
                    + " conditionals, and calls | Then: updates Environment and consumer")
    void statementExecutor_whenExecutingStatements_thenUpdatesEnvironmentAndOutput() {
        // What: DefaultStatementExecutor with fake output consumer and Environment
        List<String> output = new ArrayList<>();
        DefaultStatementExecutor executor = new DefaultStatementExecutor(output::add);
        Environment env = new Environment();

        // When: executing declaration
        DeclarationKeywordNode decl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(new BigDecimal("10"), 1, 1),
                        DataType.NUMBER,
                        1,
                        1);
        Result<Void> declRes = executor.execute(decl, env);

        // Then: variable x is defined with value 10
        assertTrue(declRes.isCorrect());
        assertEquals(new BigDecimal("10"), env.get("x").value());

        // Declaration without init expression
        DeclarationKeywordNode declNoInit =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("y", 1, 1),
                        null,
                        DataType.NUMBER,
                        1,
                        1);
        assertTrue(executor.execute(declNoInit, env).isCorrect());
        assertNull(env.get("y").value());

        // Re-declaration error
        assertFalse(executor.execute(decl, env).isCorrect());

        // When: executing assignment x = 42
        AssignNode assign =
                new AssignNode(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(new BigDecimal("42"), 1, 1),
                        1,
                        1);
        Result<Void> assignRes = executor.execute(assign, env);

        // Then: variable x is updated to 42
        assertTrue(assignRes.isCorrect());
        assertEquals(new BigDecimal("42"), env.get("x").value());

        // Invalid assignment to undeclared
        AssignNode badAssign =
                new AssignNode(
                        new IdentifierNode("nonExistent", 1, 1),
                        new NumberLiteralNode(new BigDecimal("42"), 1, 1),
                        1,
                        1);
        assertFalse(executor.execute(badAssign, env).isCorrect());

        // When: executing println(x)
        CallFunctionNode call =
                new CallFunctionNode(
                        new IdentifierNode("println", 1, 1),
                        List.of(new IdentifierNode("x", 1, 1)),
                        1,
                        1);
        Result<Void> callRes = executor.execute(call, env);

        // Then: output list receives "42"
        assertTrue(callRes.isCorrect());
        assertEquals(List.of("42"), output);

        // Call undefined function fails
        CallFunctionNode badCall =
                new CallFunctionNode(new IdentifierNode("doesNotExist", 1, 1), List.of(), 1, 1);
        assertFalse(executor.execute(badCall, env).isCorrect());

        // If statement - then branch
        IfKeywordNode ifThen =
                new IfKeywordNode(
                        new BooleanLiteralNode(true, 1, 1),
                        List.of(
                                new AssignNode(
                                        new IdentifierNode("x", 1, 1),
                                        new NumberLiteralNode(new BigDecimal("100"), 1, 1),
                                        1,
                                        1)),
                        List.of(),
                        1,
                        1);
        assertTrue(executor.execute(ifThen, env).isCorrect());
        assertEquals(new BigDecimal("100"), env.get("x").value());

        // If statement - else branch
        IfKeywordNode ifElse =
                new IfKeywordNode(
                        new BooleanLiteralNode(false, 1, 1),
                        List.of(),
                        List.of(
                                new AssignNode(
                                        new IdentifierNode("x", 1, 1),
                                        new NumberLiteralNode(new BigDecimal("200"), 1, 1),
                                        1,
                                        1)),
                        1,
                        1);
        assertTrue(executor.execute(ifElse, env).isCorrect());
        assertEquals(new BigDecimal("200"), env.get("x").value());

        // If with non-boolean condition error
        IfKeywordNode ifBadCond =
                new IfKeywordNode(
                        new StringLiteralNode("not-a-bool", 1, 1), List.of(), List.of(), 1, 1);
        assertFalse(executor.execute(ifBadCond, env).isCorrect());

        // Unsupported statement node
        ProgramNode unsupportedStmt = new ProgramNode(List.of(), 1, 1);
        assertFalse(executor.execute(unsupportedStmt, env).isCorrect());
    }
}
