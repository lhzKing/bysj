// 生成自签名 SSL 证书用于开发
import { execSync } from 'child_process'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const certDir = path.join(__dirname, '.cert')
const keyPath = path.join(certDir, 'key.pem')
const certPath = path.join(certDir, 'cert.pem')

// 确保证书目录存在
if (!fs.existsSync(certDir)) {
  fs.mkdirSync(certDir, { recursive: true })
}

// 检查证书是否已存在
if (fs.existsSync(keyPath) && fs.existsSync(certPath)) {
  console.log('✅ SSL 证书已存在')
  process.exit(0)
}

try {
  console.log('正在生成 SSL 证书...')
  
  // 使用 OpenSSL 生成自签名证书（Windows 自带）
  const command = `openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 365 ` +
    `-keyout "${keyPath}" -out "${certPath}" ` +
    `-subj "/C=CN/ST=Beijing/L=Beijing/O=Dev/OU=Dev/CN=localhost"`
  
  execSync(command, { stdio: 'inherit' })
  
  console.log('✅ SSL 证书生成成功！')
  console.log(`   密钥文件: ${keyPath}`)
  console.log(`   证书文件: ${certPath}`)
} catch (error) {
  console.error('❌ 生成证书失败:', error.message)
  console.log('\n手动方案：')
  console.log('1. 去掉 vite.config.js 中的 https: true')
  console.log('2. 使用 HTTP 访问（手机需要特殊配置）')
  process.exit(1)
}
