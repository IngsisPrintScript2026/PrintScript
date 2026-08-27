/*
 * My Project
 */

package sca.handler;

import java.util.ArrayList;
import java.util.List;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.LiteralNode;
import node.expression.function.CallFunctionNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

public class CallFunctionScaHandler implements ScaNodeHandler<CallFunctionNode> {
    @Override
    public Class<CallFunctionNode> nodeType() {
        return CallFunctionNode.class;
    }

    @Override
    public List<String> check(
            CallFunctionNode call, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        List<String> violations = new ArrayList<>();
        String functionName = call.identifierNode().name();

        boolean checkPrintln =
                "println".equalsIgnoreCase(functionName)
                        && context.mandatoryLiteralOrIdentifierInPrintln();
        boolean checkReadInput =
                "readInput".equalsIgnoreCase(functionName)
                        && context.mandatoryLiteralOrIdentifierInReadInput();

        if (checkPrintln || checkReadInput) {
            List<ExpressionNode> args = call.argumentNodes();
            for (int i = 0; i < args.size(); i++) {
                ExpressionNode arg = args.get(i);
                if (!(arg instanceof LiteralNode) && !(arg instanceof IdentifierNode)) {
                    violations.add(
                            String.format(
                                    "Function '%s' argument at index %d must be a literal or"
                                            + " variable, found expression at line %d, column %d",
                                    functionName, i, arg.line(), arg.column()));
                }
            }
        }
        return violations;
    }
}
