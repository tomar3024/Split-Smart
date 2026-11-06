import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class SplitSmart {

    private record Expense(
            String description,
            String category,
            String payer,
            long amountInPaise,
            Map<String, Long> shares) {
    }

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            String groupName = readRequired(input, "Group name: ");
            List<String> members = readMembers(input);
            List<Expense> expenses = new ArrayList<>();

            while (true) {
                System.out.println("\n" + groupName);
                System.out.println("1. Add an expense");
                System.out.println("2. View balances");
                System.out.println("3. View spending report");
                System.out.println("4. Exit");
                System.out.print("Choose an option: ");

                switch (input.nextLine().strip()) {
                    case "1" -> expenses.add(readExpense(input, members));
                    case "2" -> showBalances(members, expenses);
                    case "3" -> showReport(expenses);
                    case "4" -> {
                        return;
                    }
                    default -> System.out.println("Enter a number from 1 to 4.");
                }
            }
        }
    }

    private static List<String> readMembers(Scanner input) {
        while (true) {
            String line = readRequired(input, "Member names, separated by commas: ");
            Set<String> names = new LinkedHashSet<>();

            for (String entry : line.split(",")) {
                String name = entry.strip();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }

            if (names.size() >= 2) {
                return new ArrayList<>(names);
            }
            System.out.println("Enter at least two distinct member names.");
        }
    }

    private static Expense readExpense(Scanner input, List<String> members) {
        String description = readRequired(input, "Description: ");
        String category = readRequired(input, "Category: ");
        long amount = readAmount(input);

        for (int i = 0; i < members.size(); i++) {
            System.out.println((i + 1) + ". " + members.get(i));
        }

        int payerNumber = readMemberNumbers(input, members.size(), false)
                .iterator().next();
        String payer = members.get(payerNumber - 1);

        Set<Integer> participants = readMemberNumbers(input, members.size(), true);
        long baseShare = amount / participants.size();
        long extraPaise = amount % participants.size();

        Map<String, Long> shares = new LinkedHashMap<>();
        for (int i = 0; i < members.size(); i++) {
            if (participants.contains(i + 1)) {
                long share = baseShare;
                if (extraPaise > 0) {
                    share++;
                    extraPaise--;
                }
                shares.put(members.get(i), share);
            }
        }

        System.out.println("Expense added. Shares:");
        shares.forEach((name, share) ->
                System.out.println("  " + name + "  " + money(share)));

        return new Expense(description, category, payer, amount, shares);
    }

    private static Set<Integer> readMemberNumbers(
            Scanner input, int memberCount, boolean allowMultiple) {
        String prompt = allowMultiple
                ? "Who shares the cost? Enter member numbers separated by commas: "
                : "Who paid? Enter one member number: ";

        while (true) {
            String line = readRequired(input, prompt);
            Set<Integer> numbers = new LinkedHashSet<>();

            try {
                String[] entries = line.split(",", -1);
                if (!allowMultiple && entries.length != 1) {
                    throw new IllegalArgumentException();
                }

                for (String entry : entries) {
                    int number = Integer.parseInt(entry.strip());
                    if (number < 1 || number > memberCount || !numbers.add(number)) {
                        throw new IllegalArgumentException();
                    }
                }
                return numbers;
            } catch (IllegalArgumentException exception) {
                System.out.println("Use valid member numbers from the list, once each.");
            }
        }
    }

    private static long readAmount(Scanner input) {
        while (true) {
            String amount = readRequired(input, "Amount in rupees: ");
            if (amount.matches("[0-9]+(\\.[0-9]{1,2})?")) {
                try {
                    long paise = new BigDecimal(amount).movePointRight(2).longValueExact();
                    if (paise > 0) {
                        return paise;
                    }
                } catch (ArithmeticException exception) {
                    System.out.println("That amount is too large.");
                    continue;
                }
            }
            System.out.println("Enter a positive amount with up to two decimal places.");
        }
    }

    private static void showBalances(List<String> members, List<Expense> expenses) {
        System.out.println("\nGroup balances");

        for (String member : members) {
            long paid = expenses.stream()
                    .filter(expense -> expense.payer().equals(member))
                    .mapToLong(Expense::amountInPaise)
                    .sum();

            long owed = expenses.stream()
                    .mapToLong(expense -> expense.shares().getOrDefault(member, 0L))
                    .sum();

            long balance = paid - owed;
            if (balance > 0) {
                System.out.println(member + " should receive " + money(balance));
            } else if (balance < 0) {
                System.out.println(member + " owes " + money(-balance));
            } else {
                System.out.println(member + " is settled");
            }
        }
    }

    private static void showReport(List<Expense> expenses) {
        Map<String, Long> categoryTotals = new LinkedHashMap<>();

        System.out.println("\nExpense history");
        for (Expense expense : expenses) {
            System.out.println(expense.description() + " | " + expense.category()
                    + " | " + money(expense.amountInPaise())
                    + " | Paid by " + expense.payer());

            categoryTotals.merge(expense.category(), expense.amountInPaise(), Long::sum);
        }

        System.out.println("\nSpending by category");
        categoryTotals.forEach((category, total) ->
                System.out.println(category + "  " + money(total)));

        long total = expenses.stream().mapToLong(Expense::amountInPaise).sum();
        System.out.println("Group total  " + money(total));
    }

    private static String readRequired(Scanner input, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = input.nextLine().strip();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Please enter a value.");
        }
    }

    private static String money(long paise) {
        return "₹" + BigDecimal.valueOf(paise, 2).toPlainString();
    }
}