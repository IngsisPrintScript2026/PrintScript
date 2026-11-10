/*
 * My Project
 */

package syntactic.version;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import token.Token;
import token.TokenType;
import tokenstream.rules.TokenMatchers;
import version.Version;

public record LanguageFeatures(
        Version version,
        Set<TokenType> declarationKeywords,
        Set<TokenType> dataTypes,
        Set<String> builtInFunctions,
        Set<Feature> features) {

    public enum Feature {
        BOOLEAN_LITERAL,
        IF_STATEMENT,
        WHILE_STATEMENT
    }

    public LanguageFeatures {
        declarationKeywords = Set.copyOf(declarationKeywords);
        dataTypes = Set.copyOf(dataTypes);
        builtInFunctions = Set.copyOf(builtInFunctions);
        features = Set.copyOf(features);
    }

    public static LanguageFeatures forVersion(Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        return switch (version) {
            case V_1_0 -> v10();
            case V_1_1 -> v11();
            case V_1_2 -> v11().extendedWith(Version.V_1_2, Feature.WHILE_STATEMENT);
        };
    }

    private static LanguageFeatures v10() {
        return new LanguageFeatures(
                Version.V_1_0,
                Set.of(TokenType.LET),
                Set.of(TokenType.STRING, TokenType.NUMBER),
                Set.of("println"),
                Set.of());
    }

    private static LanguageFeatures v11() {
        return new LanguageFeatures(
                Version.V_1_1,
                Set.of(TokenType.LET, TokenType.CONST),
                Set.of(TokenType.STRING, TokenType.NUMBER, TokenType.BOOLEAN),
                Set.of("println", "readInput", "readEnv"),
                Set.of(Feature.BOOLEAN_LITERAL, Feature.IF_STATEMENT));
    }

    public LanguageFeatures extendedWith(Version newVersion, Feature... extra) {
        Set<Feature> all = new HashSet<>(features);
        all.addAll(List.of(extra));
        return new LanguageFeatures(
                newVersion, declarationKeywords, dataTypes, builtInFunctions, all);
    }

    public boolean supports(Feature feature) {
        return features.contains(feature);
    }

    public Predicate<Token> declarationKeywordMatcher() {
        return TokenMatchers.isOneOf(declarationKeywords.toArray(TokenType[]::new));
    }

    public Predicate<Token> dataTypeMatcher() {
        return TokenMatchers.isOneOf(dataTypes.toArray(TokenType[]::new));
    }
}
