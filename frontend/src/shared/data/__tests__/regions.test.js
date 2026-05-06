import { describe, expect, it } from 'vitest'
import {
  REGIONS,
  NODE_REGION_MAP,
  findRegionByValue,
  getCitiesByProvince,
  getRegionByNode
} from '@/shared/data/regions'

describe('shared/data/regions', () => {
  it('keeps province dictionary available through the compatibility entry', () => {
    expect(REGIONS.length).toBeGreaterThan(0)
    expect(findRegionByValue(REGIONS[0].value)).toEqual(REGIONS[0])
    expect(getCitiesByProvince(REGIONS[0].value)).toEqual(REGIONS[0].cities)
  })

  it('keeps node-region preset lookup behavior stable', () => {
    expect(NODE_REGION_MAP['杭州工厂']).toEqual({
      province: '浙江省',
      city: '杭州市'
    })
    expect(getRegionByNode('杭州工厂')).toEqual({
      province: '浙江省',
      city: '杭州市'
    })
    expect(getRegionByNode('杭州分拨中心')).toEqual({
      province: '浙江省',
      city: '杭州市'
    })
  })
})
