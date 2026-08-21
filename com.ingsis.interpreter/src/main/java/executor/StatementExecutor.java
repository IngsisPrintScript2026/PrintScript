package executor;

import environment.Environment;
import node.Node;
import result.Result;

public interface StatementExecutor {
    Result<Void> execute(Node statement, Environment env);
}
