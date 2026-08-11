package node.expression.operator;

import result.CorrectResult;
import result.IncorrectResult;
import result.Result;

public enum OperatorType {
    ASSIGNATION("=", 2, 1),
    PLUS("+", 12, 11),
    MINUS("-", 12, 11),
    STAR("*", 22, 21),
    SLASH("/", 22, 21);

    private final String symbol;
    private final int lBindingPower;
    private final int rBindingPower;

    OperatorType(String symbol, int lBindingPower, int rBindingPower) {
        this.symbol = symbol;
        this.lBindingPower = lBindingPower;
        this.rBindingPower = rBindingPower;
    }

    public String symbol() {
        return symbol;
    }

    public int lBindingPower() {
        return lBindingPower;
    }

    public int rBindingPower() {
        return rBindingPower;
    }

    public static boolean isOperator(String symbol) {
        for (OperatorType type : values()) {
            if (type.symbol.equals(symbol)) {
                return true;
            }
        }
        return false;
    }

    public static Result<OperatorType> fromSymbol(String symbol) {
        for (OperatorType type : values()) {
            if (type.symbol.equals(symbol)) {
                return new CorrectResult<>(type);
            }
        }
        return new IncorrectResult<>("No operator type for symbol: " + symbol);
    }
}
