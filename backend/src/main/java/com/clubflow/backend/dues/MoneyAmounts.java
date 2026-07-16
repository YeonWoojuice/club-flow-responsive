package com.clubflow.backend.dues;

import com.clubflow.backend.common.InvalidRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyAmounts {

    private static final int MAX_PRECISION = 19;

    private MoneyAmounts() {
    }

    public static BigDecimal parsePositive(String value, String fieldName) {
        if (value == null || !value.matches("[0-9]{1,19}")) {
            throw new InvalidRequestException(fieldName + "은 1~19자리의 원 단위 숫자로 입력해 주세요.");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0 || amount.precision() > MAX_PRECISION) {
            throw new InvalidRequestException(fieldName + "은 0원보다 커야 합니다.");
        }
        return amount;
    }

    public static BigDecimal refund(BigDecimal paidAmount, int refundRateBps) {
        if (paidAmount.signum() < 0 || refundRateBps < 0 || refundRateBps > 10_000) {
            throw new InvalidRequestException("환불 계산 값을 확인해 주세요.");
        }
        return paidAmount
                .multiply(BigDecimal.valueOf(refundRateBps))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.FLOOR);
    }

    public static String format(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
    }
}
