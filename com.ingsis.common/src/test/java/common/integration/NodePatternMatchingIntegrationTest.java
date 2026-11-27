/*
 * My Project
 */

package common.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import node.AssignNode;
import node.BooleanLiteralNode;
import node.CallFunctionNode;
import node.DeclarationKeywordNode;
import node.IdentifierNode;
import node.IfKeywordNode;
import node.NilExpressionNode;
import node.Node;
import node.NumberLiteralNode;
import node.OperatorNode;
import node.ProgramNode;
import node.StringLiteralNode;
import node.WhileKeywordNode;
import node.expression.literal.DataType;
import node.expression.operator.OperatorType;
import node.factory.NodeFactory;
import node.keyword.declaration.DeclarationInfo;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import token.Token;
import token.TokenType;

class NodePatternMatchingIntegrationTest {

    // Sin rama default: si se agrega un nodo a la jerarquía sellada, esto deja de compilar.
    private static String tag(Node node) {
        return switch (node) {
            case ProgramNode p -> "prog";
            case DeclarationKeywordNode d -> "decl";
            case AssignNode a -> "assign";
            case IfKeywordNode i -> "if";
            case WhileKeywordNode w -> "while";
            case CallFunctionNode c -> "call";
            case IdentifierNode id -> "id";
            case OperatorNode op -> "op";
            case NumberLiteralNode n -> "number";
            case StringLiteralNode s -> "string";
            case BooleanLiteralNode b -> "boolean";
            case NilExpressionNode nil -> "nil";
        };
    }

    @Test
    @DisplayName(
            "Given: sealed AST | When: switching exhaustively over heterogeneous nodes | Then:"
                    + " each node matches its own case")
    void sealedNode_whenSwitchingExhaustively_thenEachNodeMatchesItsCase() {
        // Given: one instance of every node kind
        Token tok = new Token(TokenType.IDENTIFIER, "test", new Position(1, 1));
        IdentifierNode id = NodeFactory.createIdentifier(tok);
        NumberLiteralNode num = new NumberLiteralNode(BigDecimal.TEN, 1, 1);
        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(
                        new DeclarationInfo(DeclarationType.LET, DataType.NUMBER, tok), id, num);
        AssignNode assign = NodeFactory.createAssign(id, num, tok);
        CallFunctionNode call = NodeFactory.createCall("print", List.of(num), tok);
        IfKeywordNode ifNode = NodeFactory.createIf(num, List.of(), List.of(), tok);
        WhileKeywordNode whileNode = NodeFactory.createWhile(num, List.of(), tok);
        OperatorNode op = NodeFactory.createOperator(OperatorType.PLUS, num, num, tok);
        ProgramNode prog = NodeFactory.createProgram(List.of(decl));

        // When / Then: the exhaustive switch dispatches each node to its case
        assertEquals("decl", tag(decl));
        assertEquals("assign", tag(assign));
        assertEquals("if", tag(ifNode));
        assertEquals("while", tag(whileNode));
        assertEquals("call", tag(call));
        assertEquals("prog", tag(prog));
        assertEquals("id", tag(id));
        assertEquals("op", tag(op));
        assertEquals("number", tag(num));
        assertEquals("string", tag(new StringLiteralNode("s", 1, 1)));
        assertEquals("boolean", tag(new BooleanLiteralNode(true, 1, 1)));
        assertEquals("nil", tag(new NilExpressionNode()));
    }

    @Test
    @DisplayName(
            "Given: OperatorNode | When: deconstructing with a record pattern | Then: binds its"
                    + " components")
    void operatorNode_whenDeconstructedWithRecordPattern_thenBindsComponents() {
        // Given: 1 + 2
        Node node =
                new OperatorNode(
                        OperatorType.PLUS,
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        new NumberLiteralNode(BigDecimal.TWO, 1, 5),
                        1,
                        3);

        // When: matching with a nested record pattern
        String result =
                switch (node) {
                    case OperatorNode(
                                    OperatorType type,
                                    NumberLiteralNode(BigDecimal l, var ll, var lc),
                                    NumberLiteralNode(BigDecimal r, var rl, var rc),
                                    var line,
                                    var column) ->
                            l + " " + type.symbol() + " " + r;
                    default -> "other";
                };

        // Then: the nested components are bound
        assertEquals("1 + 2", result);
    }
}
