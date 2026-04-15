import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class Cryptography {

    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static final SecretKey key;

    static {
        try {
            String keyString = PropertiesProvider.PROPS.getProperty("key");

            if (keyString != null && !keyString.isEmpty()) {
                // Load existing key from properties
                byte[] keyBytes = Base64.getDecoder().decode(keyString);
                key = new SecretKeySpec(keyBytes, "AES");
            } else {
                // Generate new key if not in properties
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(AES_KEY_SIZE);
                key = keyGen.generateKey();

                String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
                System.out.println("Generated new encryption key. Add this to your password.properties:");
                System.out.println("key=" + encodedKey);
            }
        } catch (Exception e) {
            System.err.println("ERROR initializing crypto key: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not initialize crypto key", e);
        }
    }

    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
        buffer.put(iv);
        buffer.put(encrypted);

        String result = Base64.getEncoder().encodeToString(buffer.array());
        return result;
    }

    public static String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }

        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        return result;
    }

    public static SecretKey getKey() {
        return key;
    }
}