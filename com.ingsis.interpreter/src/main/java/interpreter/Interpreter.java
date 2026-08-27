/*
 * My Project
 */

package interpreter;

import environment.Environment;
import node.ProgramNode;
import result.Result;
import semantic.environment.SemanticEnvironment;
import tokenstream.TokenStream;

public interface Interpreter {
    Result<SemanticEnvironment> interpret(
            TokenStream tokenStream, SemanticEnvironment semanticEnv, Environment runtimeEnv);

    Result<Void> interpret(ProgramNode program);

    Result<Void> interpret(ProgramNode program, Environment globalEnv);
}
