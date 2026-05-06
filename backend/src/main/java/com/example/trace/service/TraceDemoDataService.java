package com.example.trace.service;

import java.util.Map;

public interface TraceDemoDataService {

    Map<String, Object> generateSampleData(int count, String operator, String operatorRole);

    Map<String, Object> clearTraceData(String operator, String operatorRole);
}
