package com.video.user.util;

import com.video.common.exception.VideoException;
import com.video.common.result.ResultCodeEnum;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 账号与密码规则（注册 / 登录共用）。
 * <ul>
 *   <li>用户名：2-20 字符，允许中英文、数字、下划线，禁止纯数字（避免与历史 uid 登录混淆），不区分大小写；</li>
 *   <li>密码：至少 8 个字符、最多 72 字节（bcrypt 上限），允许任意字符（含空格/unicode），不做组合规则，但拒绝常见弱密码。</li>
 * </ul>
 */
public final class AccountRules {
    public static final int USERNAME_MIN_LEN = 2;
    public static final int USERNAME_MAX_LEN = 20;
    public static final int PASSWORD_MIN_LEN = 8;
    /** bcrypt 只取前 72 字节，超长直接拒绝而不是静默截断 */
    public static final int PASSWORD_MAX_BYTES = 72;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{IsHan}A-Za-z0-9_]+$");
    private static final Pattern ALL_DIGITS = Pattern.compile("^[0-9]+$");
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "administrator", "root", "system", "anilink", "official", "support", "guest", "null", "undefined");

    /** 常见弱密码（本地小表，不联网；只挡最典型的撞库/猜测口令） */
    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "12345678", "123456789", "1234567890", "87654321", "11111111", "00000000", "88888888", "66666666",
            "password", "password1", "passw0rd", "qwertyui", "qwerty123", "1qaz2wsx", "zxcvbnm123", "abc12345",
            "a1234567", "iloveyou", "admin123", "admin888", "woaini1314", "5201314520", "123123123", "asdfghjkl");

    private AccountRules() {
    }

    /** 登录/注册统一规范化：去首尾空格（不做大小写转换，唯一性由数据库 utf8mb3_general_ci 保证） */
    public static String normalizeUsername(String raw) {
        return raw == null ? "" : raw.trim();
    }

    /** 昵称为空时默认等于用户名 */
    public static String normalizeNickname(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        return value.isEmpty() ? fallback : value;
    }

    public static void validateUsername(String username) {
        if (username == null || username.length() < USERNAME_MIN_LEN || username.length() > USERNAME_MAX_LEN) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "用户名需为 " + USERNAME_MIN_LEN + "-" + USERNAME_MAX_LEN + " 个字符");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "用户名只能包含中英文、数字和下划线");
        }
        if (ALL_DIGITS.matcher(username).matches()) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "用户名不能是纯数字");
        }
        if (RESERVED_USERNAMES.contains(username.toLowerCase())) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "该用户名为系统保留，请换一个");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LEN) {
            throw new VideoException(ResultCodeEnum.PASSWORD_WEAK.getCode(), "密码至少 " + PASSWORD_MIN_LEN + " 位");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "密码过长（最多 " + PASSWORD_MAX_BYTES + " 字节）");
        }
        if (WEAK_PASSWORDS.contains(password.toLowerCase())) {
            throw new VideoException(ResultCodeEnum.PASSWORD_WEAK.getCode(), "密码太常见，请换一个");
        }
    }

    public static void validateNickname(String nickname) {
        if (nickname == null || nickname.length() < 2 || nickname.length() > 20) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "昵称需为 2-20 个字符");
        }
        if (CONTROL_CHARS.matcher(nickname).find()) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "昵称包含非法字符");
        }
    }
}
