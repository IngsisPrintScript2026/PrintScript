/*
 * My Project
 */

package parser.fake;

import node.expression.function.CallFunctionNode;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.handler.SemanticNodeHandler;

/** Semantic handler for calls that rejects one specific function name and accepts the rest. */
public final class StubCallSemanticHandler implements SemanticNodeHandler<CallFunctionNode> {
    private final String rejectedName;
    private final String message;

    public StubCallSemanticHandler(String rejectedName, String message) {
        this.rejectedName = rejectedName;
        this.message = message;
    }

    @Override
    public Class<CallFunctionNode> nodeType() {
        return CallFunctionNode.class;
    }

    @Override
    public Result<SemanticEnvironment> check(CallFunctionNode node, SemanticEnvironment env) {
        if (rejectedName.equals(node.identifierNode().name())) {
            return Result.failure(message);
        }
        return Result.success(env);
    }
}
