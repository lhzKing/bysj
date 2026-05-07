package com.example.trace.dto;

import com.example.trace.config.JacksonConfig;
import com.example.trace.enums.ActionType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoContractTest {

    private final ObjectMapper mapper = new JacksonConfig().objectMapper();

    @Test
    void snakeCaseBodies_shouldDeserializeThroughGlobalNamingStrategy() throws Exception {
        ProduceAssignRequest produce = mapper.readValue("""
                {"part_code":"P-001","batch_no":"ASSIGN-001","production_order_no":"PO-001","quantity":3,"manufacturer_node":"Factory-A","manufacturer_node_id":10}
                """, ProduceAssignRequest.class);
        ScanTraceRequest scan = mapper.readValue("""
                {"action_type":"INBOUND","from_node":"Node-A","to_node":"Node-B","event_time":"2026-04-13T10:00:00","correction_of":8}
                """, ScanTraceRequest.class);
        TraceCodeActivateRequest activate = mapper.readValue("""
                {"activation_node":"Node-A","device_id":"SCANNER-01","event_time":"2026-05-06T09:30:00","remark":"扫码复核"}
                """, TraceCodeActivateRequest.class);
        TraceNodeCreateRequest nodeCreate = mapper.readValue("""
                {"node_code":"factory-bj-001","node_name":"北京工厂","node_type":"FACTORY","org_id":3,"province":"北京","city":"北京市","address":"亦庄工业园","enabled":true}
                """, TraceNodeCreateRequest.class);
        TraceNodeUpdateRequest nodeUpdate = mapper.readValue("""
                {"node_name":"上海仓库","node_type":"warehouse","org_id":4,"province":"上海","city":"上海市","address":"浦东仓储园","enabled":false}
                """, TraceNodeUpdateRequest.class);
        TraceUserNodeBindingUpdateRequest userNodeBinding = mapper.readValue("""
                {"node_ids":[1,2],"default_node_id":2}
                """, TraceUserNodeBindingUpdateRequest.class);
        TraceFlowTaskCreateRequest flowTaskCreate = mapper.readValue("""
                {"task_no":"ship-001","task_type":"outbound","source_node_id":1,"target_node_id":2,"expected_quantity":100,"remark":"发货"}
                """, TraceFlowTaskCreateRequest.class);
        TraceFlowTaskCompleteRequest flowTaskComplete = mapper.readValue("""
                {"actual_quantity":98,"remark":"少扫2件","discrepancy_reason":"运输破损待复核"}
                """, TraceFlowTaskCompleteRequest.class);
        TraceFlowTaskScanRequest flowTaskScan = mapper.readValue("""
                {"trace_code":"TRACE-001","event_time":"2026-05-06T13:10:00","idempotency_key":"SCAN-001","remark":"装车出库"}
                """, TraceFlowTaskScanRequest.class);
        TraceAggregationBindRequest aggregationBind = mapper.readValue("""
                {"parent_code":"CARTON-001","child_code":"TRACE-001","relation_type":"carton","remark":"装箱"}
                """, TraceAggregationBindRequest.class);
        TraceAggregationReleaseRequest aggregationRelease = mapper.readValue("""
                {"remark":"拆箱复核"}
                """, TraceAggregationReleaseRequest.class);

        assertThat(produce.getPartCode()).isEqualTo("P-001");
        assertThat(produce.getBatchNo()).isEqualTo("ASSIGN-001");
        assertThat(produce.getProductionOrderNo()).isEqualTo("PO-001");
        assertThat(produce.getQuantity()).isEqualTo(3);
        assertThat(produce.getManufacturerNode()).isEqualTo("Factory-A");
        assertThat(produce.getManufacturerNodeId()).isEqualTo(10L);
        assertThat(scan.getActionType()).isEqualTo(ActionType.INBOUND);
        assertThat(scan.getFromNode()).isEqualTo("Node-A");
        assertThat(scan.getToNode()).isEqualTo("Node-B");
        assertThat(scan.getEventTime()).isEqualTo("2026-04-13T10:00:00");
        assertThat(scan.getCorrectionOf()).isEqualTo(8L);
        assertThat(activate.getActivationNode()).isEqualTo("Node-A");
        assertThat(activate.getDeviceId()).isEqualTo("SCANNER-01");
        assertThat(activate.getEventTime()).isEqualTo("2026-05-06T09:30:00");
        assertThat(activate.getRemark()).isEqualTo("扫码复核");
        assertThat(nodeCreate.getNodeCode()).isEqualTo("factory-bj-001");
        assertThat(nodeCreate.getNodeName()).isEqualTo("北京工厂");
        assertThat(nodeCreate.getNodeType()).isEqualTo(com.example.trace.enums.TraceNodeType.FACTORY);
        assertThat(nodeCreate.getOrgId()).isEqualTo(3L);
        assertThat(nodeCreate.getProvince()).isEqualTo("北京");
        assertThat(nodeCreate.getCity()).isEqualTo("北京市");
        assertThat(nodeCreate.getAddress()).isEqualTo("亦庄工业园");
        assertThat(nodeCreate.getEnabled()).isTrue();
        assertThat(nodeUpdate.getNodeName()).isEqualTo("上海仓库");
        assertThat(nodeUpdate.getNodeType()).isEqualTo(com.example.trace.enums.TraceNodeType.WAREHOUSE);
        assertThat(nodeUpdate.getOrgId()).isEqualTo(4L);
        assertThat(nodeUpdate.getProvince()).isEqualTo("上海");
        assertThat(nodeUpdate.getCity()).isEqualTo("上海市");
        assertThat(nodeUpdate.getAddress()).isEqualTo("浦东仓储园");
        assertThat(nodeUpdate.getEnabled()).isFalse();
        assertThat(userNodeBinding.getNodeIds()).containsExactly(1L, 2L);
        assertThat(userNodeBinding.getDefaultNodeId()).isEqualTo(2L);
        assertThat(flowTaskCreate.getTaskNo()).isEqualTo("ship-001");
        assertThat(flowTaskCreate.getTaskType()).isEqualTo(com.example.trace.enums.TraceFlowTaskType.OUTBOUND);
        assertThat(flowTaskCreate.getSourceNodeId()).isEqualTo(1L);
        assertThat(flowTaskCreate.getTargetNodeId()).isEqualTo(2L);
        assertThat(flowTaskCreate.getExpectedQuantity()).isEqualTo(100);
        assertThat(flowTaskComplete.getActualQuantity()).isEqualTo(98);
        assertThat(flowTaskComplete.getDiscrepancyReason()).isEqualTo("运输破损待复核");
        assertThat(flowTaskScan.getTraceCode()).isEqualTo("TRACE-001");
        assertThat(flowTaskScan.getEventTime()).isEqualTo("2026-05-06T13:10:00");
        assertThat(flowTaskScan.getIdempotencyKey()).isEqualTo("SCAN-001");
        assertThat(flowTaskScan.getRemark()).isEqualTo("装车出库");
        assertThat(aggregationBind.getParentCode()).isEqualTo("CARTON-001");
        assertThat(aggregationBind.getChildCode()).isEqualTo("TRACE-001");
        assertThat(aggregationBind.getRelationType()).isEqualTo(com.example.trace.enums.TraceAggregationRelationType.CARTON);
        assertThat(aggregationBind.getRemark()).isEqualTo("装箱");
        assertThat(aggregationRelease.getRemark()).isEqualTo("拆箱复核");
    }

    @Test
    void camelCaseWhitelistBodies_shouldStillDeserializeAfterJsonPropertyCleanup() throws Exception {
        UserCreateRequest user = mapper.readValue("""
                {"username":"alice","password":"abc123","roleId":2,"status":1}
                """, UserCreateRequest.class);
        PartCreateRequest part = mapper.readValue("""
                {"partCode":"P-001","partName":"Bearing","partType":"Mechanical"}
                """, PartCreateRequest.class);
        AssignPermissionsRequest permissions = mapper.readValue("""
                {"permissionIds":[1,2,3]}
                """, AssignPermissionsRequest.class);
        ResetPasswordRequest reset = mapper.readValue("""
                {"newPassword":"newPass123"}
                """, ResetPasswordRequest.class);
        LoginRequest login = mapper.readValue("""
                {"username":"alice","password":"abc123","rememberMe":true}
                """, LoginRequest.class);

        assertThat(user.getRoleId()).isEqualTo(2L);
        assertThat(part.getPartCode()).isEqualTo("P-001");
        assertThat(part.getPartName()).isEqualTo("Bearing");
        assertThat(part.getPartType()).isEqualTo("Mechanical");
        assertThat(permissions.getPermissionIds()).containsExactly(1L, 2L, 3L);
        assertThat(reset.getNewPassword()).isEqualTo("newPass123");
        assertThat(login.isRememberMe()).isTrue();
    }

    @Test
    void produceAssignRequest_shouldRejectQuantityAboveMaximumByBeanValidation() {
        ProduceAssignRequest request = new ProduceAssignRequest();
        request.setSpuId(1L);
        request.setQuantity(ProduceAssignRequest.MAX_QUANTITY + 1);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(request))
                    .anySatisfy(violation -> {
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("quantity");
                        assertThat(violation.getMessage()).isEqualTo("quantity 必须在 1 到 500 之间");
                    });
        }
    }

    @Test
    void userUpdateRequest_shouldRejectStatusOutsideAllowedRangeByBeanValidation() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setStatus(2);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(request))
                    .anySatisfy(violation -> {
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("status");
                        assertThat(violation.getMessage()).isEqualTo("status must be 0 or 1");
                    });
        }
    }

    @Test
    void bodyDtos_shouldAvoidRedundantSnakeCaseAnnotationsWhileKeepingCamelCaseWhitelistAliases() {
        assertThat(jsonPropertyValue(PartCreateRequest.class, "partCode")).isNull();
        assertThat(jsonPropertyValue(PartCreateRequest.class, "partName")).isNull();
        assertThat(jsonPropertyValue(PartCreateRequest.class, "partType")).isNull();
        assertThat(jsonPropertyValue(PartUpdateRequest.class, "partName")).isNull();
        assertThat(jsonPropertyValue(PartUpdateRequest.class, "partType")).isNull();
        assertThat(jsonPropertyValue(RoleCreateRequest.class, "roleCode")).isNull();
        assertThat(jsonPropertyValue(RoleCreateRequest.class, "roleName")).isNull();
        assertThat(jsonPropertyValue(RoleUpdateRequest.class, "roleName")).isNull();
        assertThat(jsonPropertyValue(UserCreateRequest.class, "roleId")).isNull();
        assertThat(jsonPropertyValue(UserUpdateRequest.class, "roleId")).isNull();
        assertThat(jsonPropertyValue(AssignPermissionsRequest.class, "permissionIds")).isNull();
        assertThat(jsonPropertyValue(ResetPasswordRequest.class, "newPassword")).isNull();
        assertThat(jsonPropertyValue(ChangePasswordRequest.class, "oldPassword")).isNull();
        assertThat(jsonPropertyValue(ChangePasswordRequest.class, "newPassword")).isNull();
        assertThat(jsonPropertyValue(LoginRequest.class, "rememberMe")).isNull();

        assertThat(jsonAliases(ProduceAssignRequest.class, "spuId")).isEmpty();
        assertThat(jsonAliases(ProduceAssignRequest.class, "partCode")).isEmpty();
        assertThat(jsonAliases(ProduceAssignRequest.class, "batchNo")).isEmpty();
        assertThat(jsonAliases(ProduceAssignRequest.class, "productionOrderNo")).isEmpty();
        assertThat(jsonAliases(ProduceAssignRequest.class, "manufacturerNode")).isEmpty();
        assertThat(jsonAliases(ProduceAssignRequest.class, "manufacturerNodeId")).isEmpty();
        assertThat(jsonAliases(ScanTraceRequest.class, "actionType")).isEmpty();
        assertThat(jsonAliases(ScanTraceRequest.class, "fromNode")).isEmpty();
        assertThat(jsonAliases(ScanTraceRequest.class, "toNode")).isEmpty();
        assertThat(jsonAliases(ScanTraceRequest.class, "eventTime")).isEmpty();
        assertThat(jsonAliases(ScanTraceRequest.class, "correctionOf")).isEmpty();
        assertThat(jsonAliases(TraceCodeActivateRequest.class, "activationNode")).isEmpty();
        assertThat(jsonAliases(TraceCodeActivateRequest.class, "deviceId")).isEmpty();
        assertThat(jsonAliases(TraceCodeActivateRequest.class, "eventTime")).isEmpty();
        assertThat(jsonAliases(TraceCodeActivateRequest.class, "remark")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "nodeCode")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "nodeName")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "nodeType")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "orgId")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "province")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "city")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "address")).isEmpty();
        assertThat(jsonAliases(TraceNodeCreateRequest.class, "enabled")).isEmpty();
        assertThat(jsonAliases(TraceNodeUpdateRequest.class, "nodeName")).isEmpty();
        assertThat(jsonAliases(TraceNodeUpdateRequest.class, "nodeType")).isEmpty();
        assertThat(jsonAliases(TraceNodeUpdateRequest.class, "orgId")).isEmpty();
        assertThat(jsonAliases(TraceNodeUpdateRequest.class, "province")).isEmpty();
        assertThat(jsonAliases(TraceNodeUpdateRequest.class, "city")).isEmpty();
        assertThat(jsonAliases(TraceNodeUpdateRequest.class, "address")).isEmpty();
        assertThat(jsonAliases(TraceNodeUpdateRequest.class, "enabled")).isEmpty();
        assertThat(jsonAliases(TraceUserNodeBindingUpdateRequest.class, "nodeIds")).isEmpty();
        assertThat(jsonAliases(TraceUserNodeBindingUpdateRequest.class, "defaultNodeId")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskCreateRequest.class, "taskNo")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskCreateRequest.class, "taskType")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskCreateRequest.class, "sourceNodeId")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskCreateRequest.class, "targetNodeId")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskCreateRequest.class, "expectedQuantity")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskCompleteRequest.class, "actualQuantity")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskCompleteRequest.class, "discrepancyReason")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskScanRequest.class, "traceCode")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskScanRequest.class, "eventTime")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskScanRequest.class, "idempotencyKey")).isEmpty();
        assertThat(jsonAliases(TraceFlowTaskScanRequest.class, "remark")).isEmpty();
        assertThat(jsonAliases(TraceAggregationBindRequest.class, "parentCode")).isEmpty();
        assertThat(jsonAliases(TraceAggregationBindRequest.class, "childCode")).isEmpty();
        assertThat(jsonAliases(TraceAggregationBindRequest.class, "relationType")).isEmpty();
        assertThat(jsonAliases(TraceAggregationBindRequest.class, "remark")).isEmpty();
        assertThat(jsonAliases(TraceAggregationReleaseRequest.class, "remark")).isEmpty();

        assertThat(jsonAliases(PartCreateRequest.class, "partCode")).containsExactly("partCode");
        assertThat(jsonAliases(PartCreateRequest.class, "partName")).containsExactly("partName");
        assertThat(jsonAliases(PartCreateRequest.class, "partType")).containsExactly("partType");
        assertThat(jsonAliases(UserCreateRequest.class, "roleId")).containsExactly("roleId");
        assertThat(jsonAliases(AssignPermissionsRequest.class, "permissionIds")).containsExactly("permissionIds");
        assertThat(jsonAliases(ResetPasswordRequest.class, "newPassword")).containsExactly("newPassword");
        assertThat(jsonAliases(LoginRequest.class, "rememberMe")).containsExactly("rememberMe");
    }

    private String jsonPropertyValue(Class<?> type, String fieldName) {
        JsonProperty jsonProperty = field(type, fieldName).getAnnotation(JsonProperty.class);
        return jsonProperty == null ? null : jsonProperty.value();
    }

    private List<String> jsonAliases(Class<?> type, String fieldName) {
        JsonAlias jsonAlias = field(type, fieldName).getAnnotation(JsonAlias.class);
        return jsonAlias == null ? List.of() : Arrays.asList(jsonAlias.value());
    }

    private Field field(Class<?> type, String fieldName) {
        try {
            return type.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + type.getSimpleName() + "." + fieldName, e);
        }
    }
}
