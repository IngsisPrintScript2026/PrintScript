/*
 * My Project
 */

package executor;

import environment.Environment;
import node.StatementNode;
import result.Result;

public interface StatementExecutor {
    Result<Void> execute(StatementNode statement, Environment env);
}
