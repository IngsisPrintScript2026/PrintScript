/*
 * My Project
 */

package node;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.factory.NodeFactory;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import node.visitor.NodeVisitor;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

class CommonNodesTest {

    @Test
    void testNodeFactoryAndNodes() {
        Token idTok = new Token(TokenType.IDENTIFIER, "x", new Position(1, 1));
        Token numTok = new Token(TokenType.NUMBER_LITERAL, "42", new Position(1, 5));
        Token letTok = new Token(TokenType.LET, "let", new Position(1, 1));
        Token assignTok = new Token(TokenType.EQUAL, "=", new Position(1, 3));
        Token ifTok = new Token(TokenType.IF, "if", new Position(2, 1));
        Token opTok = new Token(TokenType.PLUS, "+", new Position(1, 7));

        IdentifierNode idNode = NodeFactory.createIdentifier(idTok);
        assertEquals("x", idNode.name());
        assertEquals(1, idNode.line());
        assertEquals(1, idNode.column());
        assertEquals("x", idNode.symbol());
        assertTrue(idNode.children().isEmpty());

        NumberLiteralNode numNode = new NumberLiteralNode(new BigDecimal("42"), 1, 5);
        assertEquals(new BigDecimal("42"), ((CorrectResult<BigDecimal>) numNode.value()).value());
        assertEquals("42", numNode.symbol());
        assertTrue(numNode.children().isEmpty());

        StringLiteralNode strNode = new StringLiteralNode("hello", 1, 1);
        assertEquals("hello", ((CorrectResult<String>) strNode.value()).value());
        assertEquals("hello", strNode.symbol());
        assertTrue(strNode.children().isEmpty());

        BooleanLiteralNode boolNode = new BooleanLiteralNode(true, 1, 1);
        assertEquals(true, ((CorrectResult<Boolean>) boolNode.value()).value());
        assertEquals("true", boolNode.symbol());
        assertTrue(boolNode.children().isEmpty());

        NilExpressionNode nilNode = new NilExpressionNode();
        assertEquals("NIL", nilNode.symbol());
        assertEquals(-1, nilNode.line());
        assertEquals(-1, nilNode.column());
        assertTrue(nilNode.children().isEmpty());

        DeclarationKeywordNode decl1 =
                NodeFactory.createDeclaration(
                        DeclarationType.LET, idNode, numNode, DataType.NUMBER, letTok);
        assertEquals(DeclarationType.LET, decl1.declarationType());
        assertEquals(DataType.NUMBER, decl1.dataType());
        assertEquals("let", decl1.symbol());
        assertTrue(decl1.isMutable());
        assertEquals(2, decl1.children().size());

        DeclarationKeywordNode decl2 =
                NodeFactory.createDeclaration(DeclarationType.CONST, idNode, numNode, letTok);
        assertFalse(decl2.isMutable());
        assertNull(decl2.dataType());

        AssignNode assign = NodeFactory.createAssign(idNode, numNode, assignTok);
        assertEquals("=", assign.symbol());
        assertEquals(2, assign.children().size());

        CallFunctionNode call = NodeFactory.createCall("println", List.of(idNode, numNode), idTok);
        assertEquals("println", call.symbol());
        assertEquals(3, call.children().size());

        IfKeywordNode ifNode =
                NodeFactory.createIf(boolNode, List.of(decl1), List.of(assign), ifTok);
        assertEquals("if", ifNode.symbol());
        assertEquals(3, ifNode.children().size());

        OperatorNode opNode =
                NodeFactory.createOperator(OperatorType.PLUS, numNode, numNode, opTok);
        assertEquals("+", opNode.symbol());
        assertEquals(2, opNode.children().size());

        ProgramNode prog = NodeFactory.createProgram(List.of(decl1, assign, ifNode));
        assertEquals("PROGRAM", prog.symbol());
        assertEquals(3, prog.children().size());
        assertEquals(1, prog.line());
        assertEquals(1, prog.column());
    }

    @Test
    void testNodeVisitorAccept() {
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

        assertEquals("decl", decl.accept(visitor, null));
        assertEquals("assign", assign.accept(visitor, null));
        assertEquals("if", ifNode.accept(visitor, null));
        assertEquals("call", call.accept(visitor, null));
        assertEquals("prog", prog.accept(visitor, null));
        assertEquals("default", id.accept(visitor, null));
    }

    @Test
    void testEnums() {
        // DataType
        assertTrue(DataType.exists(new Token(TokenType.BOOLEAN, "boolean", new Position(1, 1))));
        assertTrue(DataType.exists(TokenType.STRING));
        assertFalse(DataType.exists((Token) null));
        assertFalse(DataType.exists((TokenType) null));
        assertEquals(Optional.of(DataType.NUMBER), DataType.fromTokenType(TokenType.NUMBER));
        assertEquals(Optional.empty(), DataType.fromTokenType(TokenType.IDENTIFIER));
        assertEquals(Optional.of(DataType.STRING), DataType.fromKeyword("STRING"));
        assertEquals(Optional.empty(), DataType.fromKeyword("UNKNOWN"));
        assertEquals(Optional.empty(), DataType.fromKeyword(null));

        // DeclarationType
        assertTrue(DeclarationType.exists(new Token(TokenType.LET, "let", new Position(1, 1))));
        assertTrue(DeclarationType.exists(TokenType.CONST));
        assertFalse(DeclarationType.exists((Token) null));
        assertFalse(DeclarationType.exists((TokenType) null));
        assertEquals(
                Optional.of(DeclarationType.LET), DeclarationType.fromTokenType(TokenType.LET));
        assertEquals(
                Optional.of(DeclarationType.CONST), DeclarationType.fromTokenType(TokenType.CONST));
        assertEquals(Optional.empty(), DeclarationType.fromTokenType(TokenType.IDENTIFIER));
        assertEquals(Optional.of(DeclarationType.LET), DeclarationType.fromKeyword("LET"));
        assertEquals(Optional.empty(), DeclarationType.fromKeyword(null));
        assertEquals(Optional.empty(), DeclarationType.fromKeyword("other"));

        // OperatorType
        assertTrue(OperatorType.isOperator("+"));
        assertTrue(OperatorType.isOperator("-"));
        assertTrue(OperatorType.isOperator("*"));
        assertTrue(OperatorType.isOperator("/"));
        assertTrue(OperatorType.isOperator("="));
        assertFalse(OperatorType.isOperator("?"));

        assertEquals(2, OperatorType.ASSIGNATION.lBindingPower());
        assertEquals(1, OperatorType.ASSIGNATION.rBindingPower());
        assertEquals("+", OperatorType.PLUS.symbol());

        Result<OperatorType> opRes = OperatorType.fromSymbol("+");
        assertTrue(opRes.isCorrect());
        Result<OperatorType> badRes = OperatorType.fromSymbol("invalid");
        assertFalse(badRes.isCorrect());
    }
}
