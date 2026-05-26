package com.example.trace.service.impl.support;

/**
 * Light-weight reference to a demo user (id + username) used by the
 * demo-data factories when they need to populate {@code create_by} or
 * {@code operator_user_id} columns without dragging in the full
 * {@link com.example.trace.entity.SysUser} entity.
 */
public record DemoUserRef(Long id, String username) {
}
