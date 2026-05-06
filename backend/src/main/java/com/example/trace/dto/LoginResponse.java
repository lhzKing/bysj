package com.example.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    
    /**
     * 用户权限代码列表（用于前端控制按钮显示）
     */
    private List<String> permissions;
}
