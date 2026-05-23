/**
 * 单品码（trace_code）的可执行动作判定。
 *
 * 这套规则与 TraceAssignmentWorkbench 的行内按钮、TraceDetail 的标签/QR
 * 入口共享同一份判断逻辑，避免两处状态机漂移。
 */

/**
 * GENERATED 是唯一允许首次打印的状态——避免把已打印 / 已激活的码再次入"未打印"循环。
 * @param {{ codeStatus?: string }|null} code
 * @returns {boolean}
 */
export function canPrint(code) {
  return code?.codeStatus === 'GENERATED'
}

/**
 * 终态（VOIDED / SCRAPPED）的码不允许重打——重打需要补登链事件，已作废 / 已报废链不可再写。
 * @param {{ codeStatus?: string }|null} code
 * @returns {boolean}
 */
export function canReprint(code) {
  return Boolean(code) && code.codeStatus !== 'VOIDED' && code.codeStatus !== 'SCRAPPED'
}

/**
 * 仅未激活的码（GENERATED / PRINTED）可作废。
 * @param {{ codeStatus?: string }|null} code
 * @returns {boolean}
 */
export function canVoid(code) {
  return Boolean(code) && ['GENERATED', 'PRINTED'].includes(code.codeStatus)
}

/**
 * 仅未激活的码（GENERATED / PRINTED）可激活。
 * @param {{ codeStatus?: string }|null} code
 * @returns {boolean}
 */
export function canActivate(code) {
  return Boolean(code) && ['GENERATED', 'PRINTED'].includes(code.codeStatus)
}
