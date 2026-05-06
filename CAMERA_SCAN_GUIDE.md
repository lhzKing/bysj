# 工业溯源系统 - 摄像头扫码集成方案

> 更新日期：2026-01-21  
> 状态：✅ 已验证可用

---

## 📋 目录

1. [HTTPS 开发环境配置](#https-开发环境配置)
2. [摄像头测试页面](#摄像头测试页面)
3. [QR 扫码集成方案](#qr-扫码集成方案)
4. [生产环境部署](#生产环境部署)

---

## HTTPS 开发环境配置

### 为什么需要 HTTPS？

浏览器安全策略要求：**只有 HTTPS 或 localhost 才能访问摄像头/麦克风**。  
在局域网内测试手机扫码功能时，必须启用 HTTPS。

### 安装依赖

```bash
cd frontend
npm install --save-dev --legacy-peer-deps @vitejs/plugin-basic-ssl
```

> 使用 `--legacy-peer-deps` 绕过 Vite 5/6 版本依赖冲突

### Vite 配置

**文件：** `frontend/vite.config.js`

```javascript
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig({
  plugins: [
    vue(),
    basicSsl() // 启用 HTTPS 自签名证书
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0', // 允许局域网设备访问
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

### 启动和访问

1. **启动开发服务器：**
   ```bash
   cd frontend
   npm run dev
   ```

2. **查看控制台输出：**
   ```
   ➜  Local:   https://localhost:5173/
   ➜  Network: https://10.100.97.51:5173/
   ```

3. **访问地址：**
   - **电脑**：`https://localhost:5173`
   - **手机**：`https://你的IP:5173`（例如 `https://10.100.97.51:5173`）

4. **处理证书警告：**
   - **Chrome/Edge**：点击"高级" → "继续访问"
   - **iOS Safari**：点击"显示详细信息" → "访问此网站"
   - ⚠️ 这是正常的，因为使用的是自签名证书

---

## 摄像头测试页面

### 文件结构

**文件：** `frontend/src/views/CameraTest.vue`

### 功能特性

- ✅ 开启/关闭摄像头
- ✅ 切换前后摄像头（手机适用）
- ✅ 实时视频预览（1080p）
- ✅ 拍照功能（JPEG 格式）
- ✅ 设备信息显示（分辨率、摄像头数量）
- ✅ 错误提示（权限拒绝、设备不可用）

### 路由配置

**文件：** `frontend/src/core/router/index.js`

```javascript
{
  path: '/camera-test',
  name: 'camera-test',
  component: () => import('@/views/CameraTest.vue'),
  meta: { title: '摄像头测试' }
}
```

### 核心代码

```javascript
// 请求摄像头权限
const constraints = {
  video: {
    facingMode: 'environment', // 'environment' 后置, 'user' 前置
    width: { ideal: 1920 },
    height: { ideal: 1080 }
  },
  audio: false
}

// 启动摄像头
const stream = await navigator.mediaDevices.getUserMedia(constraints)
videoElement.value.srcObject = stream
```

### 访问测试

```
https://localhost:5173/camera-test          # 电脑
https://10.100.97.51:5173/camera-test       # 手机（替换为实际 IP）
```

---

## QR 扫码集成方案

### 推荐库：vue-qrcode-reader

**优势：**
- Vue 3 原生支持
- 自动处理摄像头权限
- 支持前后摄像头切换
- 内置扫码算法（无需额外依赖 jsQR）
- 持续扫描或单次扫描模式
- 扫描框引导线

### 安装

```bash
cd frontend
npm install vue-qrcode-reader
```

### 1. 创建扫码组件

**文件：** `frontend/src/components/QRScanner.vue`

```vue
<template>
  <div class="qr-scanner-wrapper">
    <!-- 头部 -->
    <div class="scanner-header">
      <h2>扫描溯源码</h2>
      <button @click="$emit('close')" class="close-btn">✕</button>
    </div>
    
    <!-- 扫码区域 -->
    <qrcode-stream
      @detect="onDetect"
      @error="onError"
      @camera-on="onCameraReady"
      :track="paintBoundingBox"
      :camera="camera"
    >
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>摄像头加载中...</p>
      </div>
    </qrcode-stream>
    
    <!-- 底部工具栏 -->
    <div class="scanner-footer">
      <button @click="switchCamera" class="switch-btn">
        <span>🔄</span>
        切换摄像头 ({{ camera === 'rear' ? '后置' : '前置' }})
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { QrcodeStream } from 'vue-qrcode-reader'

const emit = defineEmits(['scan', 'close'])

// 状态
const loading = ref(true)
const camera = ref('rear') // 'rear' 后置, 'front' 前置

// 扫码成功回调
const onDetect = (detectedCodes) => {
  const code = detectedCodes[0]?.rawValue
  if (code) {
    console.log('扫描成功:', code)
    emit('scan', code)
  }
}

// 错误处理
const onError = (error) => {
  console.error('扫码错误:', error)
  let message = '摄像头访问失败'
  
  if (error.name === 'NotAllowedError') {
    message = '您拒绝了摄像头权限，请在浏览器设置中允许访问'
  } else if (error.name === 'NotFoundError') {
    message = '未找到摄像头设备'
  } else if (error.name === 'NotReadableError') {
    message = '摄像头被其他程序占用'
  }
  
  alert(message)
}

// 摄像头就绪
const onCameraReady = () => {
  loading.value = false
}

// 切换前后摄像头
const switchCamera = () => {
  camera.value = camera.value === 'rear' ? 'front' : 'rear'
}

// 绘制扫描框（绿色矩形）
const paintBoundingBox = (detectedCodes, ctx) => {
  for (const detectedCode of detectedCodes) {
    const { boundingBox } = detectedCode
    
    // 绿色扫描框
    ctx.strokeStyle = '#00ff00'
    ctx.lineWidth = 4
    ctx.strokeRect(
      boundingBox.x,
      boundingBox.y,
      boundingBox.width,
      boundingBox.height
    )
    
    // 可选：添加扫描框四角
    const cornerLength = 20
    ctx.lineWidth = 6
    
    // 左上角
    ctx.beginPath()
    ctx.moveTo(boundingBox.x, boundingBox.y + cornerLength)
    ctx.lineTo(boundingBox.x, boundingBox.y)
    ctx.lineTo(boundingBox.x + cornerLength, boundingBox.y)
    ctx.stroke()
    
    // 右上角
    ctx.beginPath()
    ctx.moveTo(boundingBox.x + boundingBox.width - cornerLength, boundingBox.y)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y + cornerLength)
    ctx.stroke()
    
    // 左下角
    ctx.beginPath()
    ctx.moveTo(boundingBox.x, boundingBox.y + boundingBox.height - cornerLength)
    ctx.lineTo(boundingBox.x, boundingBox.y + boundingBox.height)
    ctx.lineTo(boundingBox.x + cornerLength, boundingBox.y + boundingBox.height)
    ctx.stroke()
    
    // 右下角
    ctx.beginPath()
    ctx.moveTo(boundingBox.x + boundingBox.width - cornerLength, boundingBox.y + boundingBox.height)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height)
    ctx.lineTo(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height - cornerLength)
    ctx.stroke()
  }
}
</script>

<style scoped>
.qr-scanner-wrapper {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: black;
  display: flex;
  flex-direction: column;
}

.scanner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  backdrop-filter: blur(10px);
}

.scanner-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  font-size: 24px;
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 16px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading p {
  font-size: 16px;
  margin: 0;
}

.scanner-footer {
  padding: 1rem;
  background: rgba(0, 0, 0, 0.8);
  text-align: center;
  backdrop-filter: blur(10px);
}

.switch-btn {
  padding: 12px 24px;
  background: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.switch-btn:hover {
  background: #f0f0f0;
  transform: scale(1.05);
}

.switch-btn:active {
  transform: scale(0.95);
}
</style>
```

---

### 2. 在页面中使用

**示例：** 在溯源管理页面添加扫码功能

**文件：** `frontend/src/features/trace/views/TraceList.vue`

```vue
<template>
  <div>
    <!-- 原有的溯源管理界面 -->
    <div class="page-header">
      <h1>溯源管理</h1>
      <button @click="showScanner = true" class="scan-btn">
        📷 扫码流转
      </button>
    </div>
    
    <!-- 扫码弹窗 -->
    <QRScanner
      v-if="showScanner"
      @scan="handleScan"
      @close="showScanner = false"
    />
    
    <!-- 其他内容 -->
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import QRScanner from '@/components/QRScanner.vue'
import { addTraceEvent } from '@/features/trace/api'
import { useToast } from '@/shared/composables/useToast'

const router = useRouter()
const toast = useToast()
const showScanner = ref(false)

// 处理扫码结果
const handleScan = async (traceCode) => {
  try {
    // 关闭扫码器
    showScanner.value = false
    
    console.log('扫描到溯源码:', traceCode)
    
    // 调用后端 API 记录流转事件
    await addTraceEvent(traceCode, {
      action: 'OUTBOUND',
      location: '仓库',
      remark: '扫码出库'
    })
    
    toast.success(`溯源码 ${traceCode} 流转记录成功`)
    
    // 可选：跳转到详情页
    router.push(`/traces/${traceCode}`)
  } catch (error) {
    console.error('流转记录失败:', error)
    toast.error('流转记录失败: ' + error.message)
    
    // 失败后重新打开扫码（可选）
    // showScanner.value = true
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.scan-btn {
  padding: 12px 24px;
  background: #1C1917;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.scan-btn:hover {
  background: #000;
}

.scan-btn:active {
  transform: scale(0.98);
}
</style>
```

---

### 3. API 接口对接

**文件：** `frontend/src/features/trace/api.js`

```javascript
import request from '@/shared/utils/request'

/**
 * 添加溯源流转事件
 * @param {string} traceCode - 溯源码
 * @param {object} data - 流转数据
 */
export function addTraceEvent(traceCode, data) {
  return request.post(`/traces/${traceCode}/events`, data)
}
```

**后端接口示例：**
```
POST /api/traces/{traceCode}/events
Content-Type: application/json
Authorization: Bearer <token>

{
  "action": "OUTBOUND",
  "location": "仓库",
  "remark": "扫码出库"
}
```

---

### 4. 完整工作流程

```
用户点击"扫码流转"
    ↓
打开 QRScanner 组件（全屏）
    ↓
请求摄像头权限（首次）
    ↓
用户对准二维码
    ↓
识别成功（绿色框）
    ↓
触发 @scan 事件，传递溯源码
    ↓
关闭扫码器
    ↓
调用后端 API 记录流转
    ↓
成功：显示成功提示 + 跳转详情页
失败：显示错误提示 + 重新扫码
```

---

## 生产环境部署

### 当前方案（自签名证书）

**适用场景：** 个人项目、小团队、内网使用

**特点：**
- ✅ 手机扫码正常
- ✅ HTTPS 加密传输
- ✅ 零成本
- ⚠️ 每次访问需要"接受证书"

**使用限制：**
- 不适合公开访问
- 浏览器会显示"不安全"警告
- 每次证书更新需要重新接受

---

### 未来升级方案（正式 SSL 证书）

**步骤：**

1. **购买域名**（例如 `trace.yourdomain.com`）

2. **申请 SSL 证书**
   - **免费方案：** Let's Encrypt（推荐）
   - **付费方案：** 阿里云、腾讯云、DigiCert

3. **修改 Vite 配置**

```javascript
import fs from 'fs'
import path from 'path'

export default defineConfig({
  server: {
    https: {
      key: fs.readFileSync('/path/to/privkey.pem'),
      cert: fs.readFileSync('/path/to/fullchain.pem')
    },
    host: '0.0.0.0',
    port: 443 // HTTPS 默认端口
  }
})
```

4. **Nginx 反向代理配置**

```nginx
server {
    listen 443 ssl http2;
    server_name trace.yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/trace.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/trace.yourdomain.com/privkey.pem;
    
    # 安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # 前端静态文件
    location / {
        root /var/www/trace-frontend/dist;
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理到后端
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name trace.yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 📝 完整文件清单

### 已创建文件

```
frontend/
├── vite.config.js                          # ✅ HTTPS 配置
├── src/
│   ├── views/
│   │   └── CameraTest.vue                  # ✅ 摄像头测试页面
│   └── core/
│       └── router/
│           └── index.js                    # ✅ 路由配置（添加 /camera-test）
```

### 待创建文件（扫码功能）

```
frontend/
└── src/
    ├── components/
    │   └── QRScanner.vue                   # 📦 QR 扫码组件
    └── features/
        └── trace/
            ├── views/
            │   └── TraceList.vue           # 🔧 修改：添加扫码按钮
            └── api/
                └── index.js                # 🔧 修改：添加 addTraceEvent API
```

---

## 🎯 快速开始检查清单

- [x] 安装 `@vitejs/plugin-basic-ssl`
- [x] 配置 `vite.config.js` 启用 HTTPS
- [x] 创建摄像头测试页面 `CameraTest.vue`
- [x] 添加测试路由 `/camera-test`
- [x] 验证电脑和手机访问 HTTPS
- [ ] 安装 `vue-qrcode-reader`
- [ ] 创建 `QRScanner.vue` 扫码组件
- [ ] 在溯源页面集成扫码功能
- [ ] 对接后端流转事件 API
- [ ] 测试完整扫码流转流程

---

## ⚠️ 常见问题

### Q1: 手机访问时提示"不安全"怎么办？
**A:** 点击"高级"或"详细信息" → "继续访问"。自签名证书会有此警告，属于正常现象。

### Q2: 摄像头权限被拒绝？
**A:** 检查浏览器设置：
- **Chrome**：设置 → 隐私和安全 → 网站设置 → 摄像头 → 允许
- **Safari**：设置 → Safari → 摄像头与麦克风 → 询问或允许

### Q3: 扫码识别不了二维码？
**A:** 确保：
1. 光线充足
2. 二维码清晰无遮挡
3. 摄像头对焦正确
4. 使用后置摄像头（切换按钮）

### Q4: 局域网内其他设备访问不了？
**A:** 检查：
1. 防火墙是否开放 5173 端口
2. 设备是否在同一 WiFi
3. IP 地址是否正确（`ipconfig` 查看）

---

## 📚 参考资料

- [Vite HTTPS 配置文档](https://cn.vitejs.dev/config/server-options.html#server-https)
- [vue-qrcode-reader 官方文档](https://gruhn.github.io/vue-qrcode-reader/)
- [MediaDevices API - MDN](https://developer.mozilla.org/zh-CN/docs/Web/API/MediaDevices)
- [Let's Encrypt 免费证书](https://letsencrypt.org/zh-cn/)

---

## 📞 支持

如有问题，请参考：
1. 本文档常见问题章节
2. 浏览器控制台错误信息
3. 后端 API 日志

**项目状态：** ✅ 开发环境已完成，生产环境待部署

**下一步：** 安装 `vue-qrcode-reader` 并创建扫码组件
