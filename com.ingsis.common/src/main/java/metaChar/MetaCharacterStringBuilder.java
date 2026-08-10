package metaChar;

import java.util.List;

public sealed interface MetaCharacterStringBuilder permits MetaCharStringBuilder {
    MetaCharacterStringBuilder append(MetaCharacter metaCharacter);
    String buildString();
    boolean isEmpty();

}