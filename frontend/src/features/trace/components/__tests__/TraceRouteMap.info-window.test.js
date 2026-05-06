import { describe, expect, it } from 'vitest'
import {
  createTraceRouteInfoWindowContent,
  formatTraceRouteTime,
  getRoutePointTypeLabel
} from '@/features/trace/components/traceRouteMapInfoWindow'

describe('TraceRouteMap info window content', () => {
  it('renders route point fields as text content instead of executable HTML', () => {
    const maliciousName = '<img src=x onerror="window.__xss = true">恶意节点'

    const content = createTraceRouteInfoWindowContent({
      name: maliciousName,
      time: '2026-05-02T09:30:00',
      isStart: false,
      isEnd: true
    })

    expect(content.textContent).toContain(maliciousName)
    expect(content.textContent).toContain('📍 当前位置')
    expect(content.querySelector('img')).toBeNull()
    expect(content.innerHTML).toContain('&lt;img')
    expect(content.innerHTML).not.toContain('<img')
  })

  it('keeps route point labels and invalid time formatting deterministic', () => {
    expect(getRoutePointTypeLabel({ isStart: true, isEnd: false })).toBe('🏭 起点')
    expect(getRoutePointTypeLabel({ isStart: false, isEnd: true })).toBe('📍 当前位置')
    expect(getRoutePointTypeLabel({ isStart: false, isEnd: false })).toBe('📦 途经节点')
    expect(formatTraceRouteTime('not-a-date')).toBe('')
  })
})
