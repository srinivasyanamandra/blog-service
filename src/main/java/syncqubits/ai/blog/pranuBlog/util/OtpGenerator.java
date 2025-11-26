package syncqubits.ai.blog.pranuBlog.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    @Value("${app.otp.length}")
    private int otpLength;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        int otp = RANDOM.nextInt(max - min + 1) + min;
        return String.valueOf(otp);
    }
}