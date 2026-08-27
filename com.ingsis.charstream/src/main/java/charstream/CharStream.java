/*
 * My Project
 */

package charstream;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.io.IOException;
import metaChar.MetaCharacter;
import position.Position;
import result.Result;

public final class CharStream implements SafeIterator<MetaCharacter> {
    private final CharReader reader;
    private final PositionTracker positionTracker;

    public CharStream(CharReader reader) {
        this(reader, new PositionTracker());
    }

    private CharStream(CharReader reader, PositionTracker positionTracker) {
        this.reader = reader;
        this.positionTracker = positionTracker;
    }

    @Override
    public void unread(MetaCharacter item) {
        if (item != null && item.character() != null) {
            try {
                reader.unread(item.character());
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public Result<IterationStep<MetaCharacter>> next() {
        try {
            int raw = reader.readNextChar();

            if (raw == -1) {
                return Result.failure("EOF");
            }

            char c = (char) raw;
            Position position =
                    new Position(positionTracker.getLine(), positionTracker.getColumn());
            MetaCharacter metaChar = new MetaCharacter(c, position);

            PositionTracker nextPosition = positionTracker.advance(c);
            CharStream nextStream = new CharStream(reader, nextPosition);

            return Result.success(new IterationStep<>(metaChar, nextStream));

        } catch (IOException e) {
            return Result.failure("I/O Error: " + e.getMessage());
        }
    }
}
