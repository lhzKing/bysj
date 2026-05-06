package com.example.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceUserNodeBindingResponse {

    private Long bindingId;
    private Long userId;
    private Long nodeId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private Long orgId;
    private String province;
    private String city;
    private String address;
    private Boolean nodeEnabled;
    private Boolean bindingEnabled;
    private Boolean defaultNode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
