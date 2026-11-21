package smartstay.model;

import java.math.BigDecimal;

public interface DiscountPolicy {
    BigDecimal discount(long nights, BigDecimal base);
}
