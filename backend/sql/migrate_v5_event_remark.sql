-- R-P1-04: persist optional event remark on trace lifecycle logs.
-- Non-blank remarks are included in the log hash and RSA signature payload.

ALTER TABLE trace_lifecycle_log
  ADD COLUMN remark VARCHAR(255) NULL COMMENT '事件备注（纳入非空备注的Hash和签名）' AFTER city;
