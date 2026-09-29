/*
 * My Project
 */

package node.keyword.declaration;

import node.expression.literal.DataType;
import token.Token;

public record DeclarationInfo(DeclarationType type, DataType declaredType, Token keyword) {}
