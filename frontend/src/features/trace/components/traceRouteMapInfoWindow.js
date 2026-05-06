const TYPE_LABELS = {
  start: '🏭 起点',
  end: '📍 当前位置',
  transit: '📦 途经节点'
}

const applyStyles = (element, styles) => {
  Object.assign(element.style, styles)
  return element
}

export const getRoutePointTypeLabel = ({ isStart, isEnd }) => {
  if (isStart) return TYPE_LABELS.start
  if (isEnd) return TYPE_LABELS.end
  return TYPE_LABELS.transit
}

export const formatTraceRouteTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN')
}

export const createTraceRouteInfoWindowContent = ({ name, time, isStart, isEnd }) => {
  const container = applyStyles(document.createElement('div'), {
    padding: '8px',
    minWidth: '150px'
  })

  const title = applyStyles(document.createElement('div'), {
    fontWeight: 'bold',
    marginBottom: '4px'
  })
  title.textContent = name || '未知节点'

  const type = applyStyles(document.createElement('div'), {
    fontSize: '12px',
    color: '#666'
  })
  type.textContent = getRoutePointTypeLabel({ isStart, isEnd })

  const timeElement = applyStyles(document.createElement('div'), {
    fontSize: '11px',
    color: '#999',
    marginTop: '4px'
  })
  timeElement.textContent = formatTraceRouteTime(time)

  container.append(title, type, timeElement)
  return container
}
