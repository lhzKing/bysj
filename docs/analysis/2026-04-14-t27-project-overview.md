# T27 Project Overview

## Focus Area

T27 is limited to the backend error handling path:

- `BizCode` defines business codes but currently does not own the HTTP mapping contract.
- `BizException` infers HTTP status locally, with incomplete coverage for newer codes.
- `GlobalExceptionHandler` is the final response envelope boundary.
- Controllers / services mix implicit and explicit `BizException` construction styles.
- `SignatureUtil` still throws raw `RuntimeException` in two key failure paths.

## Runtime Boundary

- Request enters controller / interceptor.
- Business code throws `BizException` (or another runtime exception).
- `GlobalExceptionHandler` translates exceptions into unified `ApiResponse` payloads.
- Frontend already unwraps `code` + `message`; this task avoids response-shape changes.

## Desired End State

- `BizCode` becomes the single default source of HTTP status inference.
- `BizException` delegates to that mapping.
- Call sites only specify custom status when semantics intentionally differ from the default.
- Error responses remain backward-compatible in shape.
