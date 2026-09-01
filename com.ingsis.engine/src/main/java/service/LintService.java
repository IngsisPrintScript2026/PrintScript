/*
 * My Project
 */

package service;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import iterator.SafeIterator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lexer.Lexer;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import sca.ASTSca;
import sca.Sca;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.Token;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

public class LintService {

    public Result<String> analyze(Version version, InputStream in, InputStream config) {
        Sca scaAnalyzer = (config != null) ? ASTSca.fromYamlConfig(config) : new ASTSca();
        return analyzeWithSca(version, in, scaAnalyzer);
    }

    public Result<String> analyze(Version version, InputStream in) {
        return analyzeWithSca(version, in, new ASTSca());
    }

    public Result<String> analyzeWithSca(Version version, InputStream in, Sca scaAnalyzer) {
        try {
            StreamCharReader reader =
                    new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            CharStream charStream = new CharStream(reader);
            SafeIterator<Token> lexer = new Lexer(charStream);
            TokenStream currentStream = new LazyTokenStream(lexer);

            syntactic.Parser<Node> statementParser =
                    syntactic.parser.ParserFactory.createParser(version);
            SemanticChecker semanticChecker = new SemanticChecker();

            List<Node> statements = new ArrayList<>();
            SemanticEnvironment currentSemEnv = new SemanticEnvironment();

            while (!currentStream.isEmpty()) {
                Result<IterationStep<Node>> parseResult = statementParser.parse(currentStream);
                if (!parseResult.isCorrect()) {
                    String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
                    if ("EOF".equalsIgnoreCase(err) || err.contains("EOF")) {
                        break;
                    }
                    return Result.failure(
                            err.startsWith("Syntactic error:") ? err : "Syntactic error: " + err);
                }

                IterationStep<Node> step =
                        ((CorrectResult<IterationStep<Node>>) parseResult).value();
                Node statement = step.value();
                currentStream = (TokenStream) step.next();

                Result<SemanticEnvironment> semResult =
                        semanticChecker.checkNode(statement, currentSemEnv);
                if (!semResult.isCorrect()) {
                    String err = ((IncorrectResult<SemanticEnvironment>) semResult).error();
                    return Result.failure(
                            err.startsWith("Semantic error:") ? err : "Semantic error: " + err);
                }
                currentSemEnv = ((CorrectResult<SemanticEnvironment>) semResult).value();
                statements.add(statement);
            }

            ProgramNode programNode = new ProgramNode(statements, 1, 1);
            Result<List<String>> scaResult = scaAnalyzer.analyze(programNode, currentSemEnv);

            if (scaResult.isCorrect()) {
                List<String> violations = ((CorrectResult<List<String>>) scaResult).value();
                if (violations == null || violations.isEmpty()) {
                    return new CorrectResult<>("SCA analysis passed with 0 violations");
                }
                return new IncorrectResult<>(String.join("\n", violations));
            }
            return new IncorrectResult<>(((IncorrectResult<List<String>>) scaResult).error());

        } catch (Exception e) {
            return Result.failure("Analysis error: " + e.getMessage());
        }
    }
}
