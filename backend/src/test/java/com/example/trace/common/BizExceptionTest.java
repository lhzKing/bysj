package com.example.trace.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BizExceptionTest {

    @Test
    void constructor_shouldInferHttpStatusFromBizCode() {
        assertThat(new BizException(BizCode.BAD_REQUEST, "bad request").getHttpStatus()).isEqualTo(400);
        assertThat(new BizException(BizCode.PASSWORD_ERROR, "password error").getHttpStatus()).isEqualTo(401);
        assertThat(new BizException(BizCode.USER_EXISTS, "user exists").getHttpStatus()).isEqualTo(409);
        assertThat(new BizException(BizCode.TRACE_NOT_FOUND, "trace missing").getHttpStatus()).isEqualTo(404);
        assertThat(new BizException(BizCode.CONCURRENT_CONFLICT, "conflict").getHttpStatus()).isEqualTo(409);
        assertThat(new BizException(BizCode.INVALID_ACTION_TYPE, "invalid action").getHttpStatus()).isEqualTo(400);
        assertThat(new BizException(BizCode.CORRECTION_TARGET_NOT_FOUND, "correction target missing").getHttpStatus()).isEqualTo(404);
        assertThat(new BizException(BizCode.DASHBOARD_QUERY_ERROR, "dashboard query failed").getHttpStatus()).isEqualTo(500);
    }

    @Test
    void explicitConstructor_shouldPreserveOverrideStatus() {
        BizException ex = new BizException(BizCode.PASSWORD_ERROR, 400, "old password mismatch");

        assertThat(ex.getCode()).isEqualTo(BizCode.PASSWORD_ERROR);
        assertThat(ex.getHttpStatus()).isEqualTo(400);
    }

    @Test
    void causeConstructor_shouldPreserveCauseAndInferredStatus() {
        RuntimeException cause = new RuntimeException("boom");

        BizException ex = new BizException(BizCode.SERVER_ERROR, "sign failed", cause);

        assertThat(ex.getHttpStatus()).isEqualTo(500);
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
