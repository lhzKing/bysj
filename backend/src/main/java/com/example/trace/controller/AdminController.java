package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.common.BizException;
import com.example.trace.service.TraceDemoDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_ROLE = "role";
    private static final String CLEAR_TRACE_DATA_CONFIRMATION = "DELETE_TRACE_DATA";

    private final TraceDemoDataService traceDemoDataService;

    public AdminController(TraceDemoDataService traceDemoDataService) {
        this.traceDemoDataService = traceDemoDataService;
    }

    @PostMapping("/generate-sample-data")
    @RequirePermission("trace:data:generate")
    public ApiResponse<Map<String, Object>> generateSampleData(
            @RequestParam(defaultValue = "100") int count,
            HttpServletRequest request
    ) {
        String operator = requestAttribute(request, ATTR_USERNAME);
        String operatorRole = requestAttribute(request, ATTR_ROLE);
        return ApiResponse.success(traceDemoDataService.generateSampleData(count, operator, operatorRole));
    }

    @DeleteMapping("/clear-trace-data")
    @RequirePermission("trace:data:clear")
    public ApiResponse<Map<String, Object>> clearTraceData(
            @RequestParam(name = "confirm", required = false) String confirm,
            HttpServletRequest request
    ) {
        String operator = requestAttribute(request, ATTR_USERNAME);
        String operatorRole = requestAttribute(request, ATTR_ROLE);

        if (!CLEAR_TRACE_DATA_CONFIRMATION.equals(confirm)) {
            log.warn("Dangerous trace-data clear rejected due to missing/invalid confirmation: operator={}, role={}, confirmationValid=false",
                    operator, operatorRole);
            throw BizException.badRequest("清空溯源数据需要 confirm=DELETE_TRACE_DATA");
        }

        return ApiResponse.success(traceDemoDataService.clearTraceData(operator, operatorRole));
    }

    private String requestAttribute(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return "unknown";
    }
}
