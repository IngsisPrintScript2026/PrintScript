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
        String functionName = call.identifierNode().name();
        if (!shouldValidateArguments(functionName, context)) {
            return new ArrayList<>();
        }
        return validateArguments(call.argumentNodes(), functionName);
    }

    private boolean shouldValidateArguments(String functionName, ScaContext context) {
        boolean checkPrintln =
                "println".equalsIgnoreCase(functionName)
                        && context.mandatoryLiteralOrIdentifierInPrintln();
        boolean checkReadInput =
                "readInput".equalsIgnoreCase(functionName)
                        && context.mandatoryLiteralOrIdentifierInReadInput();
        return checkPrintln || checkReadInput;
    }

    private List<String> validateArguments(List<ExpressionNode> args, String functionName) {
        List<String> violations = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            ExpressionNode arg = args.get(i);
            if (!(arg instanceof LiteralNode) && !(arg instanceof IdentifierNode)) {
                violations.add(formatViolation(functionName, i, arg));
            }
        }
        return violations;
    }

    private String formatViolation(String functionName, int index, ExpressionNode arg) {
        return String.format(
                "Function '%s' argument at index %d must be a literal or"
                        + " variable, found expression at line %d, column %d",
                functionName, index, arg.line(), arg.column());
    }
}
