package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.service.TraceDemoDataService;
import com.example.trace.service.TraceMasterDataSeedService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private TraceDemoDataService traceDemoDataService;

    @Mock
    private TraceMasterDataSeedService traceMasterDataSeedService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void seedMasterData_shouldDelegateToService() {
        Map<String, Object> serviceResult = Map.of(
                "demoUsers", Map.of("inserted", 8, "skipped", 0),
                "traceNodes", Map.of("inserted", 18, "skipped", 0),
                "partSpecs", Map.of("inserted", 15, "skipped", 0),
                "userNodeBindings", Map.of("inserted", 16, "skipped", 0));
        MockHttpServletRequest request = requestWithOperator("superadmin", "SUPER_ADMIN");
        when(traceMasterDataSeedService.seedMasterData("superadmin", "SUPER_ADMIN")).thenReturn(serviceResult);

        ApiResponse<Map<String, Object>> response = adminController.seedMasterData(request);

        assertThat(response.getData()).isEqualTo(serviceResult);
        verify(traceMasterDataSeedService).seedMasterData("superadmin", "SUPER_ADMIN");
    }

    @Test
    void generateSampleData_shouldDelegateToTraceDemoDataService() {
        Map<String, Object> serviceResult = Map.of("traceCodes", 3, "lifecycleLogs", 12, "partSpecs", 5);
        MockHttpServletRequest request = requestWithOperator("superadmin", "SUPER_ADMIN");
        when(traceDemoDataService.generateSampleData(3, "superadmin", "SUPER_ADMIN")).thenReturn(serviceResult);

        ApiResponse<Map<String, Object>> response = adminController.generateSampleData(3, request);

        assertThat(response.getData()).isEqualTo(serviceResult);
        verify(traceDemoDataService).generateSampleData(3, "superadmin", "SUPER_ADMIN");
    }

    @Test
    void clearTraceData_shouldDelegateToTraceDemoDataServiceWhenConfirmationMatches() {
        Map<String, Object> serviceResult = Map.of("deletedLogs", 7L, "deletedSnapshots", 4L);
        MockHttpServletRequest request = requestWithOperator("superadmin", "SUPER_ADMIN");
        when(traceDemoDataService.clearTraceData("superadmin", "SUPER_ADMIN")).thenReturn(serviceResult);

        ApiResponse<Map<String, Object>> response = adminController.clearTraceData("DELETE_TRACE_DATA", request);

        assertThat(response.getData()).isEqualTo(serviceResult);
        verify(traceDemoDataService).clearTraceData("superadmin", "SUPER_ADMIN");
    }

    @Test
    void clearTraceData_shouldRejectInvalidConfirmation() {
        MockHttpServletRequest request = requestWithOperator("superadmin", "SUPER_ADMIN");

        assertThatThrownBy(() -> adminController.clearTraceData("DELETE", request))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("清空溯源数据需要 confirm=DELETE_TRACE_DATA");
                });

        verifyNoInteractions(traceDemoDataService);
    }

    @Test
    void adminEndpoints_shouldRequireDedicatedPermissions() throws NoSuchMethodException {
        RequirePermission generatePermission = AdminController.class
                .getMethod("generateSampleData", int.class, HttpServletRequest.class)
                .getAnnotation(RequirePermission.class);
        RequirePermission clearPermission = AdminController.class
                .getMethod("clearTraceData", String.class, HttpServletRequest.class)
                .getAnnotation(RequirePermission.class);
        RequirePermission seedMasterPermission = AdminController.class
                .getMethod("seedMasterData", HttpServletRequest.class)
                .getAnnotation(RequirePermission.class);

        assertThat(generatePermission.value()).containsExactly("trace:data:generate");
        assertThat(clearPermission.value()).containsExactly("trace:data:clear");
        assertThat(seedMasterPermission.value()).containsExactly("trace:data:seed-master");
    }

    private static MockHttpServletRequest requestWithOperator(String username, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("username", username);
        request.setAttribute("role", role);
        return request;
    }
}
