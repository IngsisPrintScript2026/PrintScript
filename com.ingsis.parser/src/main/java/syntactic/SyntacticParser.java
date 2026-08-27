/*
 * My Project
 */

package syntactic;

import iterator.IterationStep;
import java.util.ArrayList;
import java.util.List;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.parser.ParserFactory;
import tokenstream.TokenStream;
import version.Version;

public final class SyntacticParser implements Parser<ProgramNode> {
    private final Parser<Node> chainParser;

    public SyntacticParser(Version version) {
        this.chainParser = ParserFactory.createParser(version);
    }

    public SyntacticParser(Parser<Node> chainParser) {
        this.chainParser = chainParser;
    }

    public Result<IterationStep<Node>> parseStatement(TokenStream stream) {
        if (stream == null || stream.isEmpty()) {
            return Result.failure("EOF");
        }
        return chainParser.parse(stream);
    }

    public Result<ProgramNode> parseProgram(TokenStream stream) {
        List<Node> statements = new ArrayList<>();
        TokenStream currentStream = stream;

        while (!currentStream.isEmpty()) {
            switch (parseStatement(currentStream)) {
                case CorrectResult<IterationStep<Node>>(IterationStep<Node> step) -> {
                    statements.add(step.value());
                    currentStream = (TokenStream) step.next();
                }
                case IncorrectResult<IterationStep<Node>>(String err) -> {
                    return Result.failure(err);
                }
            }
        }

        return Result.success(new ProgramNode(statements, 1, 1));
    }

    @Override
    public Result<IterationStep<ProgramNode>> parse(TokenStream stream) {
        return switch (parseProgram(stream)) {
            case CorrectResult<ProgramNode>(ProgramNode programNode) ->
                    Result.success(new IterationStep<>(programNode, stream));
            case IncorrectResult<ProgramNode>(String err) -> Result.failure(err);
        };
    }
}
