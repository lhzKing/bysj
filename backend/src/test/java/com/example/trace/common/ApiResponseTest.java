package com.example.trace.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successFactory_shouldKeepCanonicalOkPayload() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.getCode()).isEqualTo(BizCode.SUCCESS);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo("payload");
    }

    @Test
    void okAlias_shouldDelegateToSuccessFactory() {
        ApiResponse<String> success = ApiResponse.success("payload", "done");
        ApiResponse<String> ok = ApiResponse.ok("payload", "done");

        assertThat(ok).usingRecursiveComparison().isEqualTo(success);
    }

    @Test
    void createdFactory_shouldPreserveCreatedStatus() {
        ApiResponse<String> response = ApiResponse.created("payload");

        assertThat(response.getCode()).isEqualTo(BizCode.SUCCESS);
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getMessage()).isEqualTo("创建成功");
        assertThat(response.getData()).isEqualTo("payload");
    }

    @Test
    void failFactory_shouldInferStatusFromBizCode() {
        ApiResponse<Void> response = ApiResponse.fail(BizCode.USER_EXISTS, "用户名已存在");

        assertThat(response.getCode()).isEqualTo(BizCode.USER_EXISTS);
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getMessage()).isEqualTo("用户名已存在");
        assertThat(response.getData()).isNull();
    }

    @Test
    void explicitFailFactory_shouldPreserveOverrideStatus() {
        ApiResponse<Void> response = ApiResponse.fail(BizCode.PASSWORD_ERROR, 400, "原密码错误");

        assertThat(response.getCode()).isEqualTo(BizCode.PASSWORD_ERROR);
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMessage()).isEqualTo("原密码错误");
    }
}
