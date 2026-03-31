-- =========================================================
-- migrate: backfill learn_card_source from learn_card
-- target: logically deprecate learn_card.document_id
-- =========================================================

-- 1) Ensure association table exists (safe rerun)
CREATE TABLE IF NOT EXISTS learn_card_source (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  start_offset INT NULL,
  end_offset INT NULL,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_card_source_user_doc (user_id, document_id),
  KEY idx_card_source_card_id (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) Optional: prevent duplicated same-source rows (run once)
-- CREATE UNIQUE INDEX uk_card_source_user_card_doc_range
-- ON learn_card_source(user_id, card_id, document_id, start_offset, end_offset);

-- 3) Backfill old learn_card.document_id associations into learn_card_source
INSERT INTO learn_card_source (
  user_id, card_id, document_id, start_offset, end_offset, gmt_create, gmt_modified
)
SELECT
  c.user_id,
  c.id AS card_id,
  c.document_id,
  c.start_offset,
  c.end_offset,
  NOW(),
  NOW()
FROM learn_card c
WHERE c.document_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM learn_card_source cs
    WHERE cs.user_id = c.user_id
      AND cs.card_id = c.id
      AND cs.document_id = c.document_id
      AND (cs.start_offset <=> c.start_offset)
      AND (cs.end_offset <=> c.end_offset)
  );

-- 4) Validation query (optional)
-- SELECT COUNT(*) AS legacy_rows FROM learn_card WHERE document_id IS NOT NULL;
-- SELECT COUNT(DISTINCT card_id) AS source_cards FROM learn_card_source;

-- 5) After application deployed with source-only logic:
--    you can safely remove old column in a separate release:
-- ALTER TABLE learn_card DROP COLUMN document_id;
-- ALTER TABLE learn_card_source DROP COLUMN context_sentence;
