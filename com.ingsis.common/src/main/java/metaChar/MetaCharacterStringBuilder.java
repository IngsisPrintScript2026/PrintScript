/*
 * My Project
 */

package metaChar;

public sealed interface MetaCharacterStringBuilder permits MetaCharStringBuilder {
    MetaCharacterStringBuilder append(MetaCharacter metaCharacter);

    String buildString();

    boolean isEmpty();
}
