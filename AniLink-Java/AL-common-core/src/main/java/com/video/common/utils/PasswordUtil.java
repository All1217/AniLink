package com.video.common.utils;

import cn.hutool.crypto.digest.BCrypt;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * 密码哈希工具：统一使用 bcrypt 存储，兼容历史无盐 MD5。
 * <p>bcrypt 轮数固定 10（OWASP 建议下限）；历史 MD5 由调用方在登录成功时就地升级为 bcrypt。</p>
 */
public final class PasswordUtil {
    /** bcrypt 轮数（2^10，单次校验约 50-100ms） */
    private static final int COST = 10;
    private static final String MD5_PATTERN = "^[0-9a-fA-F]{32}$";
    /** 账号不存在时也跑一次 bcrypt，抹平"账号是否存在"的响应时间差 */
    private static final String DUMMY_HASH = BCrypt.hashpw("anilink-dummy-password", BCrypt.gensalt(COST));

    private PasswordUtil() {
    }

    /** 生成 bcrypt 哈希（自带随机盐） */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }

    /** 校验密码；md5 历史哈希与 bcrypt 都能处理，非法哈希返回 false 而不是抛异常 */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            if (isLegacyMd5(storedHash)) {
                return storedHash.equalsIgnoreCase(DigestUtils.md5Hex(rawPassword));
            }
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 是否为历史无盐 MD5（32 位十六进制） */
    public static boolean isLegacyMd5(String storedHash) {
        return storedHash != null && storedHash.matches(MD5_PATTERN);
    }

    /** 是否需要升级：仍是 MD5，或 bcrypt 的轮数低于当前标准 */
    public static boolean needsUpgrade(String storedHash) {
        if (isLegacyMd5(storedHash)) {
            return true;
        }
        int cost = costOf(storedHash);
        return cost > 0 && cost < COST;
    }

    /** 空跑一次 bcrypt，用于账户不存在时对齐响应时间 */
    public static void dummyVerify(String rawPassword) {
        BCrypt.checkpw(rawPassword == null ? "" : rawPassword, DUMMY_HASH);
    }

    /** 从 {@code $2a$10$...} 中解析轮数，解析失败返回 0 */
    private static int costOf(String storedHash) {
        if (storedHash == null || !storedHash.startsWith("$2")) {
            return 0;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length < 3) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
