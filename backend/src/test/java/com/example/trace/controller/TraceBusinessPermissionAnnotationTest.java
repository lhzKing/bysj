package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceFlowTaskCompleteRequest;
import com.example.trace.dto.TraceFlowTaskCreateRequest;
import com.example.trace.dto.TraceFlowTaskScanRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TraceBusinessPermissionAnnotationTest {

    @Test
    void traceController_shouldUseExplicitBusinessActionPermissionsWithLegacyFallbacks() throws Exception {
        assertPermissions(
                TraceController.class.getMethod("createTraces", ProduceAssignRequest.class, HttpServletRequest.class),
                "trace:batch:create", "trace:create"
        );
        assertPermissions(
                TraceController.class.getMethod("createEvent", String.class, ScanTraceRequest.class, HttpServletRequest.class),
                "trace:scan", "trace:inbound", "trace:outbound", "trace:transfer", "trace:exception:handle"
        );
        assertPermissions(
                TraceController.class.getMethod("printCode", String.class, TraceCodeLabelActionRequest.class, HttpServletRequest.class),
                "trace:code:print", "trace:create"
        );
        assertPermissions(
                TraceController.class.getMethod("reprintCode", String.class, TraceCodeLabelActionRequest.class, HttpServletRequest.class),
                "trace:code:print", "trace:create"
        );
        assertPermissions(
                TraceController.class.getMethod("voidCode", String.class, TraceCodeLabelActionRequest.class, HttpServletRequest.class),
                "trace:code:print", "trace:create"
        );
    }

    @Test
    void traceCodeController_shouldGateActivationWithDedicatedPermission() throws Exception {
        assertPermissions(
                TraceCodeController.class.getMethod("activateCode", String.class, TraceCodeActivateRequest.class, HttpServletRequest.class),
                "trace:code:activate", "trace:create"
        );
    }

    @Test
    void traceFlowTaskController_shouldUseDedicatedTaskPermissionsWithLegacyFallbacks() throws Exception {
        assertPermissions(
                TraceFlowTaskController.class.getMethod("createTask", TraceFlowTaskCreateRequest.class, HttpServletRequest.class),
                "trace:task:create", "trace:create", "trace:scan", "trace:inbound", "trace:outbound", "trace:transfer"
        );
        assertPermissions(
                TraceFlowTaskController.class.getMethod("scanTask", Long.class, TraceFlowTaskScanRequest.class, HttpServletRequest.class),
                "trace:task:scan", "trace:scan", "trace:outbound", "trace:inbound", "trace:transfer"
        );
        assertPermissions(
                TraceFlowTaskController.class.getMethod("completeTask", Long.class, TraceFlowTaskCompleteRequest.class),
                "trace:task:complete", "trace:create", "trace:scan", "trace:inbound", "trace:outbound", "trace:transfer"
        );
        assertPermissions(
                TraceFlowTaskController.class.getMethod("cancelTask", Long.class),
                "trace:task:create", "trace:task:complete", "trace:create", "trace:scan",
                "trace:inbound", "trace:outbound", "trace:transfer"
        );
    }

    private static void assertPermissions(Method method, String... expectedPermissions) {
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        assertThat(annotation).as("missing @RequirePermission on " + method).isNotNull();
        assertThat(annotation.value()).containsExactly(expectedPermissions);
    }
}
