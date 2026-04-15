import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.net.URI;
import java.time.Instant;

public class TwoFa {

    public static String generateSecret() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
            keyGen.init(160);
            SecretKey secretKey = keyGen.generateKey();

            Base32 base32 = new Base32();
            return base32.encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Could not generate secret key", e);
        }
    }

    public static boolean validateCode(String base32Secret, String code) {
        try {
            Base32 base32 = new Base32();
            byte[] keyBytes = base32.decode(base32Secret);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");

            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
            String generatedCode = String.format("%06d", totp.generateOneTimePassword(keySpec, Instant.now()));

            return generatedCode.equals(code);
        } catch (Exception e) {
            System.err.println("Error validating code:");
            e.printStackTrace();
            return false;
        }
    }

    public static void generateQRCode(String email, String base32Secret) {
        try {
            String issuer = "PasswordManager";
            String otpAuthUrl = String.format(
                    "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    issuer, email, base32Secret, issuer
            );

            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=" +
                    java.net.URLEncoder.encode(otpAuthUrl, "UTF-8") + "&size=300x300";

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(qrCodeUrl));
            } else {
                System.out.println("QR-code URL: " + qrCodeUrl);
            }
        } catch (Exception e) {
            System.err.println("Error generating QR-kode:");
            e.printStackTrace();
        }
    }
}
