/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import position.Position;
import syntactic.parser.ParserFactory;
import syntactic.version.LanguageFeatures;
import syntactic.version.LanguageFeatures.Feature;
import token.Token;
import token.TokenType;
import version.Version;

class LanguageFeaturesUnitTest {

    private static Token token(TokenType type, String value) {
        Position pos = new Position(1, 1);
        return new Token(type, value, pos, pos);
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 | When: asking its features | Then: only let, string, number"
                    + " and println are available")
    void forVersion_whenV10_thenOnlyBasicLanguage() {
        // When
        LanguageFeatures v10 = LanguageFeatures.forVersion(Version.V_1_0);

        // Then
        assertEquals(Version.V_1_0, v10.version());
        assertEquals(Set.of("println"), v10.builtInFunctions());
        assertFalse(v10.supports(Feature.IF_STATEMENT));
        assertFalse(v10.supports(Feature.BOOLEAN_LITERAL));
        assertTrue(v10.declarationKeywordMatcher().test(token(TokenType.LET, "let")));
        assertFalse(v10.declarationKeywordMatcher().test(token(TokenType.CONST, "const")));
        assertTrue(v10.dataTypeMatcher().test(token(TokenType.NUMBER, "number")));
        assertFalse(v10.dataTypeMatcher().test(token(TokenType.BOOLEAN, "boolean")));
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.1 | When: asking its features | Then: const, boolean, if and the"
                    + " read functions are added")
    void forVersion_whenV11_thenAddsConstBooleanIfAndReadFunctions() {
        // When
        LanguageFeatures v11 = LanguageFeatures.forVersion(Version.V_1_1);

        // Then
        assertEquals(Version.V_1_1, v11.version());
        assertEquals(Set.of("println", "readInput", "readEnv"), v11.builtInFunctions());
        assertTrue(v11.supports(Feature.IF_STATEMENT));
        assertTrue(v11.supports(Feature.BOOLEAN_LITERAL));
        assertTrue(v11.declarationKeywordMatcher().test(token(TokenType.CONST, "const")));
        assertTrue(v11.dataTypeMatcher().test(token(TokenType.BOOLEAN, "boolean")));
    }

    @Test
    @DisplayName(
            "Given: no version | When: asking its features | Then: throws"
                    + " IllegalArgumentException")
    void forVersion_whenNull_thenThrows() {
        // When
        Executable lookup = () -> LanguageFeatures.forVersion(null);

        // Then
        assertThrows(IllegalArgumentException.class, lookup);
    }

    @Test
    @DisplayName(
            "Given: PrintScript 1.0 features | When: extending them with if | Then: the new"
                    + " features keep the rest and the original is unchanged")
    void extendedWith_whenAddingFeature_thenKeepsRestAndLeavesOriginalUntouched() {
        // Given
        LanguageFeatures v10 = LanguageFeatures.forVersion(Version.V_1_0);

        // When
        LanguageFeatures extended = v10.extendedWith(Version.V_1_1, Feature.IF_STATEMENT);

        // Then
        assertEquals(Version.V_1_1, extended.version());
        assertTrue(extended.supports(Feature.IF_STATEMENT));
        assertFalse(extended.supports(Feature.BOOLEAN_LITERAL));
        assertEquals(v10.builtInFunctions(), extended.builtInFunctions());
        assertFalse(v10.supports(Feature.IF_STATEMENT));
    }

    @Test
    @DisplayName(
            "Given: every version, and no version | When: creating a parser | Then: the factory"
                    + " always returns one")
    void parserFactory_whenAnyVersion_thenCreatesParser() {
        assertNotNull(ParserFactory.createParser(Version.V_1_0));
        assertNotNull(ParserFactory.createParser(Version.V_1_1));
        assertNotNull(ParserFactory.createParser(null));
    }
}
