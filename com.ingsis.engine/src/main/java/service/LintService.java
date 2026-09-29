/*
 * My Project
 */

package service;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
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
            CharStream charStream =
                    new CharStream(
                            new StreamCharReader(
                                    new InputStreamReader(in, StandardCharsets.UTF_8)));
            TokenStream stream = new LazyTokenStream(new Lexer(charStream));
            syntactic.Parser<Node> parser = syntactic.parser.ParserFactory.createParser(version);
            Result<ParsedProgram> parseRes =
                    parseAndCheckStatements(parser, new SemanticChecker(), stream);
            if (!parseRes.isCorrect()) {
                return Result.failure(((IncorrectResult<ParsedProgram>) parseRes).error());
            }
            ParsedProgram prog = ((CorrectResult<ParsedProgram>) parseRes).value();
            ProgramNode programNode = new ProgramNode(prog.statements(), 1, 1);
            return formatScaResult(scaAnalyzer.analyze(programNode, prog.semEnv()));
        } catch (Exception e) {
            return Result.failure("Analysis error: " + e.getMessage());
        }
    }

    private Result<ParsedProgram> parseAndCheckStatements(
            syntactic.Parser<Node> parser, SemanticChecker checker, TokenStream stream) {
        List<Node> statements = new ArrayList<>();
        SemanticEnvironment semEnv = new SemanticEnvironment();
        TokenStream currentStream = stream;

        while (!currentStream.isEmpty()) {
            Result<IterationStep<Node>> parseResult = parser.parse(currentStream);
            if (!parseResult.isCorrect()) {
                return handleParseError(parseResult, statements, semEnv);
            }
            IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parseResult).value();
            currentStream = (TokenStream) step.next();
            Result<SemanticEnvironment> semRes = checkStatement(checker, step.value(), semEnv);
            if (!semRes.isCorrect()) {
                return Result.failure(((IncorrectResult<SemanticEnvironment>) semRes).error());
            }
            semEnv = ((CorrectResult<SemanticEnvironment>) semRes).value();
            statements.add(step.value());
        }
        return new CorrectResult<>(new ParsedProgram(statements, semEnv));
    }

    private Result<ParsedProgram> handleParseError(
            Result<IterationStep<Node>> parseResult,
            List<Node> statements,
            SemanticEnvironment semEnv) {
        String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
        if ("EOF".equalsIgnoreCase(err) || err.contains("EOF")) {
            return new CorrectResult<>(new ParsedProgram(statements, semEnv));
        }
        return Result.failure(err.startsWith("Syntactic error:") ? err : "Syntactic error: " + err);
    }

    private Result<SemanticEnvironment> checkStatement(
            SemanticChecker checker, Node node, SemanticEnvironment env) {
        Result<SemanticEnvironment> res = checker.checkNode(node, env);
        if (!res.isCorrect()) {
            String err = ((IncorrectResult<SemanticEnvironment>) res).error();
            return Result.failure(
                    err.startsWith("Semantic error:") ? err : "Semantic error: " + err);
        }
        return res;
    }

    private Result<String> formatScaResult(Result<List<String>> scaResult) {
        if (!scaResult.isCorrect()) {
            return new IncorrectResult<>(((IncorrectResult<List<String>>) scaResult).error());
        }
        List<String> violations = ((CorrectResult<List<String>>) scaResult).value();
        if (violations == null || violations.isEmpty()) {
            return new CorrectResult<>("SCA analysis passed with 0 violations");
        }
        return new IncorrectResult<>(String.join("\n", violations));
    }

    private record ParsedProgram(List<Node> statements, SemanticEnvironment semEnv) {}
}
