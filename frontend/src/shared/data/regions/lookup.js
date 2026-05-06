import { NODE_REGION_MAP } from './node-region-map.js'

export function getRegionByNode(nodeName) {
  if (!nodeName) return null

  if (NODE_REGION_MAP[nodeName]) {
    return NODE_REGION_MAP[nodeName]
  }

  for (const [key, value] of Object.entries(NODE_REGION_MAP)) {
    if (nodeName.includes(key.substring(0, 2))) {
      return value
    }
  }

  return null
}
