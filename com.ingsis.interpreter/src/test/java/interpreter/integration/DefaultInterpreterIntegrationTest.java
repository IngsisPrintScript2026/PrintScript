/*
 * My Project
 */

package interpreter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import executor.DefaultStatementExecutor;
import interpreter.DefaultInterpreter;
import java.util.ArrayList;
import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.DataType;
import node.expression.literal.StringLiteralNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.Result;

class DefaultInterpreterIntegrationTest {

    @Test
    @DisplayName(
            "What: DefaultInterpreter | When: interpreting full program AST | Then: executes"
                    + " declarations and outputs results")
    void defaultInterpreter_whenInterpretingCompleteASTProgram_thenExecutesPipeline() {
        // What: ProgramNode with let declaration and println call
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

        // When: interpreting program
        Result<Void> res = interpreter.interpret(program);

        // Then: execution succeeds and consumer receives printed greeting
        assertTrue(res.isCorrect());
        assertEquals(List.of("Hello from interpreter"), output);

        // When & Then: verifying constructor variants
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
