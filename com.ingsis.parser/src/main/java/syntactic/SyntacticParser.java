package syntactic;

import iterator.IterationStep;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.parser.ParserFactory;
import tokenstream.TokenStream;
import version.printscript.PrintScriptVersion;

import java.util.ArrayList;
import java.util.List;

public final class SyntacticParser implements Parser<ProgramNode> {
    private final Parser<Node> chainParser;
    public SyntacticParser(PrintScriptVersion version) {
        this.chainParser = ParserFactory.createParser(version);
    }

    public SyntacticParser(Parser<Node> chainParser) {
        this.chainParser = chainParser;
    }
    @Override
    public Result<IterationStep<ProgramNode>> parse(TokenStream stream) {
        List<Node> statements = new ArrayList<>();
        TokenStream currentStream = stream;

        while (!currentStream.isEmpty()) {
            switch (chainParser.parse(currentStream)) {
                case CorrectResult<IterationStep<Node>>(IterationStep<Node> step) -> {
                    statements.add(step.value());
                    currentStream = (TokenStream) step.next();
                }
                case IncorrectResult<IterationStep<Node>>(String err) -> {
                    return Result.failure(err);
                }
            }
        }

        ProgramNode programNode = new ProgramNode(statements, 1, 1);
        return Result.success(new IterationStep<>(programNode, currentStream));
    }
}

