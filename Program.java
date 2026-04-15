import java.util.Scanner;

public class Program {

    public static void run() throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("Choose an option: 1 or 2");
        System.out.println("1: Log into your password manager account");
        System.out.println("2: Create a password manager account");

        String userInput = scan.nextLine();
        UserAction action = UserAction.fromInput(userInput);

        if (action == null) {
            System.out.println("Invalid input! Choose a valid option.");
            return;
        }

        switch (action) {
            case LOGIN -> PasswordDB.logIntoUser(scan);
            case CREATE -> PasswordDB.creatUser(scan);
            default -> System.out.println("Invalid option for this step.");
        }
    }

    public static void menu() throws Exception {
        Scanner scan = new Scanner(System.in);

        while (true) {
            printMenuOptions();
            UserAction action = UserAction.fromInput(scan.nextLine());

            if (action == null) {
                System.out.println("Invalid input! Choose a valid option.");
                continue;
            }

            switch (action) {
                case VALIDATE -> PasswordValidator.validatePassword(scan);
                case GENERATE -> PasswordValidator.generatePassword(scan);
                case SAVE -> PasswordDB.inputAndSavePassword(scan);
                case RETRIEVE -> PasswordDB.getMyPassword(scan);
                case CHANGE -> PasswordDB.changePassword(scan);
                case DELETE -> PasswordDB.deleteMyPassword(scan);
                case PWNED -> HaveIBeenPwn.checkPasswordPwned(scan);
                case QUIT -> {
                    System.out.println("Bye!");
                    return;
                }
                default -> System.out.println("Invalid menu option.");
            }


            System.out.println("_____________________");
            System.out.println("Do you want to return to menu or quit? (menu/quit)");
            UserAction next = UserAction.fromInput(scan.nextLine());

            if (next == UserAction.MENU) {
                continue;
            } else if (next == UserAction.QUIT) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid input! Returning to menu.");
            }
        }
    }

    private static void printMenuOptions() {
        System.out.println("_____________________");
        System.out.println("Choose an option:");
        System.out.println("- Validate");
        System.out.println("- Generate");
        System.out.println("- Save");
        System.out.println("- Retrieve");
        System.out.println("- Change");
        System.out.println("- Delete");
        System.out.println("- Check if password is pwned");
        System.out.println("- Quit");
        System.out.println("_____________________");
        System.out.print("Enter your choice: ");
    }
}
