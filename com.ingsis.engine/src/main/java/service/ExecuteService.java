package service;

import charstream.CharStream;
import charstream.StreamCharReader;
import engine.InputSupplier;
import engine.OutputEmitter;
import environment.Environment;
import interpreter.DefaultInterpreter;
import interpreter.Interpreter;
import iterator.IterationStep;
import iterator.SafeIterator;
import lexer.Lexer;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import syntactic.SyntacticParser;
import token.Token;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExecuteService {

    public Result<String> execute(
            Version version, OutputEmitter emitter, InputSupplier supplier, InputStream in) {
        Result<SemanticEnvironment> res = execute(version, emitter, supplier, in, new SemanticEnvironment(), new Environment());
        if (res.isCorrect()) {
            return new CorrectResult<>("Program executed successfully");
        }
        return new IncorrectResult<>(((IncorrectResult<SemanticEnvironment>) res).error());
    }

    public Result<SemanticEnvironment> execute(
            Version version,
            OutputEmitter emitter,
            InputSupplier supplier,
            InputStream in,
            SemanticEnvironment semanticEnv,
            Environment runtimeEnv) {
        try {
            StreamCharReader reader = new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            CharStream charStream = new CharStream(reader);
            SafeIterator<Token> lexer = new Lexer(charStream);
            TokenStream currentStream = new LazyTokenStream(lexer);

            syntactic.Parser<Node> statementParser = syntactic.parser.ParserFactory.createParser(version);
            SemanticChecker semanticChecker = new SemanticChecker(statementParser);

            Interpreter interpreter = new DefaultInterpreter(
                    semanticChecker,
                    msg -> {
                        if (emitter != null) {
                            emitter.emit(msg);
                        }
                    });

            return interpreter.interpret(currentStream, semanticEnv, runtimeEnv);
        } catch (Exception e) {
            return new IncorrectResult<>("Execution error: " + e.getMessage());
        }
    }
}