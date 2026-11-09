package testframework.helpers;

import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class TokenGenerator {
    private final String VALID_CHARACTERS = "0123456789ABCDEF";
    private final int TOKEN_LENGTH = 32;
    private final Random RANDOM = new Random();

    public String generateValidToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(VALID_CHARACTERS.charAt(RANDOM.nextInt(VALID_CHARACTERS.length())));
        }
        return token.toString();
    }

}