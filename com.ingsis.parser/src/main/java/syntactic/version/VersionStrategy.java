/*
 * My Project
 */

package syntactic.version;

import java.util.List;
import java.util.function.Predicate;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.keyword.IfKeywordNode;
import syntactic.Parser;
import syntactic.parser.root.AssignParser;
import syntactic.parser.root.DeclarationParser;
import syntactic.parser.root.LineExpressionParser;
import token.Token;
import version.Version;

public interface VersionStrategy {
    Version version();

    Predicate<Token> declarationKeywords();

    Predicate<Token> supportedDataTypes();

    List<Parser<? extends ExpressionNode>> primaryParsers(
            Parser<NumberLiteralNode> numberLiteralParser,
            Parser<StringLiteralNode> stringLiteralParser,
            Parser<BooleanLiteralNode> booleanLiteralParser,
            Parser<? extends ExpressionNode> functionParser,
            Parser<node.expression.Identifier.IdentifierNode> identifierParser);

    List<Parser<? extends Node>> statementParsers(
            DeclarationParser declarationParser,
            AssignParser assignParser,
            Parser<IfKeywordNode> conditionalParser,
            LineExpressionParser lineExprParser);
}
