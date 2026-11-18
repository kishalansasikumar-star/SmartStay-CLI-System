package smartstay.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

public class E245549_Invoice {
    public final long nights;
    public final BigDecimal base, extra, discount, tax, total, balance;

    public E245549_Invoice(E245549_Reservation booking, E245549_DiscountPolicy policy) {
        if (booking.cancelled()) throw new IllegalArgumentException("Booking cancelled. Advance already refunded.");
        E245549_Room room = booking.room;
        nights = ChronoUnit.DAYS.between(booking.arrival, booking.departure);
        base = BigDecimal.valueOf(room.dailyRate()).multiply(BigDecimal.valueOf(nights));
        int extraGuests = Math.max(0, booking.guests - room.capacity);
        extra = BigDecimal.valueOf(extraGuests).multiply(BigDecimal.valueOf(room.extraGuestRate()));
        discount = policy.discount(nights, base);
        BigDecimal subtotal = base.subtract(discount).add(extra);
        tax = subtotal.multiply(BigDecimal.valueOf(room.taxPercent(), 2)).setScale(2, RoundingMode.HALF_UP);
        total = subtotal.add(tax);
        balance = total.subtract(BigDecimal.valueOf(E245549_Reservation.ADVANCE));
    }
}
