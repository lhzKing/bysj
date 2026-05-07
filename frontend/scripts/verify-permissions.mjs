import fs from 'node:fs'
import path from 'node:path'
import * as permissionModule from '../src/shared/constants/permissions.js'

const expectedPermissions = [
  'dashboard:view',
  'user:view',
  'user:manage',
  'role:view',
  'role:manage',
  'part:view',
  'part:manage',
  'trace:view',
  'trace:create',
  'trace:batch:create',
  'trace:code:print',
  'trace:code:activate',
  'trace:scan',
  'trace:inbound',
  'trace:outbound',
  'trace:transfer',
  'trace:task:create',
  'trace:task:scan',
  'trace:task:complete',
  'trace:exception:handle',
  'trace:audit:view'
]

const exportedPermissions = Object.entries(permissionModule)
  .filter(([key, value]) => key !== 'PERMISSIONS' && typeof value === 'string')
  .map(([, value]) => value)
  .sort()

const expectedSorted = [...expectedPermissions].sort()

if (JSON.stringify(exportedPermissions) !== JSON.stringify(expectedSorted)) {
  console.error('Permission constants mismatch')
  console.error('Expected:', expectedSorted)
  console.error('Actual  :', exportedPermissions)
  process.exit(1)
}

const filesToCheck = [
  'src/core/router/index.js',
  'src/shared/components/layout/MainLayout.vue',
  'src/features/trace/views/ScanHub.vue'
].map(file => path.resolve(process.cwd(), file))

const forbiddenPermissions = [
  'user:create', 'user:update', 'user:delete',
  'role:create', 'role:update', 'role:delete',
  'part:create', 'part:update', 'part:delete',
  'trace:update', 'trace:delete', 'sys:admin'
]

const violations = []
for (const file of filesToCheck) {
  const content = fs.readFileSync(file, 'utf8')
  for (const permission of forbiddenPermissions) {
    if (content.includes(permission)) {
      violations.push(`${path.relative(process.cwd(), file)} => ${permission}`)
    }
  }
}

if (violations.length > 0) {
  console.error('Forbidden legacy permissions found:')
  for (const violation of violations) {
    console.error(' -', violation)
  }
  process.exit(1)
}

console.log('Permission contract verified.')
