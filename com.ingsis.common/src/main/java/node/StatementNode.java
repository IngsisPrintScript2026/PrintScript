/*
 * My Project
 */

package node;

/**
 * Todo lo que puede aparecer como sentencia dentro de un programa o de un bloque. Una expresión
 * seguida de {@code ;} también es una sentencia, por eso {@link ExpressionNode} es un subtipo.
 */
public sealed interface StatementNode extends Node
        permits DeclarationKeywordNode,
                AssignNode,
                IfKeywordNode,
                WhileKeywordNode,
                ExpressionNode {}
