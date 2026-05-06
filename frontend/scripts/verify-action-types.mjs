import * as actionTypeModule from '../src/shared/constants/actionTypes.js'

const expectedActionTypes = ['INIT', 'INBOUND', 'OUTBOUND', 'TRANSFER', 'EXCEPTION', 'CORRECTION']
const actualActionTypes = Object.values(actionTypeModule.ACTION_TYPES || {}).sort()
const expectedSorted = [...expectedActionTypes].sort()

if (JSON.stringify(actualActionTypes) !== JSON.stringify(expectedSorted)) {
  console.error('Action type constants mismatch')
  console.error('Expected:', expectedSorted)
  console.error('Actual  :', actualActionTypes)
  process.exit(1)
}

const labelKeys = Object.keys(actionTypeModule.ACTION_TYPE_LABELS || {}).sort()
if (JSON.stringify(labelKeys) !== JSON.stringify(expectedSorted)) {
  console.error('Action type labels mismatch')
  console.error('Expected:', expectedSorted)
  console.error('Actual  :', labelKeys)
  process.exit(1)
}

const colorKeys = Object.keys(actionTypeModule.ACTION_TYPE_COLORS || {}).sort()
if (JSON.stringify(colorKeys) !== JSON.stringify(expectedSorted)) {
  console.error('Action type colors mismatch')
  console.error('Expected:', expectedSorted)
  console.error('Actual  :', colorKeys)
  process.exit(1)
}

console.log('Action type contract verified.')
