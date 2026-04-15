import java.util.Scanner;

public class PasswordValidator {

    public static void validatePassword(Scanner scan) throws Exception {
        while (true) {
            System.out.println("Enter password:");
            String password = scan.nextLine();

            int spcChar = 0, upperCase = 0, lowerCase = 0, digits = 0;

            if (password.isEmpty()) {
                System.out.println("You need to enter a password!");
                continue;
            }

            for (char c : password.toCharArray()) {
                if (Character.isUpperCase(c)) upperCase++;
                else if (Character.isLowerCase(c)) lowerCase++;
                else if (Character.isDigit(c)) digits++;
                else spcChar++;
            }

            if (password.length() < 8) {
                System.out.println("- Password needs a minimum of 8 characters!");
                continue;
            }
            if (upperCase == 0) {
                System.out.println("- Password needs a minimum of 1 uppercase character!");
                continue;
            }
            if (lowerCase == 0) {
                System.out.println("- Password needs a minimum of 1 lowercase character!");
                continue;
            }
            if (spcChar == 0 || digits == 0) {
                System.out.println("- Password needs at least 1 special character or digit!");
                continue;
            }

            String strength = (password.length() >= 12) ? "strong" : "weak";
            System.out.println("- Password is valid and " + strength + "!");
            savePassword(scan, password, UserAction.VALIDATE);
            break;
        }
    }


    public static void generatePassword(Scanner scan) throws Exception {

        System.out.println("Enter password length:");
        int passwordLength = scan.nextInt();
        scan.nextLine();

        while (passwordLength < 8) {
            System.out.println("You need to choose at least 8 characters!");
            passwordLength = scan.nextInt();
            scan.nextLine();
        }

        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String spc = "!#¤%&/()=?`^@£$€{[]}+-_.:,;µ";

        StringBuilder password = new StringBuilder();

        String AllChar = upperCase + lowerCase + digits + spc;

        for (int i = password.length(); i < passwordLength; i++) {
            password.append(AllChar.charAt((int) (AllChar.length() * Math.random())));
        }
        savePassword(scan, String.valueOf(password), UserAction.GENERATE);
    }

    public static void savePassword(Scanner scan, String password, UserAction action) throws Exception {
        System.out.println("Your password is: " + password);

        while (true) {
            System.out.println("Do you want to save your password, try again, or return to menu? (save/try again/menu)");
            UserAction choice = UserAction.fromInput(scan.nextLine());

            if (choice == null) {
                System.out.println("Invalid input, please type 'save', 'try again', or 'menu'.");
                continue;
            }

            switch (choice) {
                case SAVE -> {
                    PasswordDB.ValGenSavePassword(scan, password);
                    return;
                }
                case TRY_AGAIN -> {
                    if (action == UserAction.VALIDATE) validatePassword(scan);
                    else if (action == UserAction.GENERATE) generatePassword(scan);
                    return;
                }
                case MENU -> {
                    Program.menu();
                    return;
                }
                default -> System.out.println("Invalid input, please try again.");
            }
        }
    }
}



