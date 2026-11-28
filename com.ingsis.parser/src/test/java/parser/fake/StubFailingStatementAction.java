/*
 * My Project
 */

package parser.fake;

import node.StatementNode;
import pipeline.StatementAction;
import result.Result;
import semantic.environment.SemanticEnvironment;

public final class StubFailingStatementAction implements StatementAction {
    private final String message;

    public StubFailingStatementAction(String message) {
        this.message = message;
    }

    @Override
    public Result<Void> apply(StatementNode statement, SemanticEnvironment env) {
        return Result.failure(message);
    }
}
