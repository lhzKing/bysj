# T04 Module Inventory

| File | Responsibility | Complexity | T04 Relevance |
| --- | --- | --- | --- |
| `backend/src/main/java/com/example/trace/mapper/TraceSnapshotMapper.java` | Snapshot mapper interface | S | Contains the stale custom method |
| `backend/src/main/resources/mapper/TraceMapper.xml` | Custom XML mapping for snapshot mapper | S | Contains only the stale `FOR UPDATE` query |
| `backend/src/main/java/com/example/trace/service/impl/support/TraceScanTransactionService.java` | Trace scan write transaction | M | Proves current flow uses `selectById` + optimistic lock |
| `backend/src/main/java/com/example/trace/config/MybatisPlusConfig.java` | MyBatis-Plus interceptor config | S | Confirms optimistic lock is the active strategy |
| `backend/src/test/java/com/example/trace/service/impl/support/TraceScanTransactionServiceTest.java` | Scan transaction regression test | M | Main verification target |
| `backend/src/test/java/com/example/trace/service/impl/TraceServiceImplTest.java` | Trace detail/read path test | S | Verifies snapshot read path remains stable |
| `backend/src/test/java/com/example/trace/service/impl/TraceDemoDataServiceImplTest.java` | Demo data snapshot insertion test | S | Guards snapshot mapper CRUD compile path |
