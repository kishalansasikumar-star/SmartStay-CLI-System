package smartstay.model;

import java.math.BigDecimal;

public class LongStayDiscount_2703142 implements DiscountPolicy_2703142 {
    public BigDecimal discount(long nights, BigDecimal base) {
        if (nights >= 5) return base.multiply(new BigDecimal("0.12"));
        return BigDecimal.ZERO;
    }
}
