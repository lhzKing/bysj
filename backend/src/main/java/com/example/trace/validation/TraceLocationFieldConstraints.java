package com.example.trace.validation;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;

import java.util.regex.Pattern;

/**
 * Validation rules for trace route location fields rendered on maps or persisted in the hash chain.
 */
public final class TraceLocationFieldConstraints {

    public static final int NODE_MAX_LENGTH = 64;
    public static final int REGION_MAX_LENGTH = 32;
    public static final int REMARK_MAX_LENGTH = 255;
    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 64;
    public static final String SAFE_TEXT_PATTERN =
            "[\\p{IsHan}A-Za-z0-9][\\p{IsHan}A-Za-z0-9 _\\-（）()·.。]*";
    public static final String OPTIONAL_SAFE_TEXT_PATTERN = "^\\s*$|^" + SAFE_TEXT_PATTERN + "$";
    public static final String OPTIONAL_REMARK_PATTERN = "^[^<>\\p{Cntrl}]*$";

    private static final Pattern SAFE_TEXT = Pattern.compile("^" + SAFE_TEXT_PATTERN + "$");
    private static final Pattern UNSAFE_REMARK = Pattern.compile("[<>\\p{Cntrl}]");

    private TraceLocationFieldConstraints() {
    }

    public static String normalizeNode(String fieldName, String value) {
        return normalize(fieldName, value, NODE_MAX_LENGTH);
    }

    public static String normalizeRegion(String fieldName, String value) {
        return normalize(fieldName, value, REGION_MAX_LENGTH);
    }

    public static String normalizeRemark(String fieldName, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > REMARK_MAX_LENGTH) {
            throw new BizException(BizCode.PARAM_ERROR,
                    fieldName + " length must be <= " + REMARK_MAX_LENGTH);
        }
        if (UNSAFE_REMARK.matcher(trimmed).find()) {
            throw new BizException(BizCode.PARAM_ERROR,
                    fieldName + " contains unsupported characters");
        }
        return trimmed;
    }

    public static String normalizeIdempotencyKey(String fieldName, String value) {
        return normalize(fieldName, value, IDEMPOTENCY_KEY_MAX_LENGTH);
    }

    private static String normalize(String fieldName, String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new BizException(BizCode.PARAM_ERROR,
                    fieldName + " length must be <= " + maxLength);
        }
        if (!SAFE_TEXT.matcher(trimmed).matches()) {
            throw new BizException(BizCode.PARAM_ERROR,
                    fieldName + " contains unsupported characters");
        }
        return trimmed;
    }
}
