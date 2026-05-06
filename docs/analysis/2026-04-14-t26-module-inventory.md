# T26 Module Inventory

| File | Duplication Type | Planned Action |
| --- | --- | --- |
| `backend/src/main/java/com/example/trace/mapper/SysUserMapper.java` | repeated `@Select` and `@Results` blocks | extract shared SQL constants and a reusable `@Results` / `@ResultMap` |
| `backend/src/main/resources/mapper/TraceLifecycleLogMapper.xml` | repeated lifecycle-log column projections | extract reusable `<sql>` column fragments |
| `backend/src/main/java/com/example/trace/mapper/TraceLifecycleLogMapper.java` | XML-backed mapper interface | no API change expected |
