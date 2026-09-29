/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
import node.factory.NodeFactory;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

class NodeFactoryUnitTest {

    @Test
    @DisplayName(
            "What: NodeFactory | When: creating AST nodes | Then: properties and hierarchies match"
                    + " expectations")
    void nodeFactory_whenCreatingNodes_thenPropertiesAndHierarchiesMatchExpectations() {
        // What: tokens for AST node creation
        Token idTok = new Token(TokenType.IDENTIFIER, "x", new Position(1, 1));
        Token numTok = new Token(TokenType.NUMBER_LITERAL, "42", new Position(1, 5));
        Token letTok = new Token(TokenType.LET, "let", new Position(1, 1));
        Token assignTok = new Token(TokenType.EQUAL, "=", new Position(1, 3));
        Token ifTok = new Token(TokenType.IF, "if", new Position(2, 1));
        Token opTok = new Token(TokenType.PLUS, "+", new Position(1, 7));

        // When: creating nodes through NodeFactory and direct constructors
        IdentifierNode idNode = NodeFactory.createIdentifier(idTok);
        NumberLiteralNode numNode = new NumberLiteralNode(new BigDecimal("42"), 1, 5);
        StringLiteralNode strNode = new StringLiteralNode("hello", 1, 1);
        BooleanLiteralNode boolNode = new BooleanLiteralNode(true, 1, 1);
        NilExpressionNode nilNode = new NilExpressionNode();

        DeclarationKeywordNode decl1 =
                NodeFactory.createDeclaration(
                        new node.keyword.declaration.DeclarationInfo(
                                DeclarationType.LET, DataType.NUMBER, letTok),
                        idNode,
                        numNode);
        DeclarationKeywordNode decl2 =
                NodeFactory.createDeclaration(DeclarationType.CONST, idNode, numNode, letTok);
        AssignNode assign = NodeFactory.createAssign(idNode, numNode, assignTok);
        CallFunctionNode call = NodeFactory.createCall("println", List.of(idNode, numNode), idTok);
        IfKeywordNode ifNode =
                NodeFactory.createIf(boolNode, List.of(decl1), List.of(assign), ifTok);
        OperatorNode opNode =
                NodeFactory.createOperator(OperatorType.PLUS, numNode, numNode, opTok);
        ProgramNode prog = NodeFactory.createProgram(List.of(decl1, assign, ifNode));

        // Then: all nodes preserve expected symbols, children, and properties
        assertEquals("x", idNode.name());
        assertEquals(1, idNode.line());
        assertEquals(1, idNode.column());
        assertEquals("x", idNode.symbol());
        assertTrue(idNode.children().isEmpty());

        assertEquals(new BigDecimal("42"), numNode.rawValue());
        assertEquals(new BigDecimal("42"), ((CorrectResult<BigDecimal>) numNode.value()).value());
        assertEquals(1, numNode.line());
        assertEquals(5, numNode.column());
        assertEquals("42", numNode.symbol());
        assertTrue(numNode.children().isEmpty());

        assertEquals("hello", strNode.rawValue());
        assertEquals("hello", ((CorrectResult<String>) strNode.value()).value());
        assertEquals(1, strNode.line());
        assertEquals(1, strNode.column());
        assertEquals("hello", strNode.symbol());
        assertTrue(strNode.children().isEmpty());

        assertEquals(true, boolNode.rawValue());
        assertEquals(true, ((CorrectResult<Boolean>) boolNode.value()).value());
        assertEquals(1, boolNode.line());
        assertEquals(1, boolNode.column());
        assertEquals("true", boolNode.symbol());
        assertTrue(boolNode.children().isEmpty());

        assertEquals("NIL", nilNode.symbol());
        assertEquals(-1, nilNode.line());
        assertEquals(-1, nilNode.column());
        assertTrue(nilNode.children().isEmpty());

        assertEquals(DeclarationType.LET, decl1.declarationType());
        assertEquals(DataType.NUMBER, decl1.dataType());
        assertEquals(DataType.NUMBER, decl1.declaredType());
        assertEquals(idNode, decl1.identifierNode());
        assertEquals(numNode, decl1.expressionNode());
        assertEquals(1, decl1.line());
        assertEquals(1, decl1.column());
        assertEquals("let", decl1.symbol());
        assertTrue(decl1.isMutable());
        assertEquals(2, decl1.children().size());

        assertEquals(DeclarationType.CONST, decl2.declarationType());
        assertFalse(decl2.isMutable());
        assertNull(decl2.dataType());
        assertNull(decl2.declaredType());

        assertEquals("=", assign.symbol());
        assertEquals(idNode, assign.identifierNode());
        assertEquals(numNode, assign.expressionNode());
        assertEquals(1, assign.line());
        assertEquals(3, assign.column());
        assertEquals(2, assign.children().size());

        assertEquals("println", call.symbol());
        assertEquals("println", call.identifierNode().name());
        assertEquals(2, call.argumentNodes().size());
        assertEquals(1, call.line());
        assertEquals(1, call.column());
        assertEquals(3, call.children().size());

        assertEquals("if", ifNode.symbol());
        assertEquals(boolNode, ifNode.condition());
        assertEquals(1, ifNode.thenBody().size());
        assertEquals(1, ifNode.elseBody().size());
        assertEquals(2, ifNode.line());
        assertEquals(1, ifNode.column());
        assertEquals(3, ifNode.children().size());

        assertEquals("+", opNode.symbol());
        assertEquals(OperatorType.PLUS, opNode.operatorType());
        assertEquals(numNode, opNode.left());
        assertEquals(numNode, opNode.right());
        assertEquals(1, opNode.line());
        assertEquals(7, opNode.column());
        assertEquals(2, opNode.children().size());

        assertEquals("PROGRAM", prog.symbol());
        assertEquals(3, prog.statements().size());
        assertEquals(3, prog.children().size());
        assertEquals(1, prog.line());
        assertEquals(1, prog.column());
    }

    @Test
    @DisplayName(
            "What: DataType, DeclarationType, OperatorType | When: querying enums | Then:"
                    + " conversions and predicates succeed")
    void enums_whenQueried_thenConversionsAndPredicatesSucceed() {
        // What: tokens and strings for DataType, DeclarationType, OperatorType
        Token boolTok = new Token(TokenType.BOOLEAN, "boolean", new Position(1, 1));
        Token letTok = new Token(TokenType.LET, "let", new Position(1, 1));

        // When & Then: verify DataType queries
        assertTrue(DataType.exists(boolTok));
        assertTrue(DataType.exists(TokenType.STRING));
        assertFalse(DataType.exists((Token) null));
        assertFalse(DataType.exists((TokenType) null));
        assertEquals(Optional.of(DataType.NUMBER), DataType.fromTokenType(TokenType.NUMBER));
        assertEquals(Optional.empty(), DataType.fromTokenType(TokenType.IDENTIFIER));
        assertEquals(Optional.of(DataType.STRING), DataType.fromKeyword("STRING"));
        assertEquals(Optional.empty(), DataType.fromKeyword("UNKNOWN"));
        assertEquals(Optional.empty(), DataType.fromKeyword(null));

        // When & Then: verify DeclarationType queries
        assertTrue(DeclarationType.exists(letTok));
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

        // When & Then: verify OperatorType queries
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
