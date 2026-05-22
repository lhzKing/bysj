import request from '@/core/api/request'

/**
 * 公开追溯查验 API —— 供未登录用户（消费者）通过 Login 页"通过追溯码自助验签"
 * 入口或 /public/traces/:code 路由直接访问。
 *
 * 后端契约：GET /api/public/traces/{code}
 *   - 无需认证 (LoginInterceptor / PermissionInterceptor 已白名单 /api/public/**)
 *   - 返回字段已脱敏：去掉操作员姓名、内部节点 ID/名称、内部 remark
 *   - 保留：trace_code, spu_id, spu_name, current_status, current_province/city,
 *           last_update_time, total_events, events[{event_time, action_label, province, city}],
 *           chain_verify { valid, total_logs, hash_verified_count, signature_verified_count,
 *                          anchor_hash, public_key, errors }
 *
 * @param {string} code 追溯码
 * @returns {Promise<Object>} PublicTraceResponse (camelCase 由 request.js 自动转换)
 */
export function getPublicTrace(code) {
  return request.get(`/public/traces/${encodeURIComponent(code)}`)
}
