import org.mindrot.jbcrypt.BCrypt;
import java.io.Console;
import java.sql.*;
import java.util.Scanner;

public class PasswordDB {
    private static final String url;
    private static final String user;
    private static final String sqlPassword;
    private static String loggedInUsername = null;
    private static int loggedInUserId = -1;

    static {
        user = PropertiesProvider.PROPS.getProperty("host");
        String dbName = PropertiesProvider.PROPS.getProperty("db_name");
        String port = PropertiesProvider.PROPS.getProperty("port");
        sqlPassword = PropertiesProvider.PROPS.getProperty("pwd");
        url = "jdbc:mysql://localhost:" + port + "/" + dbName;
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, sqlPassword);
    }

    public static void logIntoUser(Scanner scan) {
        System.out.print("Enter your username: ");
        String username = scan.nextLine().toLowerCase();

        String password = readPassword(scan, "Enter your password: ");

        String sql = "SELECT id, hashed_password, totp_secret, username FROM users";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String encryptedUsername = rs.getString("username");
                String decryptedUsername = Cryptography.decrypt(encryptedUsername);

                if (decryptedUsername.equals(username)) {
                    String hashedPasswordFromDB = rs.getString("hashed_password");
                    String totpSecret = rs.getString("totp_secret");

                    if (BCrypt.checkpw(password, hashedPasswordFromDB)) {
                        if (totpSecret != null && !totpSecret.isEmpty()) {
                            String decryptedSecret = Cryptography.decrypt(totpSecret);
                            System.out.print("Enter 2FA code: ");
                            String code = scan.nextLine().trim();
                            if (!TwoFa.validateCode(decryptedSecret, code)) {
                                System.out.println("Invalid 2FA code. Access denied.");
                                return;
                            }
                        }

                        loggedInUsername = username;
                        loggedInUserId = rs.getInt("id");
                        System.out.println("Login successful");
                        Program.menu();
                        return;
                    }
                }
            }
            System.out.println("Wrong username or password");
        } catch (Exception e) {
            System.err.println("Login error:");
            e.printStackTrace();
        }
    }

    public static void creatUser(Scanner scan) throws Exception {
        System.out.print("Enter your username: ");
        String username = scan.nextLine().toLowerCase();

        String password;
        while (true) {
            String pass1 = readPassword(scan, "Enter your password: ");
            String pass2 = readPassword(scan, "Confirm your password: ");
            if (!pass1.equals(pass2)) {
                System.out.println("Passwords do not match. Please try again.");
            } else {
                password = pass1;
                break;
            }
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        String secret = TwoFa.generateSecret();
        String encryptedSecret = Cryptography.encrypt(secret);
        String encryptedUsername = Cryptography.encrypt(username);

        String sql = "INSERT INTO users (username, hashed_password, totp_secret) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, encryptedUsername);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, encryptedSecret);
            stmt.executeUpdate();
            TwoFa.generateQRCode(username, secret);
        } catch (SQLException e) {
            System.err.println("Error creating user: " + username);
            e.printStackTrace();
        }

        System.out.println("_____________________");
        System.out.println("Log into your password manager");
        logIntoUser(scan);
    }

    public static void inputAndSavePassword(Scanner scan) throws Exception {
        System.out.print("Enter application/service name: ");
        String service = scan.nextLine().toLowerCase();

        System.out.print("Enter username for this service: ");
        String serviceUsername = scan.nextLine();

        System.out.print("Enter password: ");
        String password = scan.nextLine();

        System.out.print("Enter optional notes: ");
        String notes = scan.nextLine();

        String encryptedPassword = Cryptography.encrypt(password);
        savePasswordToDB(service, serviceUsername, encryptedPassword, notes);
    }

    public static void ValGenSavePassword(Scanner scan, String password) throws Exception {
        System.out.print("Enter application/service name: ");
        String service = scan.nextLine().toLowerCase();

        System.out.print("Enter username for this service: ");
        String serviceUsername = scan.nextLine();

        System.out.print("Enter optional notes: ");
        String notes = scan.nextLine();

        String encryptedPassword = Cryptography.encrypt(password);
        savePasswordToDB(service, serviceUsername, encryptedPassword, notes);
    }

    private static void savePasswordToDB(String service, String serviceUsername, String password, String notes) {
        String sql = "INSERT INTO vault_items (user_id, service_name, service_username, encrypted_password, notes) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, loggedInUserId);
            stmt.setString(2, Cryptography.encrypt(service.trim().toLowerCase()));
            stmt.setString(3, Cryptography.encrypt(serviceUsername.trim().toLowerCase()));
            stmt.setString(4, password);
            stmt.setString(5, Cryptography.encrypt(notes));
            stmt.executeUpdate();

            System.out.println("Password saved for service: " + service);
        } catch (SQLException e) {
            System.err.println("Error saving password");
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void getMyPassword(Scanner scan) {
        String password = readPassword(scan, "Enter your password to confirm identity: ");

        String sql = "SELECT hashed_password FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, loggedInUserId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("hashed_password");

                if (BCrypt.checkpw(password, storedPassword)) {
                    System.out.println("Do you want to get 'one' password or 'all' your passwords:");
                    String userInput = scan.nextLine().trim().toLowerCase();

                    switch (userInput) {
                        case "one" -> myPassword(scan);
                        case "all" -> allMyPassword();
                        default -> System.out.println("Invalid option. Please enter 'one' or 'all'.");
                    }
                } else {
                    System.out.println("Wrong password.");
                }
            } else {
                System.out.println("No user found.");
            }

        } catch (SQLException e) {
            System.err.println("Database error:");
            e.printStackTrace();
        }
    }

    public static void myPassword(Scanner scan) {
        System.out.println("Search by 'service' or 'username'?");
        String option = scan.nextLine().trim().toLowerCase();

        String queryField = null;

        switch (option) {
            case "username":
                queryField = "service_username";
                break;
            case "service":
                queryField = "service_name";
                break;
            default:
                System.out.println("Invalid option. Please enter 'service' or 'username'.");
                return;
        }

        System.out.print("Enter search value: ");
        String input = scan.nextLine().trim();

        String sql = "SELECT service_name, service_username, encrypted_password, notes " +
                "FROM vault_items WHERE user_id = ? AND " + queryField + " = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, loggedInUserId);
            stmt.setString(2, Cryptography.encrypt(input.toLowerCase()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String service = Cryptography.decrypt(rs.getString("service_name"));
                    String serviceUser = Cryptography.decrypt(rs.getString("service_username"));
                    String password = Cryptography.decrypt(rs.getString("encrypted_password"));
                    String notes = Cryptography.decrypt(rs.getString("notes"));

                    System.out.println("_____________________");
                    System.out.println("Service         : " + service);
                    System.out.println("Username        : " + serviceUser);
                    System.out.println("Password        : " + password);
                    System.out.println("Notes           : " + notes);
                } else {
                    System.out.println("No results found for your search.");
                }
            }

        } catch (Exception e) {
            System.err.println("An error occurred while retrieving password:");
            e.printStackTrace();
        }
    }


    public static void allMyPassword() {
        String sql = "SELECT service_name, encrypted_password, notes FROM vault_items WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, loggedInUserId);
            ResultSet rs = stmt.executeQuery();
            boolean found = false;

            while (rs.next()) {
                found = true;
                String service = Cryptography.decrypt(rs.getString("service_name"));
                String decryptedPassword = Cryptography.decrypt(rs.getString("encrypted_password"));
                String notes = Cryptography.decrypt(rs.getString("notes"));

                System.out.println("_____________________");
                System.out.println("Service: " + service);
                System.out.println("Password: " + decryptedPassword);
                System.out.println("Notes: " + notes);
            }

            if (!found) {
                System.out.println("No passwords found.");
            }

        } catch (Exception e) {
            System.err.println("Error retrieving passwords:");
            e.printStackTrace();
        }
    }

    public static void changePassword(Scanner scan) {
        System.out.print("Enter application/service: ");
        String service = scan.nextLine().toLowerCase();

        String currentPassword = readPassword(scan, "Enter your current password: ");

        try (Connection conn = getConnection()) {
            String sql = "SELECT encrypted_password FROM vault_items WHERE user_id = ? AND service_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loggedInUserId);
                stmt.setString(2, Cryptography.encrypt(service));

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String storedEncryptedPassword = rs.getString("encrypted_password");
                    String decryptedStoredPassword = Cryptography.decrypt(storedEncryptedPassword);

                    if (currentPassword.equals(decryptedStoredPassword)) {
                        String newPassword = readPassword(scan, "Enter your new password: ");
                        String encryptedNewPassword = Cryptography.encrypt(newPassword);

                        String updateSql = "UPDATE vault_items SET encrypted_password = ? WHERE user_id = ? AND service_name = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, encryptedNewPassword);
                            updateStmt.setInt(2, loggedInUserId);
                            updateStmt.setString(3, Cryptography.encrypt(service));

                            int rows = updateStmt.executeUpdate();
                            if (rows > 0) {
                                System.out.println("Password updated successfully.");
                            } else {
                                System.out.println("Password update failed.");
                            }
                        }
                    } else {
                        System.out.println("Current password is incorrect.");
                    }
                } else {
                    System.out.println("No password found for that service.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error during password change:");
            e.printStackTrace();
        }
    }

    public static void deleteMyPassword(Scanner scan) {
        System.out.print("Enter application/service to delete: ");
        String service = scan.nextLine().toLowerCase();

        String sql = "DELETE FROM vault_items WHERE user_id = ? AND service_name = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, loggedInUserId);
            stmt.setString(2, Cryptography.encrypt(service));

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Password for service: " + service + " has been deleted.");
            } else {
                System.out.println("No password found for that service.");
            }
        } catch (Exception e) {
            System.err.println("Error deleting password:");
            e.printStackTrace();
        }
    }

    private static String readPassword(Scanner scan, String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt);
            return new String(passwordChars);
        } else {
            System.out.print(prompt);
            return scan.nextLine();
        }
    }
}