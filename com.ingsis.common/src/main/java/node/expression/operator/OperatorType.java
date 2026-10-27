/*
 * My Project
 */

package node.expression.operator;

import result.CorrectResult;
import result.IncorrectResult;
import result.Result;

public enum OperatorType {
    ASSIGNATION("=", 2, 1),
    // left binding power < right binding power makes the operator left-associative:
    // 10 - 4 - 3 is (10 - 4) - 3
    PLUS("+", 11, 12),
    MINUS("-", 11, 12),
    STAR("*", 21, 22),
    SLASH("/", 21, 22);

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
