package com.example.trace.dto;

import lombok.Data;

@Data
public class PageRequest {

    private Integer page = 1;
    private Integer size = 10;
    private String sort;
    private String order = "desc";

    public boolean isAsc() {
        return "asc".equalsIgnoreCase(order);
    }
}
