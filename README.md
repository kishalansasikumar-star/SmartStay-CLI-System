# SmartStay Hotel Booking System

Java CLI coursework project for 2703142. Use one running instance at a time. Requires a JDK (11 or newer). The program uses Java SE and saves records locally after each successful change.

## Run on Windows

Open PowerShell in the extracted SmartStay folder. On the development machine run:

```powershell
.\run.ps1 -Jdk 'C:\Program Files\Java\jdk-25'
```

On another machine, use its JDK folder or run `.\run.ps1` with JAVA_HOME or the JDK on PATH. If local PowerShell policy blocks the script, use the IDE route: open the project, mark src as the source root, set the working directory to the project folder and launch smartstay.ui.Main_2703142.

## First booking

1. Select Administrator, then Add room. Try R101, Standard, Double, capacity 2, Available.
2. Go back and choose Guest, then Register. Use fictional guest details for demonstrations.
3. Choose Book. Enter the registered NIC/passport, room type and dates at least two calendar days ahead. Choose an available room ID, enter guest count and confirm the LKR 10,000 advance.
4. Use the displayed booking ID to view the invoice, modify the stay or cancel. Find by name accepts a full name without case sensitivity. The check-in menu supports a single date or all reservations in chronological order.
5. Reopen the application to use the saved rooms, guests and bookings. The first launch starts empty. Records are stored in data/hotel.dat. Role menus assume a trusted local operator.

## Rules and assumptions

| Topic | Behaviour |
|---|---|
| Capacity | The administrator enters the number of guests included in each room rate. The brief supplies no capacity figures. |
| Extra guests | Charge each person above capacity for every night of the stay, using the package's per-guest price. |
| Availability | Manual Reserved and Under Maintenance block bookings until cleared. For an otherwise Available room, overlapping active stays make it Reserved for the requested dates. Checkout day can be used for the next arrival. The admin list displays today's status. |
| Room changes | Current or future confirmed stays block edits, deletion and manual status changes. Historical reservations retain their booked room details. |
| Dates | Use Sri Lankan calendar dates for two days' notice, and the entered check-in time (HH:mm, Sri Lanka time) for the 24-hour rule. Changes exactly 24 hours ahead are allowed. Keeping the original arrival date on an edit retains the original notice. Older saved bookings use 14:00 because they have no recorded check-in time. |
| Billing | Discount only the base room charge by 12% at five nights or more. Apply package tax to discounted base plus extras, round tax to two decimals, then deduct the paid advance. |
| Advance | Confirmation records receipt of LKR 10,000. An edit retains it. Eligible cancellation records a full refund once and releases the dates. When the bill is smaller than the advance the invoice shows zero payable and a refund due. |

## Design and implementation

The model package contains Guest, Room, Reservation and Invoice. Java class names end with the student ID, for example Hotel_2703142. Each reservation references exactly one guest and one room. A guest or room can have many reservations across time. Private status fields and booking operations control state changes. The four room subclasses inherit common room data and override package methods. Billing calls those methods through the Room type and uses the DiscountPolicy interface for the long-stay rule.

The core package holds booking rules and local file storage. LinkedHashMap gives direct ID lookups and predictable listing order. Available-room search checks type and looks for overlapping intervals. Guest-name search scans bookings and returns every match. Check-in viewing gathers matches in an ArrayList and sorts by arrival. The ui package reads menu choices, displays validation feedback and prints invoices.

## Verification

Run the repeatable checks with:

```powershell
.\run.ps1 -Jdk 'C:\Program Files\Java\jdk-25' -Test
```

The test runner uses a fixed hotel clock and temporary files. Its 45 checks cover guest validation, packages, room management, availability, reservations, searches, date boundaries, discounts, refunds and saving/reloading. Each check prints its result. A separate CLI walkthrough was run with a Standard room, capacity 2, three guests and five nights: base 30,000, extra 5,000, discount 3,600, tax 3,140 and final payable 24,540 after advance. Booking, invoice, modification and cancellation were exercised through the menus.
