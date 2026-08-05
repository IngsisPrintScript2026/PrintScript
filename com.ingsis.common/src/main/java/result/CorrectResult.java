package result;

public record CorrectResult<T>(T value) implements Result<T> {
    @Override
    public boolean isCorrect() {
        return true;
    }
}