package com.example.trace.service.impl.support;

import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.util.HashUtil;
import com.example.trace.util.SignatureUtil;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceChainVerifyServiceTest {

    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private SignatureUtil signatureUtil;

    @Test
    void verify_shouldReportHashMismatchWhenLogTampered() {
        TraceChainVerifyService service = new TraceChainVerifyService(traceLifecycleLogMapper, signatureUtil);

        TraceLifecycleLog log = baseLog();
        log.setCurrentHash("tampered-hash");
        log.setSignature("signed");

        when(traceLifecycleLogMapper.selectFullChain("trace-1")).thenReturn(List.of(log));
        when(signatureUtil.supportsKey("default", 1)).thenReturn(true);
        when(signatureUtil.verify(any(), eq("signed"), eq("default"), eq(1))).thenReturn(true);

        ChainVerifyResponse response = service.verify("trace-1");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors())
                .extracting(ChainVerifyResponse.VerifyError::getErrorType)
                .contains("HASH_MISMATCH");
    }

    @Test
    void verify_shouldReportUnavailableKeyWhenLogKeyMetadataDoesNotMatchLoadedKey() {
        TraceChainVerifyService service = new TraceChainVerifyService(traceLifecycleLogMapper, signatureUtil);

        TraceLifecycleLog log = baseLog();
        log.setSignature("signed");
        log.setSignatureKeyId("old-key");
        log.setSignatureKeyVersion(2);

        when(traceLifecycleLogMapper.selectFullChain("trace-1")).thenReturn(List.of(log));
        when(signatureUtil.supportsKey("old-key", 2)).thenReturn(false);
        when(signatureUtil.getKeyId()).thenReturn("default");
        when(signatureUtil.getKeyVersion()).thenReturn(1);

        ChainVerifyResponse response = service.verify("trace-1");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors())
                .extracting(ChainVerifyResponse.VerifyError::getErrorType)
                .contains("SIGNATURE_KEY_UNAVAILABLE");
        assertThat(response.getErrors().get(0).getSignatureKeyId()).isEqualTo("old-key");
        assertThat(response.getErrors().get(0).getSignatureKeyVersion()).isEqualTo(2);
    }

    @Test
    void verify_shouldIncludeRemarkInSignaturePayload() {
        TraceChainVerifyService service = new TraceChainVerifyService(traceLifecycleLogMapper, signatureUtil);

        TraceLifecycleLog log = baseLog();
        log.setSignature("signed");

        when(traceLifecycleLogMapper.selectFullChain("trace-1")).thenReturn(List.of(log));
        when(signatureUtil.supportsKey("default", 1)).thenReturn(true);
        when(signatureUtil.verify(any(), eq("signed"), eq("default"), eq(1))).thenReturn(true);

        ChainVerifyResponse response = service.verify("trace-1");

        assertThat(response.isValid()).isTrue();
        ArgumentCaptor<String> signatureDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(signatureUtil).verify(signatureDataCaptor.capture(), eq("signed"), eq("default"), eq(1));
        assertThat(signatureDataCaptor.getValue()).contains("remark=sealed package");
        assertThat(signatureDataCaptor.getValue()).contains("operator=scanner");
    }

    @Test
    void verify_shouldReportHashMismatchWhenOperatorTamperedOnProtectedLog() {
        TraceChainVerifyService service = new TraceChainVerifyService(traceLifecycleLogMapper, signatureUtil);

        TraceLifecycleLog log = baseLog();
        log.setOperator("attacker");
        log.setSignature("signed");

        when(traceLifecycleLogMapper.selectFullChain("trace-1")).thenReturn(List.of(log));
        when(signatureUtil.supportsKey("default", 1)).thenReturn(true);
        when(signatureUtil.verify(any(), eq("signed"), eq("default"), eq(1))).thenReturn(true);

        ChainVerifyResponse response = service.verify("trace-1");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors())
                .extracting(ChainVerifyResponse.VerifyError::getErrorType)
                .contains("HASH_MISMATCH");
    }

    @Test
    void verify_shouldAcceptLegacyHashAndSignaturePayloadForPreB06Logs() {
        TraceChainVerifyService service = new TraceChainVerifyService(traceLifecycleLogMapper, signatureUtil);

        TraceLifecycleLog log = legacyLog();
        log.setSignature("signed");

        when(traceLifecycleLogMapper.selectFullChain("trace-1")).thenReturn(List.of(log));
        when(signatureUtil.supportsKey("default", 1)).thenReturn(true);
        when(signatureUtil.verify(any(), eq("signed"), eq("default"), eq(1))).thenReturn(false, true);

        ChainVerifyResponse response = service.verify("trace-1");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getHashVerifiedCount()).isEqualTo(1);
        assertThat(response.getSignatureVerifiedCount()).isEqualTo(1);

        ArgumentCaptor<String> signatureDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(signatureUtil, times(2)).verify(signatureDataCaptor.capture(), eq("signed"), eq("default"), eq(1));
        assertThat(signatureDataCaptor.getAllValues().get(0)).contains("operator=scanner");
        assertThat(signatureDataCaptor.getAllValues().get(1)).doesNotContain("operator=");
    }

    private static TraceLifecycleLog baseLog() {
        TraceLifecycleLog log = new TraceLifecycleLog();
        log.setId(1L);
        log.setTraceCode("trace-1");
        log.setActionType(ActionType.INIT.getCode());
        log.setToNode("factory-A");
        log.setProvince("guangdong");
        log.setCity("shenzhen");
        log.setRemark("sealed package");
        log.setOperator("scanner");
        log.setEventTime(LocalDateTime.of(2026, 4, 9, 10, 0));
        log.setIngestTime(LocalDateTime.of(2026, 4, 9, 10, 0));
        log.setPrevHash("GENESIS");
        log.setCorrectionOf(null);
        log.setCurrentHash(HashUtil.calculateHash(
                log.getTraceCode(),
                log.getActionType(),
                log.getFromNode(),
                log.getToNode(),
                log.getProvince(),
                log.getCity(),
                log.getRemark(),
                log.getEventTime(),
                log.getIngestTime(),
                log.getPrevHash(),
                log.getCorrectionOf(),
                log.getOperator()
        ));
        log.setSignatureKeyId("default");
        log.setSignatureKeyVersion(1);
        return log;
    }

    private static TraceLifecycleLog legacyLog() {
        TraceLifecycleLog log = baseLog();
        log.setCurrentHash(HashUtil.calculateLegacyHash(
                log.getTraceCode(),
                log.getActionType(),
                log.getFromNode(),
                log.getToNode(),
                log.getProvince(),
                log.getCity(),
                log.getRemark(),
                log.getEventTime(),
                log.getIngestTime(),
                log.getPrevHash(),
                log.getCorrectionOf()
        ));
        return log;
    }
}
