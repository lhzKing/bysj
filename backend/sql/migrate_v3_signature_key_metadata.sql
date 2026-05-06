-- R-P0-02: add signature key metadata for RSA key rotation.
-- Existing rows are backfilled to the current backup key metadata (`default` / 1).
-- If a deployment uses a different current key id/version, update these values before running.

ALTER TABLE trace_lifecycle_log
  ADD COLUMN signature_key_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '签名密钥标识' AFTER signature,
  ADD COLUMN signature_key_version INT NOT NULL DEFAULT 1 COMMENT '签名密钥版本' AFTER signature_key_id;

CREATE INDEX idx_signature_key ON trace_lifecycle_log(signature_key_id, signature_key_version);

UPDATE trace_lifecycle_log
SET signature_key_id = 'default',
    signature_key_version = 1
WHERE signature IS NOT NULL
  AND (signature_key_id IS NULL OR signature_key_id = '' OR signature_key_version IS NULL);
