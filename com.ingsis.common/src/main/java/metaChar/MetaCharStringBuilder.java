package metaChar;

import position.Position;

import java.util.List;

public final class MetaCharStringBuilder implements MetaCharacterStringBuilder{
    private final StringBuilder builder = new StringBuilder();
    //Se utilizan valores negativos para representar que esta inicializado vacio y para no utilizar null como Position
    private Position position = new Position(-1,-1);

    @Override
    public MetaCharacterStringBuilder append(MetaCharacter metaCharacter) {
        if (builder.isEmpty()) {
            this.position = metaCharacter.position();
        }
        this.builder.append(metaCharacter.character());
        return this;
    }

    public String buildString() {
        return this.builder.toString();
    }
    public Position getStartPosition() {
        return this.position;
    }

    public boolean isEmpty() {
        return builder.isEmpty();
    }

}
