/*
 * My Project
 */

package common.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.factory.NodeFactory;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import node.visitor.NodeVisitor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import token.Token;
import token.TokenType;

class NodeVisitorIntegrationTest {

    @Test
    @DisplayName(
            "What: NodeVisitor | When: visiting heterogeneous AST nodes | Then: dispatches"
                    + " polymorphic visits correctly")
    void nodeVisitor_whenVisitingHeterogeneousNodes_thenDispatchesPolymorphicVisits() {
        // What: full set of AST nodes and a visitor implementation
        Token tok = new Token(TokenType.IDENTIFIER, "test", new Position(1, 1));
        IdentifierNode id = NodeFactory.createIdentifier(tok);
        NumberLiteralNode num = new NumberLiteralNode(BigDecimal.TEN, 1, 1);
        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(DeclarationType.LET, id, num, DataType.NUMBER, tok);
        AssignNode assign = NodeFactory.createAssign(id, num, tok);
        CallFunctionNode call = NodeFactory.createCall("print", List.of(num), tok);
        IfKeywordNode ifNode = NodeFactory.createIf(num, List.of(), List.of(), tok);
        ProgramNode prog = NodeFactory.createProgram(List.of(decl));

        NodeVisitor<String, Void> visitor =
                new NodeVisitor<String, Void>() {
                    @Override
                    public String visit(DeclarationKeywordNode node, Void context) {
                        return "decl";
                    }

                    @Override
                    public String visit(AssignNode node, Void context) {
                        return "assign";
                    }

                    @Override
                    public String visit(IfKeywordNode node, Void context) {
                        return "if";
                    }

                    @Override
                    public String visit(CallFunctionNode node, Void context) {
                        return "call";
                    }

                    @Override
                    public String visit(ProgramNode node, Void context) {
                        return "prog";
                    }

                    @Override
                    public String visitDefault(Node node, Void context) {
                        return "default";
                    }
                };

        // When: invoking accept on each distinct node
        String declResult = decl.accept(visitor, null);
        String assignResult = assign.accept(visitor, null);
        String ifResult = ifNode.accept(visitor, null);
        String callResult = call.accept(visitor, null);
        String progResult = prog.accept(visitor, null);
        String defaultResult = id.accept(visitor, null);

        // Then: each visitor method produces its corresponding dispatched tag
        assertEquals("decl", declResult);
        assertEquals("assign", assignResult);
        assertEquals("if", ifResult);
        assertEquals("call", callResult);
        assertEquals("prog", progResult);
        assertEquals("default", defaultResult);
    }
}
