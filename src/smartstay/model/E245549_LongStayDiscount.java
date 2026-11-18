package smartstay.model;

import java.math.BigDecimal;

public class E245549_LongStayDiscount implements E245549_DiscountPolicy {
    public BigDecimal discount(long nights, BigDecimal base) {
        if (nights >= 5) return base.multiply(new BigDecimal("0.12"));
        return BigDecimal.ZERO;
    }
}
