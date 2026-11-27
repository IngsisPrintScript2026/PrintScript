/*
 * My Project
 */

package sca.handler;

import java.util.ArrayList;
import java.util.List;
import node.CallFunctionNode;
import node.ExpressionNode;
import node.IdentifierNode;
import node.LiteralNode;
import node.NilExpressionNode;
import node.OperatorNode;
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
            if (!isLiteralOrIdentifier(arg)) {
                violations.add(formatViolation(functionName, i, arg));
            }
        }
        return violations;
    }

    private static boolean isLiteralOrIdentifier(ExpressionNode arg) {
        return switch (arg) {
            case LiteralNode<?> literal -> true;
            case IdentifierNode identifier -> true;
            case OperatorNode operator -> false;
            case CallFunctionNode call -> false;
            case NilExpressionNode nil -> false;
        };
    }

    private String formatViolation(String functionName, int index, ExpressionNode arg) {
        return String.format(
                "Function '%s' argument at index %d must be a literal or"
                        + " variable, found expression %s",
                functionName, index, arg.range());
    }
}
