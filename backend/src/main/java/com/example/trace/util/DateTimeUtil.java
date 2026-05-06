package com.example.trace.util;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;

public class DateTimeUtil {

    public static final String EVENT_TIME_FORMAT_DESCRIPTION =
            "ISO-8601 local datetime, e.g. 2026-01-16T10:30:00";

    private static final DateTimeFormatter EVENT_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private DateTimeUtil() {
    }

    /**
     * Parse trace event time strictly.
     *
     * <p>Blank values keep the historical contract and default to server current time. Non-blank values
     * must be ISO-8601 local date-time strings; invalid values raise a 400 business error instead of
     * silently falling back to now.</p>
     */
    public static LocalDateTime parseOrNow(String input) {
        if (input == null || input.isBlank()) {
            return LocalDateTime.now();
        }

        return parseEventTime(input);
    }

    public static LocalDateTime parseEventTime(String input) {
        if (input == null || input.isBlank()) {
            throw invalidEventTime();
        }

        try {
            return LocalDateTime.parse(input.trim(), EVENT_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw invalidEventTime();
        }
    }

    private static BizException invalidEventTime() {
        return new BizException(BizCode.PARAM_ERROR,
                "eventTime must be " + EVENT_TIME_FORMAT_DESCRIPTION);
    }
}
