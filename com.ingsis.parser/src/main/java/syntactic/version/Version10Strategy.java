/*
 * My Project
 */

package syntactic.version;

import java.util.List;
import java.util.function.Predicate;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.keyword.IfKeywordNode;
import syntactic.Parser;
import syntactic.parser.root.AssignParser;
import syntactic.parser.root.DeclarationParser;
import syntactic.parser.root.LineExpressionParser;
import token.Token;
import token.TokenType;
import tokenstream.rules.TokenMatchers;
import version.Version;

public class Version10Strategy implements VersionStrategy {

    @Override
    public Version version() {
        return Version.V_1_0;
    }

    @Override
    public Predicate<Token> declarationKeywords() {
        return TokenMatchers.isOneOf(TokenType.LET);
    }

    @Override
    public Predicate<Token> supportedDataTypes() {
        return TokenMatchers.isOneOf(TokenType.STRING, TokenType.NUMBER);
    }

    @Override
    public List<Parser<? extends ExpressionNode>> primaryParsers(
            Parser<NumberLiteralNode> numberLiteralParser,
            Parser<StringLiteralNode> stringLiteralParser,
            Parser<BooleanLiteralNode> booleanLiteralParser,
            Parser<? extends ExpressionNode> functionParser,
            Parser<IdentifierNode> identifierParser) {
        return List.of(numberLiteralParser, stringLiteralParser, functionParser, identifierParser);
    }

    @Override
    public List<Parser<? extends Node>> statementParsers(
            DeclarationParser declarationParser,
            AssignParser assignParser,
            Parser<IfKeywordNode> conditionalParser,
            LineExpressionParser lineExprParser) {
        return List.of(declarationParser, assignParser, lineExprParser);
    }
}
