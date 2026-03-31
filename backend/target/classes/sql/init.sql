-- =========================================================
-- English Learning - init.sql
-- 当前版本初始化/对齐脚本（结构化义项 + 文档测验）
-- 说明：
-- 1) 兼容已有库：使用 IF NOT EXISTS
-- 2) 本次已弃用 learn_card_note，执行 DROP
-- =========================================================

-- 0) 清理弃用表（原卡片注释）
DROP TABLE IF EXISTS learn_card_note;

-- 1) 义项表（一词多义）
CREATE TABLE IF NOT EXISTS learn_card_sense (
  id BIGINT NOT NULL AUTO_INCREMENT,
  card_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  label TEXT NULL,
  translation_zh TEXT NULL,
  explanation_en TEXT NULL,
  tone TEXT NULL,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_learn_card_sense_card_id (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) 例句表（题源）
CREATE TABLE IF NOT EXISTS learn_card_example (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sense_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  sentence_en TEXT NOT NULL,
  sentence_zh TEXT NULL,
  scenario_tag TEXT NULL,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_learn_card_example_sense_id (sense_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) 同义词表（仅展示，无例句）
CREATE TABLE IF NOT EXISTS learn_card_synonym (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sense_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  lemma TEXT NOT NULL,
  note_zh TEXT NULL,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_learn_card_synonym_sense_id (sense_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) 卡片全局扩展块（搭配/提示/高阶句）
CREATE TABLE IF NOT EXISTS learn_card_global_extra (
  id BIGINT NOT NULL AUTO_INCREMENT,
  card_id BIGINT NOT NULL,
  collocations_json TEXT NULL,
  native_tip TEXT NULL,
  high_level_en TEXT NULL,
  high_level_zh TEXT NULL,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_global_extra_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) 文档测验会话
CREATE TABLE IF NOT EXISTS learn_quiz_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  total_count INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  gmt_create DATETIME NOT NULL,
  completed_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_quiz_session_user_doc (user_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6) 文档测验题项
CREATE TABLE IF NOT EXISTS learn_quiz_session_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  card_id BIGINT NOT NULL,
  example_id BIGINT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  prompt_text TEXT NULL,
  options_json TEXT NULL,
  expected_text TEXT NULL,
  user_answer TEXT NULL,
  is_correct TINYINT(1) NULL,
  answered_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_quiz_item_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
