package ar.com.rotula.exception;

import java.math.BigDecimal;

public class PercentageSumExceededException extends RuntimeException {
    public PercentageSumExceededException(BigDecimal current, BigDecimal incoming) {
        super(String.format(
                "La suma de porcentajes supera el 100%%. Suma actual: %.3f%%, nuevo valor: %.3f%%",
                current, incoming));
    }
}
