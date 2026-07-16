package com.clubflow.backend.dues;

import com.clubflow.backend.common.InvalidRequestException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class MoneyAmountsTest {
    @Test void 환불액의_원_미만은_버린다() {
        assertThat(MoneyAmounts.refund(new BigDecimal("30001"), 5000))
                .isEqualByComparingTo("15000");
    }

    @Test void 금액은_19자리_양의_정수만_허용한다() {
        assertThat(MoneyAmounts.parsePositive("9999999999999999999", "회비"))
                .isEqualByComparingTo("9999999999999999999");
        assertThatThrownBy(() -> MoneyAmounts.parsePositive("10000000000000000000", "회비"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> MoneyAmounts.parsePositive("1e10", "회비"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> MoneyAmounts.parsePositive("0", "회비"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
