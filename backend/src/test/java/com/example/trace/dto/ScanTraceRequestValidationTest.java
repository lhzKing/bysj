package com.example.trace.dto;

import com.example.trace.enums.ActionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ScanTraceRequestValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void shouldAcceptNormalChineseAndAsciiRouteFields() {
        ScanTraceRequest request = baseRequest();
        request.setFromNode("物流中转站-华东");
        request.setToNode("Warehouse_A01");
        request.setProvince("江苏省");
        request.setCity("苏州市");
        request.setIdempotencyKey("scan-20260505-0001");

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectHtmlLikeNodeName() {
        ScanTraceRequest request = baseRequest();
        request.setToNode("<img src=x onerror=alert(1)>");

        Set<ConstraintViolation<ScanTraceRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("toNode");
                    assertThat(violation.getMessage()).isEqualTo("toNode contains unsupported characters");
                });
    }

    @Test
    void shouldRejectOverlongRegionField() {
        ScanTraceRequest request = baseRequest();
        request.setProvince("浙".repeat(33));

        Set<ConstraintViolation<ScanTraceRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("province");
                    assertThat(violation.getMessage()).isEqualTo("province length must be <= 32");
                });
    }

    @Test
    void shouldRejectHtmlLikeRemark() {
        ScanTraceRequest request = baseRequest();
        request.setRemark("<script>alert(1)</script>");

        Set<ConstraintViolation<ScanTraceRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("remark");
                    assertThat(violation.getMessage()).isEqualTo("remark contains unsupported characters");
                });
    }

    @Test
    void shouldRejectUnsafeIdempotencyKey() {
        ScanTraceRequest request = baseRequest();
        request.setIdempotencyKey("<scan>");

        Set<ConstraintViolation<ScanTraceRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("idempotencyKey");
                    assertThat(violation.getMessage()).isEqualTo("idempotencyKey contains unsupported characters");
                });
    }

    private ScanTraceRequest baseRequest() {
        ScanTraceRequest request = new ScanTraceRequest();
        request.setActionType(ActionType.INBOUND);
        return request;
    }
}
