package com.example.trace.util;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateTimeUtilTest {

    @Test
    void parseOrNow_shouldParseIsoLocalDateTimeWithSeconds() {
        LocalDateTime parsed = DateTimeUtil.parseOrNow("2026-01-16T10:30:00");

        assertThat(parsed).isEqualTo(LocalDateTime.of(2026, 1, 16, 10, 30));
    }

    @Test
    void parseOrNow_shouldDefaultBlankValueToCurrentTime() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        LocalDateTime parsed = DateTimeUtil.parseOrNow("  ");

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(parsed).isBetween(before, after);
    }

    @Test
    void parseOrNow_shouldRejectInvalidValueInsteadOfFallingBackToNow() {
        assertThatThrownBy(() -> DateTimeUtil.parseOrNow("not-a-date"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getHttpStatus()).isEqualTo(400);
                    assertThat(exception.getMessage()).contains("eventTime must be ISO-8601");
                });
    }

    @Test
    void parseOrNow_shouldRejectLegacySpaceSeparatedFormat() {
        assertThatThrownBy(() -> DateTimeUtil.parseOrNow("2026-01-16 10:30:00"))
                .isInstanceOf(BizException.class);
    }
}
