package smartstay.model;

import java.math.BigDecimal;

public interface E245549_DiscountPolicy {
    BigDecimal discount(long nights, BigDecimal base);
}
