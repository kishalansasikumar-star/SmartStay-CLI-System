package smartstay.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

public class Invoice {
    public final long nights;
    public final BigDecimal base, extra, discount, tax, total, balance;

    public Invoice(Reservation booking, DiscountPolicy policy) {
        if (booking.cancelled()) throw new IllegalArgumentException("Booking cancelled. Advance already refunded.");
        Room room = booking.room;
        nights = ChronoUnit.DAYS.between(booking.arrival, booking.departure);
        base = BigDecimal.valueOf(room.dailyRate()).multiply(BigDecimal.valueOf(nights));
        // The room rate already covers guests up to the included count.
        int extraGuests = Math.max(0, booking.guests - room.capacity);
        extra = BigDecimal.valueOf(extraGuests).multiply(BigDecimal.valueOf(room.extraGuestRate()))
                .multiply(BigDecimal.valueOf(nights));
        discount = policy.discount(nights, base);
        // Apply tax to the discounted room charge and the full extra guest fees.
        BigDecimal subtotal = base.subtract(discount).add(extra);
        tax = subtotal.multiply(BigDecimal.valueOf(room.taxPercent(), 2)).setScale(2, RoundingMode.HALF_UP);
        total = subtotal.add(tax);
        balance = total.subtract(BigDecimal.valueOf(Reservation.ADVANCE));
    }
}
