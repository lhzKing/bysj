# 自动生成包含当前所有局域网 IP 的 SSL 证书
Write-Host "开始生成开发环境 SSL 证书..." -ForegroundColor Cyan

# 1. 获取所有本机 IPv4 地址
Write-Host "`n检测本机 IP 地址..." -ForegroundColor Yellow
$ipAddresses = Get-NetIPAddress -AddressFamily IPv4 | 
    Where-Object { $_.IPAddress -ne '127.0.0.1' -and $_.PrefixOrigin -ne 'WellKnown' } |
    Select-Object -ExpandProperty IPAddress

Write-Host "检测到以下 IP 地址：" -ForegroundColor Green
$ipAddresses | ForEach-Object { Write-Host "  + $_" -ForegroundColor Green }

# 2. 创建 OpenSSL 配置文件
Write-Host "`n生成 OpenSSL 配置文件..." -ForegroundColor Yellow
$certDir = Join-Path $PSScriptRoot "certs"
if (-not (Test-Path $certDir)) {
    New-Item -ItemType Directory -Path $certDir | Out-Null
}

$configPath = Join-Path $certDir "openssl.cnf"

# 构建 alt_names 配置
$altNames = @(
    "DNS.1 = localhost"
    "DNS.2 = *.localhost"
    "DNS.3 = *.local"
    "IP.1 = 127.0.0.1"
)

$ipIndex = 2
foreach ($ip in $ipAddresses) {
    $altNames += "IP.$ipIndex = $ip"
    $ipIndex++
}

# 生成配置文件内容
$configContent = @"
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C = CN
ST = Beijing
L = Beijing
O = TraceSystem Dev
OU = Development
CN = localhost

[v3_req]
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
$($altNames -join "`n")
"@

$configContent | Out-File -FilePath $configPath -Encoding UTF8 -Force
Write-Host "+ 配置文件已生成: $configPath" -ForegroundColor Green

# 3. 生成证书
Write-Host "`n生成 SSL 证书..." -ForegroundColor Yellow
$keyPath = Join-Path $certDir "localhost.key"
$certPath = Join-Path $certDir "localhost.crt"

# 删除旧证书（如果存在）
if (Test-Path $keyPath) { Remove-Item $keyPath -Force }
if (Test-Path $certPath) { Remove-Item $certPath -Force }

# 生成新证书
$null = & openssl req -new -x509 -newkey rsa:2048 -sha256 -nodes `
    -keyout $keyPath -days 3650 -out $certPath `
    -config $configPath -extensions v3_req 2>&1

if ($LASTEXITCODE -eq 0 -and (Test-Path $certPath)) {
    Write-Host "+ 证书生成成功！" -ForegroundColor Green
    Write-Host "  私钥: $keyPath" -ForegroundColor Gray
    Write-Host "  证书: $certPath" -ForegroundColor Gray
} else {
    Write-Host "x 证书生成失败！" -ForegroundColor Red
    exit 1
}

# 4. 验证证书内容
Write-Host "`n证书包含的域名和 IP：" -ForegroundColor Cyan
$certText = & openssl x509 -in $certPath -text -noout 2>&1 | Out-String
if ($certText -match "DNS:([^\n]+)") {
    $dnsEntries = $matches[1] -split ',' | ForEach-Object { $_.Trim() }
    $dnsEntries | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
}

# 5. 使用说明
Write-Host "`n=== 证书生成完成！===" -ForegroundColor Green
Write-Host "`n手机测试访问地址（任选其一）：" -ForegroundColor Cyan
foreach ($ip in $ipAddresses) {
    Write-Host "  https://$($ip):5173/" -ForegroundColor Yellow
}

Write-Host "`n下一步操作：" -ForegroundColor Yellow
Write-Host "  1. 重启 Vite 开发服务器: npm run dev" -ForegroundColor White
Write-Host "  2. 手机访问上述任一地址" -ForegroundColor White
Write-Host "  3. 点击 高级 -> 继续 接受证书（首次）" -ForegroundColor White
Write-Host ""
