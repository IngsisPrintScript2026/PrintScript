/*
 * My Project
 */

package interpreter.fake;

import node.Node;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;

/** Semantic checker that accepts every statement, or rejects all of them with a fixed error. */
public final class StubSemanticChecker extends SemanticChecker {
    private final String error;

    private StubSemanticChecker(String error) {
        this.error = error;
    }

    public static StubSemanticChecker accepting() {
        return new StubSemanticChecker(null);
    }

    public static StubSemanticChecker rejectingWith(String error) {
        return new StubSemanticChecker(error);
    }

    @Override
    public Result<SemanticEnvironment> checkNode(Node node, SemanticEnvironment env) {
        return error == null ? Result.success(env) : Result.failure(error);
    }
}
