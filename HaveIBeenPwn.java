import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Scanner;

public class HaveIBeenPwn {

    /**
     * Main method to check if a password has been pwned
     * Uses the k-anonymity model - only sends first 5 chars of SHA1 hash
     */
    public static void checkPasswordPwned(Scanner scan) {
        System.out.print("Enter a password to check: ");
        String password = scan.nextLine();

        if (password.isEmpty()) {
            System.out.println("Invalid password.");
            return;
        }

        System.out.println("Checking if this password has been compromised...");

        int count = checkPassword(password);

        if (count > 0) {
            System.out.println("⚠️  WARNING: This password has been seen " + count + " times in data breaches!");
            System.out.println("DO NOT use this password. Choose a different one.");
        } else if (count == 0) {
            System.out.println("✓ Good news! This password was not found in any known breaches.");
            System.out.println("However, still use a strong, unique password for each account.");
        } else {
            System.out.println("Unable to check password. Please try again later.");
        }
    }

    /**
     * Check if a password appears in the HIBP database
     * Returns the number of times it appears, or -1 on error
     */
    public static int checkPassword(String password) {
        HttpURLConnection conn = null;
        try {
            // Generate SHA1 hash of the password
            String sha1Hash = generateSHA1(password).toUpperCase();

            // Split hash - send only first 5 chars (k-anonymity)
            String hashPrefix = sha1Hash.substring(0, 5);
            String hashSuffix = sha1Hash.substring(5);

            // Make API request with hash prefix
            URL url = new URL("https://api.pwnedpasswords.com/range/" + hashPrefix);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "JavaPasswordManager");
            conn.setRequestProperty("Add-Padding", "true"); // Add padding for privacy
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                // Read response and look for our hash suffix
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Each line format: "HASHSUFFIX:COUNT"
                        String[] parts = line.split(":");
                        if (parts.length == 2 && parts[0].equalsIgnoreCase(hashSuffix)) {
                            return Integer.parseInt(parts[1]);
                        }
                    }
                }
                // Hash suffix not found in response = password not pwned
                return 0;
            } else {
                System.err.println("Error: Unexpected response code: " + responseCode);
                return -1;
            }

        } catch (java.net.UnknownHostException e) {
            System.err.println("Error: Cannot connect to pwnedpasswords.com. Check your internet connection.");
            return -1;
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Error: Connection timed out. Please try again later.");
            return -1;
        } catch (Exception e) {
            System.err.println("Error checking password: " + e.getMessage());
            e.printStackTrace();
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Generate SHA1 hash of a string
     */
    private static String generateSHA1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(input.getBytes("UTF-8"));

        // Convert byte array to hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}