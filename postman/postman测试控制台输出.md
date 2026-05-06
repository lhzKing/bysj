POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "9a893873-8b81-486e-84b4-6ffb213b794a",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "58"
  },
  "Request Body": "{\"username\": \"superadmin\", \"password\": \"superadmin123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:02 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"登录成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhMTc5ZmQ5NC05MDI4LTQyM2QtYTQ5NS03MmMzNGRjNTNlMGIiLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTAyLCJleHAiOjE3NzAwNDk1MDJ9.T6Z1Ab20ydkXlflBi-UuasFnn1Mu2NBLG6sWHLafuNc\",\"username\":\"superadmin\",\"role\":\"SUPER_ADMIN\",\"permissions\":[\"user:view\",\"role:manage\",\"trace:create\",\"dashboard:view\",\"trace:view\",\"role:view\",\"user:manage\",\"part:manage\",\"trace:scan\",\"part:view\"]}}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "1142ee71-3c93-4e48-a71e-db163e215244",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "48"
  },
  "Request Body": "{\"username\": \"admin\", \"password\": \"admin123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:02 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"登录成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ\",\"username\":\"admin\",\"role\":\"ADMIN\",\"permissions\":[\"user:view\",\"role:manage\",\"trace:create\",\"dashboard:view\",\"trace:view\",\"role:view\",\"user:manage\",\"part:manage\",\"trace:scan\",\"part:view\"]}}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "666a0c40-9bcd-403f-a83f-391b5fb40efd",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "54"
  },
  "Request Body": "{\"username\": \"producer\", \"password\": \"producer123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:02 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"登录成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJiMWMwODU3ZS01ZDcyLTQ5NTMtOTgxMy04MzRiMGYwOTg0MmEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.bsK6S9cC6T6Ekf7XcJsnkZgQmPuiA0kMzNS_fe6F_68\",\"username\":\"producer\",\"role\":\"PRODUCER\",\"permissions\":[\"trace:create\",\"dashboard:view\",\"trace:view\",\"part:view\"]}}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "8fd616cf-00be-4e9a-b23a-72104a27c2ca",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "56"
  },
  "Request Body": "{\"username\": \"warehouse\", \"password\": \"warehouse123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:02 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"登录成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI0N2VhNmYxNS00NTgyLTQ4MjQtOTA0NS0zY2Y4NzRjNTFlNTEiLCJzdWIiOiJ3YXJlaG91c2UiLCJ1c2VybmFtZSI6IndhcmVob3VzZSIsInJvbGUiOiJXQVJFSE9VU0UiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.RYhDSw8l4VhMGRXtDdb-83niHINH5q0tXl60dDRtVsI\",\"username\":\"warehouse\",\"role\":\"WAREHOUSE\",\"permissions\":[\"dashboard:view\",\"trace:view\",\"trace:inbound\",\"trace:scan\",\"part:view\",\"trace:outbound\"]}}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "8779fb98-dba5-4fc6-918b-7293739e01e6",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "56"
  },
  "Request Body": "{\"username\": \"logistics\", \"password\": \"logistics123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:02 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"登录成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJmOWI2MGIzYi1hNDAzLTRhYmEtOTY2OS04NGZkZTc1NTIyZTkiLCJzdWIiOiJsb2dpc3RpY3MiLCJ1c2VybmFtZSI6ImxvZ2lzdGljcyIsInJvbGUiOiJMT0dJU1RJQ1MiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.nHqUuLqLx7ePrJcfiOVZGkiBbYstACIRUyyS7BP2zuY\",\"username\":\"logistics\",\"role\":\"LOGISTICS\",\"permissions\":[\"dashboard:view\",\"trace:view\",\"trace:transfer\",\"trace:scan\"]}}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "07b07d25-70b5-44bd-9a77-c379b28315d5",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "46"
  },
  "Request Body": "{\"username\": \"user\", \"password\": \"user123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:02 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"登录成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM\",\"username\":\"user\",\"role\":\"USER\",\"permissions\":[\"dashboard:view\",\"trace:view\"]}}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "e02b65d5-59fd-43a5-a99c-127222808ee9",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "50"
  },
  "Request Body": "{\"username\": \"admin\", \"password\": \"wrongpassword\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:03 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":11002,\"status\":401,\"message\":\"用户名或密码错误\"}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "691ab518-a96a-4026-862e-08341a94fa56",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "60"
  },
  "Request Body": "{\"username\": \"notexistuser12345\", \"password\": \"anypassword\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:03 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":11002,\"status\":401,\"message\":\"用户名或密码错误\"}"
}
GET http://localhost:8080/api/auth/me: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhMTc5ZmQ5NC05MDI4LTQyM2QtYTQ5NS03MmMzNGRjNTNlMGIiLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTAyLCJleHAiOjE3NzAwNDk1MDJ9.T6Z1Ab20ydkXlflBi-UuasFnn1Mu2NBLG6sWHLafuNc",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "752a6978-399a-475e-b877-b6a5961ed87b",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:03 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"获取用户信息成功\",\"data\":{\"id\":1,\"username\":\"superadmin\",\"role_code\":\"SUPER_ADMIN\",\"role_name\":\"超级管理员\",\"permissions\":[\"user:view\",\"role:manage\",\"trace:create\",\"dashboard:view\",\"trace:view\",\"role:view\",\"user:manage\",\"part:manage\",\"trace:scan\",\"part:view\"],\"status\":1,\"create_time\":\"2026-01-17 18:10:27\"}}"
}
POST http://localhost:8080/api/auth/refresh: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhMTc5ZmQ5NC05MDI4LTQyM2QtYTQ5NS03MmMzNGRjNTNlMGIiLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTAyLCJleHAiOjE3NzAwNDk1MDJ9.T6Z1Ab20ydkXlflBi-UuasFnn1Mu2NBLG6sWHLafuNc",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "f6bf2bd3-dec6-47e7-b39f-f4397b60ce52",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "0"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:03 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"Token 刷新成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs\",\"username\":\"superadmin\",\"role\":\"SUPER_ADMIN\",\"permissions\":[]}}"
}
GET http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "ac26514a-8118-4c74-af50-22aaa2ceb520",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json;charset=UTF-8",
    "content-length": "64",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10002,\"status\":401,\"message\":\"未授权，请先登录\"}"
}
GET http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJoYWNrZXIiLCJyb2xlIjoiU1VQRVJfQURNSU4ifQ.fakesignature",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "26f316e3-16b6-4a8b-b3dd-0c8b562d93de",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json;charset=UTF-8",
    "content-length": "82",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10002,\"status\":401,\"message\":\"Token 无效或已过期，请重新登录\"}"
}
GET http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer not.a.valid.jwt.token",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "1061c307-9b97-48cd-92d3-ea018d2d5331",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json;charset=UTF-8",
    "content-length": "82",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10002,\"status\":401,\"message\":\"Token 无效或已过期，请重新登录\"}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "600c2f2d-adec-4316-88a0-a9c362375e54",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "60"
  },
  "Request Body": "{\"username\": \"admin' OR '1'='1' --\", \"password\": \"anything\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":11002,\"status\":401,\"message\":\"用户名或密码错误\"}"
}
GET http://localhost:8080/api/users?username=admin%27%20OR%201=1%20--: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "e81de730-06cf-49ac-bea2-193b7c4efbaa",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"list\":[],\"total\":0,\"page\":1,\"size\":10,\"total_pages\":0}}"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "ee656784-7adc-40ce-8330-5d6caf7a71fc",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "71"
  },
  "Request Body": "{\"username\": \"<script>alert('xss')</script>\", \"password\": \"test123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":11002,\"status\":401,\"message\":\"用户名或密码错误\"}"
}
GET http://localhost:8080/api/traces/..%2F..%2Fetc%2Fpasswd: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62575
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "870c1754-7a97-47fb-9063-a3aaaed3dd94",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "content-type": "text/html;charset=utf-8",
    "content-language": "en",
    "content-length": "435",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "connection": "close"
  },
  "Response Body": "<!doctype html><html lang=\"en\"><head><title>HTTP Status 400 – Bad Request</title><style type=\"text/css\">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 400 – Bad Request</h1></body></html>"
}
POST http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "f49e35fe-6417-4245-a2b9-57a2cfa3ef58",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "271"
  },
  "Request Body": "{\"username\": \"test\", \"password\": \"test123\", \"extra1\": \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\", \"extra2\": \"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":11002,\"status\":401,\"message\":\"用户名或密码错误\"}"
}
GET http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "5826a2f6-9ab6-42ba-89b3-50008cd48619",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"list\":[{\"id\":9,\"username\":\"testUser\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-01-20 14:39:06\",\"update_time\":\"2026-01-20 14:39:06\"},{\"id\":1,\"username\":\"superadmin\",\"role_id\":1,\"role_code\":\"SUPER_ADMIN\",\"role_name\":\"超级管理员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":2,\"username\":\"admin\",\"role_id\":2,\"role_code\":\"ADMIN\",\"role_name\":\"系统管理员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"username\":\"producer\",\"role_id\":3,\"role_code\":\"PRODUCER\",\"role_name\":\"生产人员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"username\":\"warehouse\",\"role_id\":4,\"role_code\":\"WAREHOUSE\",\"role_name\":\"仓库人员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":5,\"username\":\"logistics\",\"role_id\":5,\"role_code\":\"LOGISTICS\",\"role_name\":\"物流人员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":6,\"username\":\"user\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"}],\"total\":7,\"page\":1,\"size\":10,\"total_pages\":1}}"
}
OPTIONS http://localhost:8080/api/auth/login: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "origin": "https://localhost:5173",
    "access-control-request-method": "POST",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "1bbdd1d2-20be-46ed-b70e-7b3072e8e0d5",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "access-control-allow-origin": "https://localhost:5173",
    "access-control-allow-credentials": "true",
    "access-control-allow-methods": "GET, POST, PUT, PATCH, DELETE, OPTIONS",
    "access-control-allow-headers": "Authorization, Content-Type, Accept, X-Requested-With",
    "access-control-max-age": "3600",
    "content-length": "0",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  }
}
GET http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "37d5e16e-42cf-49c7-aab0-920f46d9baba",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json;charset=UTF-8",
    "content-length": "64",
    "date": "Sun, 01 Feb 2026 16:25:04 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"无权限执行此操作\"}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "b7bbf55d-e662-4dc0-b663-7aefad2d8d88",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "59"
  },
  "Request Body": "{\"spu_id\": 1, \"quantity\": 1, \"manufacturer_node\": \"测试\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json;charset=UTF-8",
    "content-length": "64",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"无权限执行此操作\"}"
}
POST http://localhost:8080/api/traces/TC-TEST-001/events: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZGZhYzAzOC1kZWJiLTQ5ZTgtOTE0YS05MjUzYmUzOTA0MWUiLCJzdWIiOiJsb2dpc3RpY3MiLCJ1c2VybmFtZSI6ImxvZ2lzdGljcyIsInJvbGUiOiJMT0dJU1RJQ1MiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.YZgSIHQ2aicDoGbO34clUjXwPy11LV-t3pwCoBKiplQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "646a5fea-3d26-49d0-b278-1d8f04b669e4",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "60"
  },
  "Request Body": "{\"action_type\": \"INBOUND\", \"from_node\": \"A\", \"to_node\": \"B\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"无权限执行此操作，需要权限: trace:inbound\"}"
}
POST http://localhost:8080/api/traces/TC-TEST-001/events: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZGZhYzAzOC1kZWJiLTQ5ZTgtOTE0YS05MjUzYmUzOTA0MWUiLCJzdWIiOiJsb2dpc3RpY3MiLCJ1c2VybmFtZSI6ImxvZ2lzdGljcyIsInJvbGUiOiJMT0dJU1RJQ1MiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.YZgSIHQ2aicDoGbO34clUjXwPy11LV-t3pwCoBKiplQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "8bb615d7-a8e5-4879-ae47-f49472f4d909",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "61"
  },
  "Request Body": "{\"action_type\": \"OUTBOUND\", \"from_node\": \"A\", \"to_node\": \"B\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"无权限执行此操作，需要权限: trace:outbound\"}"
}
POST http://localhost:8080/api/traces/TC-TEST-001/events: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhODVjZWE4YS02NzgxLTRiNzctYjExZS04MmYxODZiMWI0NzgiLCJzdWIiOiJ3YXJlaG91c2UiLCJ1c2VybmFtZSI6IndhcmVob3VzZSIsInJvbGUiOiJXQVJFSE9VU0UiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.fEWKN2RyD2i-vadjA4Pwq7g79zmdWm86DXfHDpIuM00",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "f1f558bf-7bb1-4c9d-bf6c-2ef714cd4974",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "61"
  },
  "Request Body": "{\"action_type\": \"TRANSFER\", \"from_node\": \"A\", \"to_node\": \"B\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"无权限执行此操作，需要权限: trace:transfer\"}"
}
PATCH http://localhost:8080/api/users/1/status?status=0: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "c55549e9-6daa-470e-9203-b759b5961053",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "0"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"superadmin 账号不能被禁用\"}"
}
DELETE http://localhost:8080/api/users/1: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "3b82cc37-f290-4fe3-95a4-02378c2a9775",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"superadmin 账号不能被删除\"}"
}
GET http://localhost:8080/api/dashboard/kpi: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "6aea420e-3c63-4ad3-998d-3ad1082c8eca",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"range\":\"30d\",\"total_traces\":184,\"total_logs\":221,\"exception_count\":3,\"today_new\":12}}"
}
GET http://localhost:8080/api/traces/TC-TEST-001: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "a2690645-2483-4307-a05a-170d22caf264",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:05 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":20001,\"status\":404,\"message\":\"未知溯源码: TC-TEST-001\"}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "6daf665a-e095-41c6-90e2-c8fb44f5bcf0",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "117"
  },
  "Request Body": "{\"spu_id\": 1, \"quantity\": 1, \"manufacturer_node\": \"权限测试工厂\", \"province\": \"浙江省\", \"city\": \"杭州市\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":201,\"message\":\"赋码成功\",\"data\":{\"generated_count\":1,\"trace_codes\":[\"98f5fa04-0657-4e43-a301-d212258fa4db\"]}}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "8f95c771-47d4-433d-ad95-32fc5bc419ae",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "112"
  },
  "Request Body": "{\"spu_id\": 1, \"quantity\": 2, \"manufacturer_node\": \"测试工厂A\", \"province\": \"浙江省\", \"city\": \"杭州市\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":201,\"message\":\"赋码成功\",\"data\":{\"generated_count\":2,\"trace_codes\":[\"62861642-907e-43ed-b316-74b7e2a511d9\",\"ed5f431b-4d58-439c-9bce-e8488bc84fe1\"]}}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "e9b278eb-073d-48a9-af46-9718f1e6ed08",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "83"
  },
  "Request Body": "{\"part_code\": \"SPU-VALVE-001\", \"quantity\": 1, \"manufacturer_node\": \"测试工厂B\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":201,\"message\":\"赋码成功\",\"data\":{\"generated_count\":1,\"trace_codes\":[\"94dfcfc3-c8ed-49d0-8153-c76fdf1d1a31\"]}}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62596
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "ec55b6c8-eeb5-4234-a66f-9af51e4e92bc",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "63"
  },
  "Request Body": "{\"spu_id\": 99999, \"quantity\": 1, \"manufacturer_node\": \"测试\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"SPU不存在: 99999\"}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62603
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "4ebca004-89e9-41ab-aec7-58117242e509",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "79"
  },
  "Request Body": "{\"part_code\": \"NONEXISTENT-PART\", \"quantity\": 1, \"manufacturer_node\": \"测试\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"配件编码不存在: NONEXISTENT-PART\"}"
}
POST http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9/events: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhODVjZWE4YS02NzgxLTRiNzctYjExZS04MmYxODZiMWI0NzgiLCJzdWIiOiJ3YXJlaG91c2UiLCJ1c2VybmFtZSI6IndhcmVob3VzZSIsInJvbGUiOiJXQVJFSE9VU0UiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.fEWKN2RyD2i-vadjA4Pwq7g79zmdWm86DXfHDpIuM00",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "fd40f2f8-3406-4db7-a042-64be81a0cf03",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "129"
  },
  "Request Body": "{\"action_type\": \"INBOUND\", \"from_node\": \"测试工厂A\", \"to_node\": \"中心仓库\", \"province\": \"江苏省\", \"city\": \"苏州市\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":201,\"message\":\"流转记录成功\"}"
}
POST http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9/events: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhODVjZWE4YS02NzgxLTRiNzctYjExZS04MmYxODZiMWI0NzgiLCJzdWIiOiJ3YXJlaG91c2UiLCJ1c2VybmFtZSI6IndhcmVob3VzZSIsInJvbGUiOiJXQVJFSE9VU0UiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.fEWKN2RyD2i-vadjA4Pwq7g79zmdWm86DXfHDpIuM00",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "6ec2bc80-9c12-4c25-9f8a-012addc2299c",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "129"
  },
  "Request Body": "{\"action_type\": \"OUTBOUND\", \"from_node\": \"中心仓库\", \"to_node\": \"物流中心\", \"province\": \"江苏省\", \"city\": \"南京市\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":201,\"message\":\"流转记录成功\"}"
}
POST http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9/events: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZGZhYzAzOC1kZWJiLTQ5ZTgtOTE0YS05MjUzYmUzOTA0MWUiLCJzdWIiOiJsb2dpc3RpY3MiLCJ1c2VybmFtZSI6ImxvZ2lzdGljcyIsInJvbGUiOiJMT0dJU1RJQ1MiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.YZgSIHQ2aicDoGbO34clUjXwPy11LV-t3pwCoBKiplQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "75504118-ade7-4d60-bc71-0e58c5b7769c",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "132"
  },
  "Request Body": "{\"action_type\": \"TRANSFER\", \"from_node\": \"物流中心\", \"to_node\": \"区域配送站\", \"province\": \"上海市\", \"city\": \"上海市\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":201,\"message\":\"流转记录成功\"}"
}
GET http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "481ec5b3-9295-40f1-a835-1cd6120e25e7",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:06 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"snapshot\":{\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"current_status\":\"TRANSFERRED\",\"current_node\":\"区域配送站\",\"current_owner\":\"区域配送站\",\"province\":\"上海市\",\"city\":\"上海市\",\"last_event_time\":\"2026-02-02T00:25:07\",\"last_log_id\":561,\"last_hash\":\"433bd390e5420a8f759c1a99c8a9743d1485a80800ae0adcdb0d4c027dfaad1c\",\"version\":3,\"update_time\":\"2026-02-02T00:25:07\"},\"history\":[{\"id\":556,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"INIT\",\"from_node\":null,\"to_node\":\"测试工厂A\",\"province\":\"浙江省\",\"city\":\"杭州市\",\"event_time\":\"2026-02-02T00:25:06\",\"ingest_time\":\"2026-02-02T00:25:06\",\"prev_hash\":\"GENESIS\",\"current_hash\":\"33b7315fa285246dd8fda565bd1434af2ab60f36fcbacfbbbf40961ec079b615\",\"correction_of\":null,\"operator\":\"producer\",\"signature\":\"YlzcRK7civnHVutmUaqCfgMCzigPYwTiezBp0wle3obFt6uEa/spxcQ205M9bf4LZCdjH/yV3/6sb/sr/zdZAtji9SK4sS0QhEhEevARThgwU74BQGy8SG6qjUXXcUaVrIgk4GPQGfrfehE8DCPKZxwCHMzTs5oxsSiKHz+LVxuXAwJl6+9f09/MyUUCW6mNqpr7xiCxxXZAMs8xLWgzFnsicA/wLRH/YT6Zni6iwI+GbuoEg+EuaPZewgKy+ExQ8iMgnF6rY1GTQxffI0VcF7X1da9+8ceHYk4DynPELrYr3JwA20RFg8VEPwk7BsllEPUgyrdhFMdW2bPaj4nQyA==\",\"create_time\":\"2026-02-02T00:25:07\"},{\"id\":559,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"INBOUND\",\"from_node\":\"测试工厂A\",\"to_node\":\"中心仓库\",\"province\":\"江苏省\",\"city\":\"苏州市\",\"event_time\":\"2026-02-02T00:25:07\",\"ingest_time\":\"2026-02-02T00:25:07\",\"prev_hash\":\"33b7315fa285246dd8fda565bd1434af2ab60f36fcbacfbbbf40961ec079b615\",\"current_hash\":\"a8e4bbf43ed8b28ada13c530a181f6d2e1863c99442cabf8c2ffc9b5e44a013f\",\"correction_of\":null,\"operator\":\"warehouse\",\"signature\":\"MR+h1iEAMhBxoWRvVqop2cUO5axYLSSv86w1mhxKh7KshaQSsUCYVKesThwmqQlygkxzPjxulLtazd2snMjIKWTkQYr0FWrmfnbHTFuiKMN8u6Trx33iQ5hZ1ZACzD7CNQS2bcdSE+TZb1HWRfY63Uby40dP5pA1tWOw/HcD9VlT5bWkbuLQWOXxaneRj0mcW8N/wJTYsleUnE5RYnCGGF4QhfSz74c6Dpc/lUjXQcr3WUb9YI7ux6bR7zMfz371zjtVTr93kjW6+v1VEdhZnbhCzSdzbVIIjw2d2gtCqzfSAupRT5ma7l8juLjhM74oQR/yF1xFhDkm6KBU+ZHX8Q==\",\"create_time\":\"2026-02-02T00:25:07\"},{\"id\":560,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"OUTBOUND\",\"from_node\":\"中心仓库\",\"to_node\":\"物流中心\",\"province\":\"江苏省\",\"city\":\"南京市\",\"event_time\":\"2026-02-02T00:25:07\",\"ingest_time\":\"2026-02-02T00:25:07\",\"prev_hash\":\"a8e4bbf43ed8b28ada13c530a181f6d2e1863c99442cabf8c2ffc9b5e44a013f\",\"current_hash\":\"4e6dd0b866f9b53b65ea846e848c2418a1e80ac88926f90177c49e6819fe70e8\",\"correction_of\":null,\"operator\":\"warehouse\",\"signature\":\"sGuFrLRQ0i/HjHUqdkqolCIEifqaDR2FEVmbYDiG01J7nhkmhUQNYhXwIDfjK+oPmobZvABqTZ9j5Sg1FX8pdf15P7JEaPKpdu6U186zu4EQlt1KmYohkyqHbc5CiNHFpCzryz8UM1fBdH+XYD6mB5Xz1+7NhzOQr9V2/IeGQa2Z9t9hAYUhgvDjOs9KWyjmsZ01Hi9D+9tY0Hgl14NBy7U0Rrd0mrRXFNM+5siKGsUUWCensmZDRJWHcftJmjsnvfG/WFVbETrkIgsExNMR14XwfzrB3tTCpuPrxn6O32JmdOVSFFIJe2gXq6bknXyYhT1nNBRUsOjJMlq+z49gEA==\",\"create_time\":\"2026-02-02T00:25:08\"},{\"id\":561,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"TRANSFER\",\"from_node\":\"物流中心\",\"to_node\":\"区域配送站\",\"province\":\"上海市\",\"city\":\"上海市\",\"event_time\":\"2026-02-02T00:25:07\",\"ingest_time\":\"2026-02-02T00:25:07\",\"prev_hash\":\"4e6dd0b866f9b53b65ea846e848c2418a1e80ac88926f90177c49e6819fe70e8\",\"current_hash\":\"433bd390e5420a8f759c1a99c8a9743d1485a80800ae0adcdb0d4c027dfaad1c\",\"correction_of\":null,\"operator\":\"logistics\",\"signature\":\"iPbePeaE2aCYad+UbKINFloRT08n2SDQfTjjovqwE17M/P2EwvDiCeIn5T/ic8xOsE87RwnViH3fipHBIz4CIAkatnKiyRnqvZ/dC3pvGDhIo8MBvt/BPyT2aXZiapdBLMdJWagMilSY/8ogaKO4Td0RVX5J3RiP7xZUtQyzuka97/HewbrgLDuDDh8PSe9/6NlmcSHOl4K1wRvLzmeF3n+8MJt6WwCNYQRXWKdrFZ/Dxx2BFbVbW5PrZy2YiZdeN5R2FeCfcxe0iUuZ8l0zB5xOPIDOdLEVYav62nFxpkdDiVAlLXjrl9NPU/STaEs8NUvoD7VniMIok+02n6XWQQ==\",\"create_time\":\"2026-02-02T00:25:08\"}]}}"
}
GET http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9/verify: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "acc8ae68-a217-49ab-8388-a0d876155533",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"valid\":true,\"total_logs\":4,\"hash_verified_count\":4,\"signature_verified_count\":4,\"anchor_hash\":\"433bd390e5420a8f759c1a99c8a9743d1485a80800ae0adcdb0d4c027dfaad1c\",\"anchor_signature\":\"iPbePeaE2aCYad+UbKINFloRT08n2SDQfTjjovqwE17M/P2EwvDiCeIn5T/ic8xOsE87RwnViH3fipHBIz4CIAkatnKiyRnqvZ/dC3pvGDhIo8MBvt/BPyT2aXZiapdBLMdJWagMilSY/8ogaKO4Td0RVX5J3RiP7xZUtQyzuka97/HewbrgLDuDDh8PSe9/6NlmcSHOl4K1wRvLzmeF3n+8MJt6WwCNYQRXWKdrFZ/Dxx2BFbVbW5PrZy2YiZdeN5R2FeCfcxe0iUuZ8l0zB5xOPIDOdLEVYav62nFxpkdDiVAlLXjrl9NPU/STaEs8NUvoD7VniMIok+02n6XWQQ==\",\"public_key\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0etR/XYGtMzGaziHWV0QQlp66pOVA13ekBuHvnFttsF2xMi9BJzexQQG0q2X8LkBBr7EBBJw//Tp3qM/n9TykkNHt2AIPvgJLAb2ib9pS3EUXlEq5bsgtnzyw/j8fmudTAcvHDmu2G1PIUYl5/OuVkE7/58GzjZjXRxNVTSHtp0aqPT5JZfC1S4lvRkq2n1jyFBQLz53z6K/c1n1JosmZ6/wnsArOqoVxt2jE3KAHtuRZxHLDb4wuk27AxKg/qCGuT32I19OjKuQ6T7ho5fRlpsteB+i1o/NHLvBVB5SLWTcBSpEZukDzPwX8UR72IolWPJSyokP7LEDFu69O1aA8QIDAQAB\",\"errors\":[],\"verify_time\":\"2026-02-02T00:25:07.881906200\",\"verify_duration_ms\":5}}"
}
GET http://localhost:8080/api/traces/public-key: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "ac751054-d659-443d-877b-bee846462733",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"publicKey\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0etR/XYGtMzGaziHWV0QQlp66pOVA13ekBuHvnFttsF2xMi9BJzexQQG0q2X8LkBBr7EBBJw//Tp3qM/n9TykkNHt2AIPvgJLAb2ib9pS3EUXlEq5bsgtnzyw/j8fmudTAcvHDmu2G1PIUYl5/OuVkE7/58GzjZjXRxNVTSHtp0aqPT5JZfC1S4lvRkq2n1jyFBQLz53z6K/c1n1JosmZ6/wnsArOqoVxt2jE3KAHtuRZxHLDb4wuk27AxKg/qCGuT32I19OjKuQ6T7ho5fRlpsteB+i1o/NHLvBVB5SLWTcBSpEZukDzPwX8UR72IolWPJSyokP7LEDFu69O1aA8QIDAQAB\",\"signatureAlgorithm\":\"SHA256withRSA\",\"algorithm\":\"RSA\"}}"
}
GET http://localhost:8080/api/traces/NONEXISTENT-TRACE-CODE-12345: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "d97cb88e-c97d-41f1-85eb-87aa03094cc8",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":20001,\"status\":404,\"message\":\"未知溯源码: NONEXISTENT-TRACE-CODE-12345\"}"
}
POST http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9/events: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62605
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhODVjZWE4YS02NzgxLTRiNzctYjExZS04MmYxODZiMWI0NzgiLCJzdWIiOiJ3YXJlaG91c2UiLCJ1c2VybmFtZSI6IndhcmVob3VzZSIsInJvbGUiOiJXQVJFSE9VU0UiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.fEWKN2RyD2i-vadjA4Pwq7g79zmdWm86DXfHDpIuM00",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "3ebaac53-33f8-496b-9128-340ddcf7100e",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "65"
  },
  "Request Body": "{\"action_type\": \"INVALID_TYPE\", \"from_node\": \"A\", \"to_node\": \"B\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"非法的 ActionType 值，允许的值: INIT, INBOUND, OUTBOUND, TRANSFER, EXCEPTION, CORRECTION\"}"
}
GET http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "381e89d1-1b7c-4113-ac82-ba86d935c497",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"list\":[{\"id\":9,\"username\":\"testUser\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-01-20 14:39:06\",\"update_time\":\"2026-01-20 14:39:06\"},{\"id\":1,\"username\":\"superadmin\",\"role_id\":1,\"role_code\":\"SUPER_ADMIN\",\"role_name\":\"超级管理员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":2,\"username\":\"admin\",\"role_id\":2,\"role_code\":\"ADMIN\",\"role_name\":\"系统管理员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"username\":\"producer\",\"role_id\":3,\"role_code\":\"PRODUCER\",\"role_name\":\"生产人员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"username\":\"warehouse\",\"role_id\":4,\"role_code\":\"WAREHOUSE\",\"role_name\":\"仓库人员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":5,\"username\":\"logistics\",\"role_id\":5,\"role_code\":\"LOGISTICS\",\"role_name\":\"物流人员\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"},{\"id\":6,\"username\":\"user\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"}],\"total\":7,\"page\":1,\"size\":10,\"total_pages\":1}}"
}
POST http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "abd58310-8c49-4a10-8647-ec627d2e86f5",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "82"
  },
  "Request Body": "{\"username\": \"testuser108389\", \"password\": \"Test123456\", \"roleId\": 6, \"status\": 1}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":13,\"username\":\"testuser108389\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-02-02 00:25:08\",\"update_time\":\"2026-02-02 00:25:08\"}}"
}
GET http://localhost:8080/api/users/13: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "e30eeafb-4af2-4f69-ac41-8e9ce1607ff9",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":13,\"username\":\"testuser108389\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-02-02 00:25:08\",\"update_time\":\"2026-02-02 00:25:08\"}}"
}
PUT http://localhost:8080/api/users/13: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "43e51f29-188f-4f44-9eff-c34e7a213370",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "13"
  },
  "Request Body": "{\"status\": 1}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":13,\"username\":\"testuser108389\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-02-02 00:25:08\",\"update_time\":\"2026-02-02 00:25:08\"}}"
}
PATCH http://localhost:8080/api/users/13/status?status=0: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "c5e3a113-bdda-4daa-b15a-2acb78f37084",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "0"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:07 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":13,\"username\":\"testuser108389\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":0,\"create_time\":\"2026-02-02 00:25:08\",\"update_time\":\"2026-02-02 00:25:08\"}}"
}
PATCH http://localhost:8080/api/users/13/status?status=1: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "6c6acfa3-ab8d-4c39-979d-f8ae1d6ac9df",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "0"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":13,\"username\":\"testuser108389\",\"role_id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"status\":1,\"create_time\":\"2026-02-02 00:25:08\",\"update_time\":\"2026-02-02 00:25:08\"}}"
}
PATCH http://localhost:8080/api/users/13/role?roleId=5: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "3f2b494b-3ff2-47cf-b9a8-3e76244e8994",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "0"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":13,\"username\":\"testuser108389\",\"role_id\":5,\"role_code\":\"LOGISTICS\",\"role_name\":\"物流人员\",\"status\":1,\"create_time\":\"2026-02-02 00:25:08\",\"update_time\":\"2026-02-02 00:25:08\"}}"
}
POST http://localhost:8080/api/users/13/reset-password: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "bc2973b8-47e3-46b3-9eae-1a14112f52e4",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "32"
  },
  "Request Body": "{\"newPassword\": \"NewPass123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\"}"
}
POST http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "afd9f7a6-0868-42d7-a76f-e7e413eaae57",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "60"
  },
  "Request Body": "{\"username\": \"admin\", \"password\": \"Test123456\", \"roleId\": 6}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10007,\"status\":409,\"message\":\"用户名已存在\"}"
}
DELETE http://localhost:8080/api/users/13: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "0c93488e-719a-48ac-9ef7-cfb48fe362bf",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\"}"
}
GET http://localhost:8080/api/roles: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "d6788653-00da-4cd6-aa18-48e11f88c66b",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":[{\"id\":1,\"role_code\":\"SUPER_ADMIN\",\"role_name\":\"超级管理员\",\"remark\":\"拥有最高权限，可管理所有用户包括其他管理员\",\"permissions\":[{\"id\":1,\"perm_code\":\"trace:create\",\"perm_name\":\"生产赋码\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces\",\"remark\":\"创建溯源实例\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":2,\"perm_code\":\"trace:scan\",\"perm_name\":\"扫码流转\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"记录流转事件\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":5,\"perm_code\":\"user:view\",\"perm_name\":\"查看用户\",\"api_method\":\"GET\",\"api_pattern\":\"/api/users/*\",\"remark\":\"查看用户列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":6,\"perm_code\":\"user:manage\",\"perm_name\":\"管理用户\",\"api_method\":\"*\",\"api_pattern\":\"/api/users/*\",\"remark\":\"创建、修改、删除用户\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":7,\"perm_code\":\"role:view\",\"perm_name\":\"查看角色\",\"api_method\":\"GET\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"查看角色和权限列表\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":8,\"perm_code\":\"role:manage\",\"perm_name\":\"管理角色\",\"api_method\":\"*\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"增删改角色和权限分配\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":9,\"perm_code\":\"part:view\",\"perm_name\":\"查看配件\",\"api_method\":\"GET\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"查看配件列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":10,\"perm_code\":\"part:manage\",\"perm_name\":\"管理配件\",\"api_method\":\"*\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"创建、修改、删除配件\",\"create_time\":\"2026-01-17 18:10:27\"}],\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":2,\"role_code\":\"ADMIN\",\"role_name\":\"系统管理员\",\"remark\":\"拥有管理权限，可管理普通用户\",\"permissions\":[{\"id\":1,\"perm_code\":\"trace:create\",\"perm_name\":\"生产赋码\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces\",\"remark\":\"创建溯源实例\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":2,\"perm_code\":\"trace:scan\",\"perm_name\":\"扫码流转\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"记录流转事件\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":5,\"perm_code\":\"user:view\",\"perm_name\":\"查看用户\",\"api_method\":\"GET\",\"api_pattern\":\"/api/users/*\",\"remark\":\"查看用户列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":6,\"perm_code\":\"user:manage\",\"perm_name\":\"管理用户\",\"api_method\":\"*\",\"api_pattern\":\"/api/users/*\",\"remark\":\"创建、修改、删除用户\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":7,\"perm_code\":\"role:view\",\"perm_name\":\"查看角色\",\"api_method\":\"GET\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"查看角色和权限列表\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":8,\"perm_code\":\"role:manage\",\"perm_name\":\"管理角色\",\"api_method\":\"*\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"增删改角色和权限分配\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":9,\"perm_code\":\"part:view\",\"perm_name\":\"查看配件\",\"api_method\":\"GET\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"查看配件列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":10,\"perm_code\":\"part:manage\",\"perm_name\":\"管理配件\",\"api_method\":\"*\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"创建、修改、删除配件\",\"create_time\":\"2026-01-17 18:10:27\"}],\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"role_code\":\"PRODUCER\",\"role_name\":\"生产人员\",\"remark\":\"可进行生产赋码\",\"permissions\":[{\"id\":1,\"perm_code\":\"trace:create\",\"perm_name\":\"生产赋码\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces\",\"remark\":\"创建溯源实例\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":9,\"perm_code\":\"part:view\",\"perm_name\":\"查看配件\",\"api_method\":\"GET\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"查看配件列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"}],\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"role_code\":\"WAREHOUSE\",\"role_name\":\"仓库人员\",\"remark\":\"可进行入库/出库操作\",\"permissions\":[{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":9,\"perm_code\":\"part:view\",\"perm_name\":\"查看配件\",\"api_method\":\"GET\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"查看配件列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":11,\"perm_code\":\"trace:inbound\",\"perm_name\":\"入库操作\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"允许执行入库扫码操作\",\"create_time\":\"2026-02-01 21:06:38\"},{\"id\":12,\"perm_code\":\"trace:outbound\",\"perm_name\":\"出库操作\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"允许执行出库扫码操作\",\"create_time\":\"2026-02-01 21:06:38\"}],\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":5,\"role_code\":\"LOGISTICS\",\"role_name\":\"物流人员\",\"remark\":\"可进行流转操作\",\"permissions\":[{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":13,\"perm_code\":\"trace:transfer\",\"perm_name\":\"物流流转\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"允许执行物流流转扫码操作\",\"create_time\":\"2026-02-01 21:06:38\"}],\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":6,\"role_code\":\"USER\",\"role_name\":\"普通用户\",\"remark\":\"仅可查询溯源信息\",\"permissions\":[{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"}],\"create_time\":\"2026-01-17 18:10:27\"}]}"
}
GET http://localhost:8080/api/roles/1: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "9d92e3ec-60ee-4642-80db-10bde131d5b5",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":1,\"role_code\":\"SUPER_ADMIN\",\"role_name\":\"超级管理员\",\"remark\":\"拥有最高权限，可管理所有用户包括其他管理员\",\"permissions\":[{\"id\":1,\"perm_code\":\"trace:create\",\"perm_name\":\"生产赋码\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces\",\"remark\":\"创建溯源实例\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":2,\"perm_code\":\"trace:scan\",\"perm_name\":\"扫码流转\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"记录流转事件\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":5,\"perm_code\":\"user:view\",\"perm_name\":\"查看用户\",\"api_method\":\"GET\",\"api_pattern\":\"/api/users/*\",\"remark\":\"查看用户列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":6,\"perm_code\":\"user:manage\",\"perm_name\":\"管理用户\",\"api_method\":\"*\",\"api_pattern\":\"/api/users/*\",\"remark\":\"创建、修改、删除用户\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":7,\"perm_code\":\"role:view\",\"perm_name\":\"查看角色\",\"api_method\":\"GET\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"查看角色和权限列表\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":8,\"perm_code\":\"role:manage\",\"perm_name\":\"管理角色\",\"api_method\":\"*\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"增删改角色和权限分配\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":9,\"perm_code\":\"part:view\",\"perm_name\":\"查看配件\",\"api_method\":\"GET\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"查看配件列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":10,\"perm_code\":\"part:manage\",\"perm_name\":\"管理配件\",\"api_method\":\"*\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"创建、修改、删除配件\",\"create_time\":\"2026-01-17 18:10:27\"}],\"create_time\":\"2026-01-17 18:10:27\"}}"
}
GET http://localhost:8080/api/roles/permissions: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "5b0fe415-dab2-4e5d-b84d-883b85ab9910",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":[{\"id\":1,\"perm_code\":\"trace:create\",\"perm_name\":\"生产赋码\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces\",\"remark\":\"创建溯源实例\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":2,\"perm_code\":\"trace:scan\",\"perm_name\":\"扫码流转\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"记录流转事件\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":3,\"perm_code\":\"trace:view\",\"perm_name\":\"溯源查询\",\"api_method\":\"GET\",\"api_pattern\":\"/api/traces/*\",\"remark\":\"查看溯源详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":4,\"perm_code\":\"dashboard:view\",\"perm_name\":\"看板查看\",\"api_method\":\"GET\",\"api_pattern\":\"/api/dashboard/*\",\"remark\":\"查看统计数据\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":5,\"perm_code\":\"user:view\",\"perm_name\":\"查看用户\",\"api_method\":\"GET\",\"api_pattern\":\"/api/users/*\",\"remark\":\"查看用户列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":6,\"perm_code\":\"user:manage\",\"perm_name\":\"管理用户\",\"api_method\":\"*\",\"api_pattern\":\"/api/users/*\",\"remark\":\"创建、修改、删除用户\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":7,\"perm_code\":\"role:view\",\"perm_name\":\"查看角色\",\"api_method\":\"GET\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"查看角色和权限列表\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":8,\"perm_code\":\"role:manage\",\"perm_name\":\"管理角色\",\"api_method\":\"*\",\"api_pattern\":\"/api/roles/*\",\"remark\":\"增删改角色和权限分配\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":9,\"perm_code\":\"part:view\",\"perm_name\":\"查看配件\",\"api_method\":\"GET\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"查看配件列表和详情\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":10,\"perm_code\":\"part:manage\",\"perm_name\":\"管理配件\",\"api_method\":\"*\",\"api_pattern\":\"/api/parts/*\",\"remark\":\"创建、修改、删除配件\",\"create_time\":\"2026-01-17 18:10:27\"},{\"id\":11,\"perm_code\":\"trace:inbound\",\"perm_name\":\"入库操作\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"允许执行入库扫码操作\",\"create_time\":\"2026-02-01 21:06:38\"},{\"id\":12,\"perm_code\":\"trace:outbound\",\"perm_name\":\"出库操作\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"允许执行出库扫码操作\",\"create_time\":\"2026-02-01 21:06:38\"},{\"id\":13,\"perm_code\":\"trace:transfer\",\"perm_name\":\"物流流转\",\"api_method\":\"POST\",\"api_pattern\":\"/api/traces/*/events\",\"remark\":\"允许执行物流流转扫码操作\",\"create_time\":\"2026-02-01 21:06:38\"}]}"
}
GET http://localhost:8080/api/parts: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "94833330-501b-4baf-a5cd-c93679950e75",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:08 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"list\":[{\"id\":3,\"part_code\":\"SPU-VALVE-002\",\"part_name\":\"气动球阀\",\"part_type\":\"阀门类\",\"model\":\"V-2024002\",\"manufacturer\":\"上海阀门厂\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":4,\"part_code\":\"SPU-VALVE-003\",\"part_name\":\"电动蝶阀\",\"part_type\":\"阀门类\",\"model\":\"V-2024003\",\"manufacturer\":\"浙江阀门集团\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":5,\"part_code\":\"SPU-VALVE-004\",\"part_name\":\"手动闸阀\",\"part_type\":\"阀门类\",\"model\":\"V-2024004\",\"manufacturer\":\"江苏管道设备\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":6,\"part_code\":\"SPU-VALVE-005\",\"part_name\":\"止回阀\",\"part_type\":\"阀门类\",\"model\":\"V-2024005\",\"manufacturer\":\"天津阀门制造\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":7,\"part_code\":\"SPU-VALVE-006\",\"part_name\":\"安全阀\",\"part_type\":\"阀门类\",\"model\":\"V-2024006\",\"manufacturer\":\"山东安全设备\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":8,\"part_code\":\"SPU-BEAR-001\",\"part_name\":\"深沟球轴承\",\"part_type\":\"轴承类\",\"model\":\"B-6205\",\"manufacturer\":\"SKF中国\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":9,\"part_code\":\"SPU-BEAR-002\",\"part_name\":\"圆柱滚子轴承\",\"part_type\":\"轴承类\",\"model\":\"B-NU206\",\"manufacturer\":\"哈尔滨轴承\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":10,\"part_code\":\"SPU-BEAR-003\",\"part_name\":\"调心滚子轴承\",\"part_type\":\"轴承类\",\"model\":\"B-22208\",\"manufacturer\":\"洛阳LYC\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":11,\"part_code\":\"SPU-BEAR-004\",\"part_name\":\"角接触球轴承\",\"part_type\":\"轴承类\",\"model\":\"B-7206\",\"manufacturer\":\"人本轴承\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"},{\"id\":12,\"part_code\":\"SPU-MOTOR-001\",\"part_name\":\"三相异步电机\",\"part_type\":\"电机类\",\"model\":\"M-Y160M\",\"manufacturer\":\"卧龙电机\",\"unit\":\"件\",\"remark\":\"示例数据\",\"create_time\":\"2026-01-19 13:55:08\",\"update_time\":\"2026-01-19 13:55:08\"}],\"total\":19,\"page\":1,\"size\":10,\"total_pages\":2}}"
}
POST http://localhost:8080/api/parts: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "27868009-5387-4086-878d-5a0176764a0a",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "120"
  },
  "Request Body": "{\"partCode\": \"TEST-PART-109964\", \"partName\": \"测试配件\", \"partType\": \"测试类型\", \"manufacturer\": \"测试厂商\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":26,\"part_code\":\"TEST-PART-109964\",\"part_name\":\"测试配件\",\"part_type\":\"测试类型\",\"model\":null,\"manufacturer\":\"测试厂商\",\"unit\":null,\"remark\":null,\"create_time\":\"2026-02-02 00:25:10\",\"update_time\":\"2026-02-02 00:25:10\"}}"
}
GET http://localhost:8080/api/parts/26: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "b72c40bc-c824-42ce-a263-1e51994c0532",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":26,\"part_code\":\"TEST-PART-109964\",\"part_name\":\"测试配件\",\"part_type\":\"测试类型\",\"model\":null,\"manufacturer\":\"测试厂商\",\"unit\":null,\"remark\":null,\"create_time\":\"2026-02-02 00:25:10\",\"update_time\":\"2026-02-02 00:25:10\"}}"
}
GET http://localhost:8080/api/parts/code/TEST-PART-109964: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "51074fff-f8d0-4f46-b09f-099ffd64a4d4",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":26,\"part_code\":\"TEST-PART-109964\",\"part_name\":\"测试配件\",\"part_type\":\"测试类型\",\"model\":null,\"manufacturer\":\"测试厂商\",\"unit\":null,\"remark\":null,\"create_time\":\"2026-02-02 00:25:10\",\"update_time\":\"2026-02-02 00:25:10\"}}"
}
PUT http://localhost:8080/api/parts/26: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "095de703-bcc9-4fd1-9ae7-fb321fc40c6d",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "37"
  },
  "Request Body": "{\"partName\": \"更新后的配件名\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"id\":26,\"part_code\":\"TEST-PART-109964\",\"part_name\":\"更新后的配件名\",\"part_type\":\"测试类型\",\"model\":null,\"manufacturer\":\"测试厂商\",\"unit\":null,\"remark\":null,\"create_time\":\"2026-02-02 00:25:10\",\"update_time\":\"2026-02-02 00:25:10\"}}"
}
GET http://localhost:8080/api/parts/types: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "4c2c5325-ff57-4e75-9a5c-512580aae46f",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":[\"传感器类\",\"测试类型\",\"电机类\",\"管件类\",\"轴承类\",\"阀门类\"]}"
}
GET http://localhost:8080/api/parts/manufacturers: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "e89b1c68-2176-4086-bab5-11a8598a7b8b",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":[\"E+H中国\",\"SICK中国\",\"SKF中国\",\"上海阀门厂\",\"人本轴承\",\"佳木斯电机\",\"卧龙电机\",\"哈尔滨轴承\",\"大洋电机\",\"天津阀门制造\",\"宝钢股份\",\"山东安全设备\",\"横河中国\",\"江苏管道设备\",\"河北管件\",\"洛阳LYC\",\"测试厂商\",\"浙江阀门集团\",\"示例制造商\",\"重庆法兰\"]}"
}
GET http://localhost:8080/api/parts?keyword=%E9%98%80%E9%97%A8: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "71111dc6-6034-447c-9813-962db0e7c4b8",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"list\":[{\"id\":1,\"part_code\":\"SPU-VALVE-001\",\"part_name\":\"工业高压阀门\",\"part_type\":\"阀门类\",\"model\":\"V-2024001\",\"manufacturer\":\"示例制造商\",\"unit\":\"件\",\"remark\":\"演示数据\",\"create_time\":\"2026-01-17 18:10:27\",\"update_time\":\"2026-01-17 18:10:27\"}],\"total\":1,\"page\":1,\"size\":10,\"total_pages\":1}}"
}
DELETE http://localhost:8080/api/parts/26: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "04cfb8c2-1011-41d7-8689-50d917735e9e",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\"}"
}
GET http://localhost:8080/api/dashboard/kpi: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "feb22229-e7c8-4d70-802a-78c72ac6ec95",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"range\":\"30d\",\"total_traces\":188,\"total_logs\":228,\"exception_count\":3,\"today_new\":16}}"
}
GET http://localhost:8080/api/dashboard/kpi?range=7d: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "34a59dc3-9a3f-4a04-a403-81969077f504",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:10 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"range\":\"7d\",\"total_traces\":188,\"total_logs\":58,\"exception_count\":0,\"today_new\":16}}"
}
GET http://localhost:8080/api/dashboard/map: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "744ae9e9-4d17-4b02-83c1-e0c243dff039",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"total\":22,\"range\":\"30d\",\"items\":[{\"name\":\"浙江省\",\"value\":45},{\"name\":\"江苏省\",\"value\":25},{\"name\":\"上海市\",\"value\":15},{\"name\":\"山西省\",\"value\":14},{\"name\":\"四川省\",\"value\":13},{\"name\":\"河南省\",\"value\":13},{\"name\":\"甘肃省\",\"value\":13},{\"name\":\"广东省\",\"value\":10},{\"name\":\"辽宁省\",\"value\":10},{\"name\":\"重庆市\",\"value\":10},{\"name\":\"山东省\",\"value\":8},{\"name\":\"福建省\",\"value\":8},{\"name\":\"贵州省\",\"value\":8},{\"name\":\"河北省\",\"value\":7},{\"name\":\"云南省\",\"value\":6},{\"name\":\"湖南省\",\"value\":5},{\"name\":\"北京市\",\"value\":3},{\"name\":\"陕西省\",\"value\":3},{\"name\":\"黑龙江省\",\"value\":3},{\"name\":\"天津市\",\"value\":2},{\"name\":\"湖北省\",\"value\":2},{\"name\":\"吉林省\",\"value\":1}]}}"
}
GET http://localhost:8080/api/dashboard/trend: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "2f72e551-0d9b-4b57-8f72-6c357a40a7c4",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"total\":14,\"range\":\"30d\",\"items\":[{\"count\":21,\"label\":\"2026-01-03\"},{\"count\":23,\"label\":\"2026-01-04\"},{\"count\":18,\"label\":\"2026-01-05\"},{\"count\":11,\"label\":\"2026-01-06\"},{\"count\":17,\"label\":\"2026-01-07\"},{\"count\":17,\"label\":\"2026-01-08\"},{\"count\":17,\"label\":\"2026-01-09\"},{\"count\":19,\"label\":\"2026-01-10\"},{\"count\":13,\"label\":\"2026-01-11\"},{\"count\":12,\"label\":\"2026-01-12\"},{\"count\":1,\"label\":\"2026-01-13\"},{\"count\":1,\"label\":\"2026-01-21\"},{\"count\":30,\"label\":\"2026-02-01\"},{\"count\":28,\"label\":\"2026-02-02\"}]}}"
}
GET http://localhost:8080/api/dashboard/topology: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "04edd426-0684-4e91-949a-9780d3f7a5dd",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"nodes\":[{\"symbolSize\":40,\"name\":\"12\"},{\"symbolSize\":40,\"name\":\"重庆\"},{\"symbolSize\":40,\"name\":\"上海仓库A\"},{\"symbolSize\":40,\"name\":\"发往物流中心\"},{\"symbolSize\":40,\"name\":\"上海化工园仓库\"},{\"symbolSize\":40,\"name\":\"昆明建材城\"},{\"symbolSize\":40,\"name\":\"石家庄电机厂\"},{\"symbolSize\":40,\"name\":\"上海智能仓\"},{\"symbolSize\":40,\"name\":\"上海生产车间A\"},{\"symbolSize\":40,\"name\":\"太原传感器基地\"},{\"symbolSize\":40,\"name\":\"杭州生产基地\"},{\"symbolSize\":40,\"name\":\"东莞电机制造\"},{\"symbolSize\":40,\"name\":\"福州电机厂\"},{\"symbolSize\":40,\"name\":\"郑州仓储中心\"},{\"symbolSize\":40,\"name\":\"青岛港仓库\"},{\"symbolSize\":40,\"name\":\"中心仓库\"},{\"symbolSize\":40,\"name\":\"物流中心\"},{\"symbolSize\":40,\"name\":\"佛山变频电机厂\"},{\"symbolSize\":40,\"name\":\"杭州成品仓\"},{\"symbolSize\":40,\"name\":\"佛山成品库\"},{\"symbolSize\":40,\"name\":\"兰州钢管厂\"},{\"symbolSize\":40,\"name\":\"天津港仓库\"},{\"symbolSize\":40,\"name\":\"太原工业园仓库\"},{\"symbolSize\":40,\"name\":\"厦门物流中心\"},{\"symbolSize\":40,\"name\":\"成都建材城\"},{\"symbolSize\":40,\"name\":\"长沙机械厂\"},{\"symbolSize\":40,\"name\":\"哈尔滨轴承厂\"},{\"symbolSize\":40,\"name\":\"南京物流枢纽\"},{\"symbolSize\":40,\"name\":\"武汉物流枢纽\"},{\"symbolSize\":40,\"name\":\"贵阳阀门制造\"},{\"symbolSize\":40,\"name\":\"宁波港仓库\"},{\"symbolSize\":40,\"name\":\"杭州石化仓库\"},{\"symbolSize\":40,\"name\":\"重庆物流园\"},{\"symbolSize\":40,\"name\":\"广州电机工厂\"},{\"symbolSize\":40,\"name\":\"成都化工仓\"},{\"symbolSize\":40,\"name\":\"成都阀门基地\"},{\"symbolSize\":40,\"name\":\"西安阀门集团\"},{\"symbolSize\":40,\"name\":\"昆明管道厂\"},{\"symbolSize\":40,\"name\":\"沈阳仓储中心\"},{\"symbolSize\":40,\"name\":\"沈阳机械市场\"},{\"symbolSize\":40,\"name\":\"洛阳LYC轴承\"},{\"symbolSize\":40,\"name\":\"测试工厂A\"},{\"symbolSize\":40,\"name\":\"济南建材市场\"},{\"symbolSize\":40,\"name\":\"深圳分销点\"},{\"symbolSize\":40,\"name\":\"物流\"},{\"symbolSize\":40,\"name\":\"仓库\"},{\"symbolSize\":40,\"name\":\"北京配送站\"},{\"symbolSize\":40,\"name\":\"区域配送站\"},{\"symbolSize\":40,\"name\":\"石家庄机电城\"},{\"symbolSize\":40,\"name\":\"无锡分销中心\"},{\"symbolSize\":40,\"name\":\"苏州传感器基地\"},{\"symbolSize\":40,\"name\":\"苏州工业园\"},{\"symbolSize\":40,\"name\":\"重庆法兰制造\"},{\"symbolSize\":40,\"name\":\"长春汽车配件城\"},{\"symbolSize\":40,\"name\":\"天津阀门制造\"}],\"range\":\"30d\",\"links\":[{\"source\":\"12\",\"target\":\"重庆\"},{\"source\":\"上海仓库A\",\"target\":\"发往物流中心\"},{\"source\":\"上海化工园仓库\",\"target\":\"昆明建材城\"},{\"source\":\"上海化工园仓库\",\"target\":\"石家庄电机厂\"},{\"source\":\"上海智能仓\",\"target\":\"上海化工园仓库\"},{\"source\":\"上海生产车间A\",\"target\":\"上海化工园仓库\"},{\"source\":\"上海生产车间A\",\"target\":\"太原传感器基地\"},{\"source\":\"上海生产车间A\",\"target\":\"杭州生产基地\"},{\"source\":\"东莞电机制造\",\"target\":\"东莞电机制造\"},{\"source\":\"东莞电机制造\",\"target\":\"福州电机厂\"},{\"source\":\"东莞电机制造\",\"target\":\"郑州仓储中心\"},{\"source\":\"东莞电机制造\",\"target\":\"青岛港仓库\"},{\"source\":\"中心仓库\",\"target\":\"物流中心\"},{\"source\":\"佛山变频电机厂\",\"target\":\"佛山变频电机厂\"},{\"source\":\"佛山变频电机厂\",\"target\":\"杭州成品仓\"},{\"source\":\"佛山成品库\",\"target\":\"福州电机厂\"},{\"source\":\"兰州钢管厂\",\"target\":\"兰州钢管厂\"},{\"source\":\"兰州钢管厂\",\"target\":\"天津港仓库\"},{\"source\":\"兰州钢管厂\",\"target\":\"太原工业园仓库\"},{\"source\":\"兰州钢管厂\",\"target\":\"昆明建材城\"},{\"source\":\"兰州钢管厂\",\"target\":\"青岛港仓库\"},{\"source\":\"厦门物流中心\",\"target\":\"厦门物流中心\"},{\"source\":\"厦门物流中心\",\"target\":\"成都建材城\"},{\"source\":\"厦门物流中心\",\"target\":\"长沙机械厂\"},{\"source\":\"哈尔滨轴承厂\",\"target\":\"南京物流枢纽\"},{\"source\":\"哈尔滨轴承厂\",\"target\":\"武汉物流枢纽\"},{\"source\":\"天津港仓库\",\"target\":\"贵阳阀门制造\"},{\"source\":\"太原传感器基地\",\"target\":\"太原传感器基地\"},{\"source\":\"太原传感器基地\",\"target\":\"福州电机厂\"},{\"source\":\"太原传感器基地\",\"target\":\"长沙机械厂\"},{\"source\":\"太原工业园仓库\",\"target\":\"太原工业园仓库\"},{\"source\":\"宁波港仓库\",\"target\":\"宁波港仓库\"},{\"source\":\"宁波港仓库\",\"target\":\"杭州石化仓库\"},{\"source\":\"宁波港仓库\",\"target\":\"重庆物流园\"},{\"source\":\"宁波港仓库\",\"target\":\"青岛港仓库\"},{\"source\":\"广州电机工厂\",\"target\":\"兰州钢管厂\"},{\"source\":\"成都建材城\",\"target\":\"成都化工仓\"},{\"source\":\"成都建材城\",\"target\":\"成都建材城\"},{\"source\":\"成都阀门基地\",\"target\":\"成都化工仓\"},{\"source\":\"成都阀门基地\",\"target\":\"成都建材城\"},{\"source\":\"成都阀门基地\",\"target\":\"西安阀门集团\"},{\"source\":\"昆明建材城\",\"target\":\"郑州仓储中心\"},{\"source\":\"昆明管道厂\",\"target\":\"昆明建材城\"},{\"source\":\"昆明管道厂\",\"target\":\"昆明管道厂\"},{\"source\":\"昆明管道厂\",\"target\":\"郑州仓储中心\"},{\"source\":\"昆明管道厂\",\"target\":\"长沙机械厂\"},{\"source\":\"杭州成品仓\",\"target\":\"宁波港仓库\"},{\"source\":\"杭州成品仓\",\"target\":\"杭州成品仓\"},{\"source\":\"杭州成品仓\",\"target\":\"杭州生产基地\"},{\"source\":\"杭州生产基地\",\"target\":\"杭州生产基地\"},{\"source\":\"杭州生产基地\",\"target\":\"杭州石化仓库\"},{\"source\":\"武汉物流枢纽\",\"target\":\"杭州成品仓\"},{\"source\":\"武汉物流枢纽\",\"target\":\"武汉物流枢纽\"},{\"source\":\"沈阳仓储中心\",\"target\":\"沈阳仓储中心\"},{\"source\":\"沈阳仓储中心\",\"target\":\"沈阳机械市场\"},{\"source\":\"沈阳仓储中心\",\"target\":\"重庆物流园\"},{\"source\":\"沈阳机械市场\",\"target\":\"兰州钢管厂\"},{\"source\":\"沈阳机械市场\",\"target\":\"沈阳仓储中心\"},{\"source\":\"沈阳机械市场\",\"target\":\"沈阳机械市场\"},{\"source\":\"沈阳机械市场\",\"target\":\"洛阳LYC轴承\"},{\"source\":\"洛阳LYC轴承\",\"target\":\"佛山成品库\"},{\"source\":\"洛阳LYC轴承\",\"target\":\"洛阳LYC轴承\"},{\"source\":\"测试工厂A\",\"target\":\"中心仓库\"},{\"source\":\"济南建材市场\",\"target\":\"沈阳仓储中心\"},{\"source\":\"济南建材市场\",\"target\":\"济南建材市场\"},{\"source\":\"深圳分销点\",\"target\":\"哈尔滨轴承厂\"},{\"source\":\"深圳分销点\",\"target\":\"深圳分销点\"},{\"source\":\"物流\",\"target\":\"仓库\"},{\"source\":\"物流中心\",\"target\":\"北京配送站\"},{\"source\":\"物流中心\",\"target\":\"区域配送站\"},{\"source\":\"石家庄机电城\",\"target\":\"宁波港仓库\"},{\"source\":\"石家庄电机厂\",\"target\":\"太原工业园仓库\"},{\"source\":\"石家庄电机厂\",\"target\":\"成都建材城\"},{\"source\":\"石家庄电机厂\",\"target\":\"成都阀门基地\"},{\"source\":\"石家庄电机厂\",\"target\":\"石家庄机电城\"},{\"source\":\"石家庄电机厂\",\"target\":\"石家庄电机厂\"},{\"source\":\"福州电机厂\",\"target\":\"上海生产车间A\"},{\"source\":\"福州电机厂\",\"target\":\"无锡分销中心\"},{\"source\":\"福州电机厂\",\"target\":\"杭州成品仓\"},{\"source\":\"苏州传感器基地\",\"target\":\"苏州传感器基地\"},{\"source\":\"苏州传感器基地\",\"target\":\"重庆物流园\"},{\"source\":\"苏州工业园\",\"target\":\"太原传感器基地\"},{\"source\":\"苏州工业园\",\"target\":\"苏州传感器基地\"},{\"source\":\"西安阀门集团\",\"target\":\"宁波港仓库\"},{\"source\":\"西安阀门集团\",\"target\":\"西安阀门集团\"},{\"source\":\"贵阳阀门制造\",\"target\":\"哈尔滨轴承厂\"},{\"source\":\"贵阳阀门制造\",\"target\":\"贵阳阀门制造\"},{\"source\":\"贵阳阀门制造\",\"target\":\"郑州仓储中心\"},{\"source\":\"郑州仓储中心\",\"target\":\"郑州仓储中心\"},{\"source\":\"重庆法兰制造\",\"target\":\"苏州传感器基地\"},{\"source\":\"重庆法兰制造\",\"target\":\"重庆法兰制造\"},{\"source\":\"重庆法兰制造\",\"target\":\"重庆物流园\"},{\"source\":\"重庆物流园\",\"target\":\"重庆物流园\"},{\"source\":\"长春汽车配件城\",\"target\":\"天津阀门制造\"},{\"source\":\"长沙机械厂\",\"target\":\"长沙机械厂\"},{\"source\":\"青岛港仓库\",\"target\":\"长春汽车配件城\"},{\"source\":\"青岛港仓库\",\"target\":\"青岛港仓库\"}]}}"
}
GET http://localhost:8080/api/dashboard/topology?trace_code=62861642-907e-43ed-b316-74b7e2a511d9: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "2d80555d-6a3f-41eb-afb0-811ab23be87e",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"nodes\":[{\"symbolSize\":40,\"name\":\"测试工厂A\"},{\"symbolSize\":40,\"name\":\"中心仓库\"},{\"symbolSize\":40,\"name\":\"物流中心\"},{\"symbolSize\":40,\"name\":\"区域配送站\"}],\"range\":\"30d\",\"links\":[{\"source\":\"测试工厂A\",\"target\":\"中心仓库\"},{\"source\":\"中心仓库\",\"target\":\"物流中心\"},{\"source\":\"物流中心\",\"target\":\"区域配送站\"}]}}"
}
POST http://localhost:8080/api/auth/register: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62607
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "b9cd08c2-efe1-4a1c-9020-810e21d26042",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "44"
  },
  "Request Body": "{\"username\": \"ab\", \"password\": \"Test123456\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"username: 用户名长度应为3-20个字符\"}"
}
POST http://localhost:8080/api/auth/register: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62612
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "e872d87b-f65d-47f7-90d1-1eaf29c6c27e",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "48"
  },
  "Request Body": "{\"username\": \"testuser123\", \"password\": \"12345\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"password: 密码长度应为6-50个字符; password: 密码必须包含至少一个字母和一个数字\"}"
}
POST http://localhost:8080/api/auth/register: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62614
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "0b28074e-3637-470d-bce7-ba86acc58a47",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "51"
  },
  "Request Body": "{\"username\": \"testuser123\", \"password\": \"12345678\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"password: 密码必须包含至少一个字母和一个数字\"}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62616
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "803bf478-760c-4ddf-a469-7c0eee9febd0",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "59"
  },
  "Request Body": "{\"spu_id\": 1, \"quantity\": 0, \"manufacturer_node\": \"测试\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"quantity: 最小不能小于1\"}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62618
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "96c59f4d-13b1-479f-b1a2-cee4bb0fbc53",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "60"
  },
  "Request Body": "{\"spu_id\": 1, \"quantity\": -5, \"manufacturer_node\": \"测试\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"quantity: 最小不能小于1\"}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62620
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "00b8dbc7-5e33-4b24-a178-3faadc59d8cf",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "2"
  },
  "Request Body": "{}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:11 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10001,\"status\":400,\"message\":\"quantity: 最小不能小于1\"}"
}
GET http://localhost:8080/api/users?page=-1&size=0: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62622
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "1e4a9d8f-c3d9-4449-b6a6-9d3e191e425e",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"list\":[],\"total\":7,\"page\":-1,\"size\":0,\"total_pages\":2147483647}}"
}
GET http://localhost:8080/api/users?username=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62622
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "f29d5807-218d-4800-b207-7d067e1e2b95",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"list\":[],\"total\":0,\"page\":1,\"size\":10,\"total_pages\":0}}"
}
GET http://localhost:8080/api/users?username=%3C%3E%22%27%26;|: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62622
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "ff94bcc5-d6dc-4ed8-b6c8-a1e96a7ad059",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "content-type": "text/html;charset=utf-8",
    "content-language": "en",
    "content-length": "435",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "connection": "close"
  },
  "Response Body": "<!doctype html><html lang=\"en\"><head><title>HTTP Status 400 – Bad Request</title><style type=\"text/css\">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 400 – Bad Request</h1></body></html>"
}
GET http://localhost:8080/api/users/999999: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62624
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "03618041-50a6-43d1-88a8-744001289ba5",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10004,\"status\":404,\"message\":\"用户不存在\"}"
}
POST http://localhost:8080/api/traces: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62624
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNjAwOGVjOS0wYWRkLTQzMzYtOTJmMS0wNmYxZTRkNGIwNzEiLCJzdWIiOiJwcm9kdWNlciIsInVzZXJuYW1lIjoicHJvZHVjZXIiLCJyb2xlIjoiUFJPRFVDRVIiLCJ0b2tlbl92ZXJzaW9uIjoyLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.kZW1a5Opsw-TRX0KNkzK1mYRXY16ro0ZHcJAPjJPRTA",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "6a41c0b3-eb33-47c9-b934-76be5ed78664",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "88"
  },
  "Request Body": "{\"part_code\": \"SPU-VALVE-001\", \"quantity\": 1, \"manufacturer_node\": \"并发测试工厂\"}",
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":201,\"message\":\"赋码成功\",\"data\":{\"generated_count\":1,\"trace_codes\":[\"6972c296-6f2e-4598-857c-9b97ad5c001a\"]}}"
}
POST http://localhost:8080/api/traces/event: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62624
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "content-type": "application/json",
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhODVjZWE4YS02NzgxLTRiNzctYjExZS04MmYxODZiMWI0NzgiLCJzdWIiOiJ3YXJlaG91c2UiLCJ1c2VybmFtZSI6IndhcmVob3VzZSIsInJvbGUiOiJXQVJFSE9VU0UiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.fEWKN2RyD2i-vadjA4Pwq7g79zmdWm86DXfHDpIuM00",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "0e3368af-48c9-4334-9c1b-363ec43852c8",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive",
    "content-length": "151"
  },
  "Request Body": "{\"trace_code\": \"62861642-907e-43ed-b316-74b7e2a511d9\", \"action_type\": \"INBOUND\", \"location\": \"幂等性测试仓库\", \"operator\": \"幂等性测试员\"}",
  "Response Headers": {
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10005,\"status\":500,\"message\":\"服务器异常: HttpRequestMethodNotSupportedException\"}"
}
GET http://localhost:8080/api/traces?page=1&size=100: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62626
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "a5ab674d-9e95-47f4-92fd-02c7c3294227",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10005,\"status\":500,\"message\":\"服务器异常: HttpRequestMethodNotSupportedException\"}"
}
GET http://localhost:8080/api/traces?page=1&size=10000: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62628
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "60e7ea32-f372-43bd-b2bc-f168e1667b6f",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10005,\"status\":500,\"message\":\"服务器异常: HttpRequestMethodNotSupportedException\"}"
}
GET http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9/verify: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62630
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "e1846aae-e533-4261-aaa2-4b3212f6f5ee",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"valid\":true,\"total_logs\":4,\"hash_verified_count\":4,\"signature_verified_count\":4,\"anchor_hash\":\"433bd390e5420a8f759c1a99c8a9743d1485a80800ae0adcdb0d4c027dfaad1c\",\"anchor_signature\":\"iPbePeaE2aCYad+UbKINFloRT08n2SDQfTjjovqwE17M/P2EwvDiCeIn5T/ic8xOsE87RwnViH3fipHBIz4CIAkatnKiyRnqvZ/dC3pvGDhIo8MBvt/BPyT2aXZiapdBLMdJWagMilSY/8ogaKO4Td0RVX5J3RiP7xZUtQyzuka97/HewbrgLDuDDh8PSe9/6NlmcSHOl4K1wRvLzmeF3n+8MJt6WwCNYQRXWKdrFZ/Dxx2BFbVbW5PrZy2YiZdeN5R2FeCfcxe0iUuZ8l0zB5xOPIDOdLEVYav62nFxpkdDiVAlLXjrl9NPU/STaEs8NUvoD7VniMIok+02n6XWQQ==\",\"public_key\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0etR/XYGtMzGaziHWV0QQlp66pOVA13ekBuHvnFttsF2xMi9BJzexQQG0q2X8LkBBr7EBBJw//Tp3qM/n9TykkNHt2AIPvgJLAb2ib9pS3EUXlEq5bsgtnzyw/j8fmudTAcvHDmu2G1PIUYl5/OuVkE7/58GzjZjXRxNVTSHtp0aqPT5JZfC1S4lvRkq2n1jyFBQLz53z6K/c1n1JosmZ6/wnsArOqoVxt2jE3KAHtuRZxHLDb4wuk27AxKg/qCGuT32I19OjKuQ6T7ho5fRlpsteB+i1o/NHLvBVB5SLWTcBSpEZukDzPwX8UR72IolWPJSyokP7LEDFu69O1aA8QIDAQAB\",\"errors\":[],\"verify_time\":\"2026-02-02T00:25:12.832936900\",\"verify_duration_ms\":4}}"
}
GET http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62630
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "28c95a2b-a4d4-4f28-a8d8-e2fb9468f189",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:12 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"success\",\"data\":{\"snapshot\":{\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"current_status\":\"TRANSFERRED\",\"current_node\":\"区域配送站\",\"current_owner\":\"区域配送站\",\"province\":\"上海市\",\"city\":\"上海市\",\"last_event_time\":\"2026-02-02T00:25:07\",\"last_log_id\":561,\"last_hash\":\"433bd390e5420a8f759c1a99c8a9743d1485a80800ae0adcdb0d4c027dfaad1c\",\"version\":3,\"update_time\":\"2026-02-02T00:25:07\"},\"history\":[{\"id\":556,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"INIT\",\"from_node\":null,\"to_node\":\"测试工厂A\",\"province\":\"浙江省\",\"city\":\"杭州市\",\"event_time\":\"2026-02-02T00:25:06\",\"ingest_time\":\"2026-02-02T00:25:06\",\"prev_hash\":\"GENESIS\",\"current_hash\":\"33b7315fa285246dd8fda565bd1434af2ab60f36fcbacfbbbf40961ec079b615\",\"correction_of\":null,\"operator\":\"producer\",\"signature\":\"YlzcRK7civnHVutmUaqCfgMCzigPYwTiezBp0wle3obFt6uEa/spxcQ205M9bf4LZCdjH/yV3/6sb/sr/zdZAtji9SK4sS0QhEhEevARThgwU74BQGy8SG6qjUXXcUaVrIgk4GPQGfrfehE8DCPKZxwCHMzTs5oxsSiKHz+LVxuXAwJl6+9f09/MyUUCW6mNqpr7xiCxxXZAMs8xLWgzFnsicA/wLRH/YT6Zni6iwI+GbuoEg+EuaPZewgKy+ExQ8iMgnF6rY1GTQxffI0VcF7X1da9+8ceHYk4DynPELrYr3JwA20RFg8VEPwk7BsllEPUgyrdhFMdW2bPaj4nQyA==\",\"create_time\":\"2026-02-02T00:25:07\"},{\"id\":559,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"INBOUND\",\"from_node\":\"测试工厂A\",\"to_node\":\"中心仓库\",\"province\":\"江苏省\",\"city\":\"苏州市\",\"event_time\":\"2026-02-02T00:25:07\",\"ingest_time\":\"2026-02-02T00:25:07\",\"prev_hash\":\"33b7315fa285246dd8fda565bd1434af2ab60f36fcbacfbbbf40961ec079b615\",\"current_hash\":\"a8e4bbf43ed8b28ada13c530a181f6d2e1863c99442cabf8c2ffc9b5e44a013f\",\"correction_of\":null,\"operator\":\"warehouse\",\"signature\":\"MR+h1iEAMhBxoWRvVqop2cUO5axYLSSv86w1mhxKh7KshaQSsUCYVKesThwmqQlygkxzPjxulLtazd2snMjIKWTkQYr0FWrmfnbHTFuiKMN8u6Trx33iQ5hZ1ZACzD7CNQS2bcdSE+TZb1HWRfY63Uby40dP5pA1tWOw/HcD9VlT5bWkbuLQWOXxaneRj0mcW8N/wJTYsleUnE5RYnCGGF4QhfSz74c6Dpc/lUjXQcr3WUb9YI7ux6bR7zMfz371zjtVTr93kjW6+v1VEdhZnbhCzSdzbVIIjw2d2gtCqzfSAupRT5ma7l8juLjhM74oQR/yF1xFhDkm6KBU+ZHX8Q==\",\"create_time\":\"2026-02-02T00:25:07\"},{\"id\":560,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"OUTBOUND\",\"from_node\":\"中心仓库\",\"to_node\":\"物流中心\",\"province\":\"江苏省\",\"city\":\"南京市\",\"event_time\":\"2026-02-02T00:25:07\",\"ingest_time\":\"2026-02-02T00:25:07\",\"prev_hash\":\"a8e4bbf43ed8b28ada13c530a181f6d2e1863c99442cabf8c2ffc9b5e44a013f\",\"current_hash\":\"4e6dd0b866f9b53b65ea846e848c2418a1e80ac88926f90177c49e6819fe70e8\",\"correction_of\":null,\"operator\":\"warehouse\",\"signature\":\"sGuFrLRQ0i/HjHUqdkqolCIEifqaDR2FEVmbYDiG01J7nhkmhUQNYhXwIDfjK+oPmobZvABqTZ9j5Sg1FX8pdf15P7JEaPKpdu6U186zu4EQlt1KmYohkyqHbc5CiNHFpCzryz8UM1fBdH+XYD6mB5Xz1+7NhzOQr9V2/IeGQa2Z9t9hAYUhgvDjOs9KWyjmsZ01Hi9D+9tY0Hgl14NBy7U0Rrd0mrRXFNM+5siKGsUUWCensmZDRJWHcftJmjsnvfG/WFVbETrkIgsExNMR14XwfzrB3tTCpuPrxn6O32JmdOVSFFIJe2gXq6bknXyYhT1nNBRUsOjJMlq+z49gEA==\",\"create_time\":\"2026-02-02T00:25:08\"},{\"id\":561,\"trace_code\":\"62861642-907e-43ed-b316-74b7e2a511d9\",\"spu_id\":1,\"action_type\":\"TRANSFER\",\"from_node\":\"物流中心\",\"to_node\":\"区域配送站\",\"province\":\"上海市\",\"city\":\"上海市\",\"event_time\":\"2026-02-02T00:25:07\",\"ingest_time\":\"2026-02-02T00:25:07\",\"prev_hash\":\"4e6dd0b866f9b53b65ea846e848c2418a1e80ac88926f90177c49e6819fe70e8\",\"current_hash\":\"433bd390e5420a8f759c1a99c8a9743d1485a80800ae0adcdb0d4c027dfaad1c\",\"correction_of\":null,\"operator\":\"logistics\",\"signature\":\"iPbePeaE2aCYad+UbKINFloRT08n2SDQfTjjovqwE17M/P2EwvDiCeIn5T/ic8xOsE87RwnViH3fipHBIz4CIAkatnKiyRnqvZ/dC3pvGDhIo8MBvt/BPyT2aXZiapdBLMdJWagMilSY/8ogaKO4Td0RVX5J3RiP7xZUtQyzuka97/HewbrgLDuDDh8PSe9/6NlmcSHOl4K1wRvLzmeF3n+8MJt6WwCNYQRXWKdrFZ/Dxx2BFbVbW5PrZy2YiZdeN5R2FeCfcxe0iUuZ8l0zB5xOPIDOdLEVYav62nFxpkdDiVAlLXjrl9NPU/STaEs8NUvoD7VniMIok+02n6XWQQ==\",\"create_time\":\"2026-02-02T00:25:08\"}]}}"
}
GET http://localhost:8080/api/traces/62861642-907e-43ed-b316-74b7e2a511d9/logs: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62630
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjdjZmM3ZS1jNjFmLTQ5YjUtYmE4Mi02MTYzYjU2ZjgzNWIiLCJzdWIiOiJ1c2VyIiwidXNlcm5hbWUiOiJ1c2VyIiwicm9sZSI6IlVTRVIiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDMsImV4cCI6MTc3MDA0OTUwM30.xn77hFs_QknyLs9drDXfwreGmqEzCVZ1xC3o3IFjUfM",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "93660f08-77eb-4142-8690-63ecd9727da6",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:13 GMT",
    "connection": "close"
  },
  "Response Body": "{\"code\":10005,\"status\":500,\"message\":\"服务器异常: NoResourceFoundException\"}"
}
GET http://localhost:8080/api/auth/me: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62632
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWVlYmRlYy0zYzRlLTQ3NzQtOTY3My1jNWY3ODNjNmNlODciLCJzdWIiOiJzdXBlcmFkbWluIiwidXNlcm5hbWUiOiJzdXBlcmFkbWluIiwicm9sZSI6IlNVUEVSX0FETUlOIiwidG9rZW5fdmVyc2lvbiI6MCwiaWF0IjoxNzY5OTYzMTA0LCJleHAiOjE3NzAwNDk1MDR9.o5LGWRr07b_XmYd7Qgw9g9IjPRDjUkwEMByD4jvtaZs",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "3a810b11-1654-4129-bc79-52ed2d5227a4",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:13 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"获取用户信息成功\",\"data\":{\"id\":1,\"username\":\"superadmin\",\"role_code\":\"SUPER_ADMIN\",\"role_name\":\"超级管理员\",\"permissions\":[\"user:view\",\"role:manage\",\"trace:create\",\"dashboard:view\",\"trace:view\",\"role:view\",\"user:manage\",\"part:manage\",\"trace:scan\",\"part:view\"],\"status\":1,\"create_time\":\"2026-01-17 18:10:27\"}}"
}
GET http://localhost:8080/api/auth/me: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62632
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5YjQxNDE5ZS02ODgxLTQyMzAtYjUwNy02MzNkZWIyYzE5MTgiLCJzdWIiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NjMxMDIsImV4cCI6MTc3MDA0OTUwMn0.l06ipi1L913Qral-MA-XuITCY5LImECoojOxdZtLyYQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "7d1102c7-96af-4c56-a31a-5a89e846bb06",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json",
    "transfer-encoding": "chunked",
    "date": "Sun, 01 Feb 2026 16:25:13 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":0,\"status\":200,\"message\":\"获取用户信息成功\",\"data\":{\"id\":2,\"username\":\"admin\",\"role_code\":\"ADMIN\",\"role_name\":\"系统管理员\",\"permissions\":[\"user:view\",\"role:manage\",\"trace:create\",\"dashboard:view\",\"trace:view\",\"role:view\",\"user:manage\",\"part:manage\",\"trace:scan\",\"part:view\"],\"status\":1,\"create_time\":\"2026-01-17 18:10:27\"}}"
}
GET http://localhost:8080/api/users: {
  "Network": {
    "addresses": {
      "local": {
        "address": "::1",
        "family": "IPv6",
        "port": 62632
      },
      "remote": {
        "address": "::1",
        "family": "IPv6",
        "port": 8080
      }
    }
  },
  "Request Headers": {
    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZGZhYzAzOC1kZWJiLTQ5ZTgtOTE0YS05MjUzYmUzOTA0MWUiLCJzdWIiOiJsb2dpc3RpY3MiLCJ1c2VybmFtZSI6ImxvZ2lzdGljcyIsInJvbGUiOiJMT0dJU1RJQ1MiLCJ0b2tlbl92ZXJzaW9uIjowLCJpYXQiOjE3Njk5NTIxNjAsImV4cCI6MTc3MDAzODU2MH0.YZgSIHQ2aicDoGbO34clUjXwPy11LV-t3pwCoBKiplQ",
    "user-agent": "PostmanRuntime/7.51.0",
    "accept": "*/*",
    "postman-token": "fe72f746-fbb4-4a30-b0c4-b12a940a2a10",
    "host": "localhost:8080",
    "accept-encoding": "gzip, deflate, br",
    "connection": "keep-alive"
  },
  "Response Headers": {
    "vary": [
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ],
    "content-type": "application/json;charset=UTF-8",
    "content-length": "64",
    "date": "Sun, 01 Feb 2026 16:25:13 GMT",
    "keep-alive": "timeout=60",
    "connection": "keep-alive"
  },
  "Response Body": "{\"code\":10003,\"status\":403,\"message\":\"无权限执行此操作\"}"
}