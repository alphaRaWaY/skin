package org.skinAI.utils;

import java.security.SecureRandom;

public class RandomPasswordUtil {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#_?";

    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARACTERS;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomPassword() {
        int length = (int) (Math.random()*14+6);
        if (length < 6 || length > 20) {
            throw new IllegalArgumentException("密码长度必须在 6-20 之间！");
        }

        StringBuilder password = new StringBuilder(length);

        // 确保至少包含一个大小写字母、数字和特殊字符
        password.append(UPPERCASE.charAt(RANDOM.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(RANDOM.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARACTERS.charAt(RANDOM.nextInt(SPECIAL_CHARACTERS.length())));

        // 生成剩余部分
        for (int i = 4; i < length; i++) {
            password.append(ALL_CHARACTERS.charAt(RANDOM.nextInt(ALL_CHARACTERS.length())));
        }

        // 由于我们是按类别添加字符，所以打乱顺序以增加随机性
        return shuffleString(password.toString());
    }

    // Fisher-Yates 洗牌算法，用于打乱字符串
    private static String shuffleString(String input) {
        char[] array = input.toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }

    public static void main(String[] args) {
        System.out.println("随机密码: " + generateRandomPassword());  // 生成12位随机密码
    }
}
