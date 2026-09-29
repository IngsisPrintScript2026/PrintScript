/*
 * My Project
 */

package syntactic.version;

import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import syntactic.Parser;

public record LiteralParsers(
        Parser<NumberLiteralNode> number,
        Parser<StringLiteralNode> string,
        Parser<BooleanLiteralNode> bool) {}
