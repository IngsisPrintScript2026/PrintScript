/*
 * My Project
 */

package interpreter;

import static org.junit.jupiter.api.Assertions.*;

import builtin.DefaultFunctionRegistry;
import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import executor.DefaultStatementExecutor;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;

class EvaluatorAndExecutorTest {

    @Test
    void testExpressionEvaluator() {
        DefaultFunctionRegistry registry =
                new DefaultFunctionRegistry(prompt -> "inputVal", varName -> "envVal");
        List<String> output = new ArrayList<>();
        DefaultExpressionEvaluator evaluator =
                new DefaultExpressionEvaluator(registry, output::add);
        Environment env = new Environment();
        env.declare("x", new BigDecimal("10"), DataType.NUMBER, true);

        // Literals
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

        // Identifier
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

        // Call builtins
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

        CallFunctionNode unknownCall =
                new CallFunctionNode(new IdentifierNode("unknown", 1, 1), List.of(), 1, 1);
        assertFalse(evaluator.evaluate(unknownCall, env).isCorrect());

        // Unsupported expr
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

    @Test
    void testStatementExecutor() {
        List<String> output = new ArrayList<>();
        DefaultStatementExecutor executor = new DefaultStatementExecutor(output::add);
        Environment env = new Environment();

        // Declaration
        DeclarationKeywordNode decl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(new BigDecimal("10"), 1, 1),
                        DataType.NUMBER,
                        1,
                        1);
        Result<Void> declRes = executor.execute(decl, env);
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

        // Assign
        AssignNode assign =
                new AssignNode(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(new BigDecimal("42"), 1, 1),
                        1,
                        1);
        assertTrue(executor.execute(assign, env).isCorrect());
        assertEquals(new BigDecimal("42"), env.get("x").value());

        AssignNode badAssign =
                new AssignNode(
                        new IdentifierNode("nonExistent", 1, 1),
                        new NumberLiteralNode(new BigDecimal("42"), 1, 1),
                        1,
                        1);
        assertFalse(executor.execute(badAssign, env).isCorrect());

        // Call println
        CallFunctionNode call =
                new CallFunctionNode(
                        new IdentifierNode("println", 1, 1),
                        List.of(new IdentifierNode("x", 1, 1)),
                        1,
                        1);
        assertTrue(executor.execute(call, env).isCorrect());
        assertEquals(List.of("42"), output);

        // Call undefined function
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

    @Test
    void testDefaultInterpreterExecution() {
        List<String> output = new ArrayList<>();
        DefaultInterpreter interpreter = new DefaultInterpreter(output::add);

        DeclarationKeywordNode decl =
                new DeclarationKeywordNode(
                        DeclarationType.LET,
                        new IdentifierNode("greeting", 1, 1),
                        new StringLiteralNode("Hello from interpreter", 1, 1),
                        DataType.STRING,
                        1,
                        1);
        CallFunctionNode call =
                new CallFunctionNode(
                        new IdentifierNode("println", 1, 1),
                        List.of(new IdentifierNode("greeting", 1, 1)),
                        1,
                        1);

        ProgramNode program = new ProgramNode(List.of(decl, call), 1, 1);

        Result<Void> res = interpreter.interpret(program);
        assertTrue(res.isCorrect());
        assertEquals(List.of("Hello from interpreter"), output);

        // Test constructors
        assertNotNull(new DefaultInterpreter());
        assertNotNull(new DefaultInterpreter(output::add));
        assertNotNull(
                new DefaultInterpreter(
                        new syntactic.Parser<Node>() {
                            @Override
                            public Result<iterator.IterationStep<Node>> parse(
                                    tokenstream.TokenStream stream) {
                                return Result.failure("EOF");
                            }
                        },
                        null,
                        new DefaultStatementExecutor(output::add)));
    }
}
