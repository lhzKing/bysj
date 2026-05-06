package com.example.trace.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类 - 使用 BCrypt 算法
 * 
 * BCrypt 特点：
 * - 自动加盐，每次加密结果不同
 * - 计算成本可调（默认10轮）
 * - 抗彩虹表攻击
 */
@Component
public class PasswordEncoder {

    private final BCryptPasswordEncoder encoder;

    public PasswordEncoder() {
        // 使用默认强度（10轮），可根据需要调整
        // 强度越高越安全，但加密/验证时间也越长
        this.encoder = new BCryptPasswordEncoder();
    }

    /**
     * 加密密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt 加密后的密码（60字符）
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码
     *
     * @param rawPassword     明文密码（用户输入）
     * @param encodedPassword 加密后的密码（数据库存储）
     * @return true=匹配, false=不匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 检查密码是否已经是 BCrypt 加密格式
     * BCrypt 格式：$2a$10$... 或 $2b$10$...
     *
     * @param password 待检查的密码
     * @return true=已加密, false=明文
     */
    public boolean isEncoded(String password) {
        if (password == null || password.length() < 60) {
            return false;
        }
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
