-- SkinAI doctor-facing schema V2
-- Target: MySQL 8+
-- Note:
-- 1) This file is additive and migration-friendly.
-- 2) Existing legacy tables (family/report) are kept for transition and can be retired later.

SET NAMES utf8mb4;

-- ==========================================
-- Helper procedures for MySQL-compatible DDL
-- ==========================================
DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `sp_create_index_if_missing`;

DELIMITER $$

CREATE PROCEDURE `sp_add_column_if_missing`(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_ddl TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `sp_create_index_if_missing`(
  IN p_table_name VARCHAR(64),
  IN p_index_name VARCHAR(64),
  IN p_index_ddl TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @ddl = CONCAT('CREATE INDEX `', p_index_name, '` ON `', p_table_name, '` ', p_index_ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

-- =========================
-- 1. Doctor account extension
-- =========================
CALL sp_add_column_if_missing('user', 'job_number', 'job_number VARCHAR(64) NULL COMMENT ''doctor job number'' AFTER `mobile`');
CALL sp_add_column_if_missing('user', 'password_hash', 'password_hash VARCHAR(128) NULL COMMENT ''MD5 hash for password login'' AFTER `job_number`');
CALL sp_add_column_if_missing('user', 'department', 'department VARCHAR(64) NULL COMMENT ''doctor department'' AFTER `password_hash`');
CALL sp_add_column_if_missing('user', 'title', 'title VARCHAR(64) NULL COMMENT ''doctor title'' AFTER `department`');
CALL sp_add_column_if_missing('user', 'status', 'status VARCHAR(16) NULL DEFAULT ''ONLINE'' COMMENT ''ONLINE/OFFLINE'' AFTER `title`');

CALL sp_create_index_if_missing('user', 'idx_user_mobile', '(mobile)');
CALL sp_create_index_if_missing('user', 'idx_user_job_number', '(job_number)');

-- =========================
-- 2. Patient profile
-- =========================
CREATE TABLE IF NOT EXISTS `patient`
(
    `id`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `doctor_id`      BIGINT UNSIGNED NOT NULL COMMENT 'owner doctor id',
    `patient_name`   VARCHAR(100)    NOT NULL,
    `gender`         VARCHAR(16)     NULL,
    `age`            INT             NULL,
    `phone`          VARCHAR(32)     NULL,
    `id_card_masked` VARCHAR(64)     NULL,
    `notes`          TEXT            NULL,
    `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_patient_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL sp_create_index_if_missing('patient', 'idx_patient_doctor_name', '(doctor_id, patient_name)');

-- =========================
-- 3. Medical case (core business)
-- =========================
CREATE TABLE IF NOT EXISTS `medical_case`
(
    `id`                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `doctor_id`         BIGINT UNSIGNED NOT NULL,
    `patient_id`        BIGINT UNSIGNED NOT NULL,
    `case_no`           VARCHAR(64)     NOT NULL COMMENT 'business case number',
    `status`            VARCHAR(24)     NOT NULL DEFAULT 'PENDING'
                      COMMENT 'PENDING/IN_PROGRESS/FOLLOWUP/DONE/CLOSED',
    `chief_complaint`   TEXT            NULL,
    `present_history`   TEXT            NULL,
    `treatment_history` TEXT            NULL,
    `duration`          VARCHAR(100)    NULL,
    `extra_notes`       TEXT            NULL,
    `diagnosed_type`    VARCHAR(128)    NULL,
    `ai_advice`         TEXT            NULL,
    `ai_introduction`   TEXT            NULL,
    `check_time`        DATETIME        NULL,
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uq_medical_case_case_no` UNIQUE (`case_no`),
    CONSTRAINT `fk_medical_case_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user`(`id`),
    CONSTRAINT `fk_medical_case_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL sp_create_index_if_missing('medical_case', 'idx_medical_case_doctor_status_time', '(doctor_id, status, created_at)');
CALL sp_create_index_if_missing('medical_case', 'idx_medical_case_patient_time', '(patient_id, created_at)');

-- =========================
-- 4. Case images
-- =========================
CREATE TABLE IF NOT EXISTS `case_image`
(
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `case_id`    BIGINT UNSIGNED NOT NULL,
    `object_key` VARCHAR(255)    NOT NULL COMMENT 'OSS object key',
    `public_url` TEXT            NULL,
    `is_primary` TINYINT(1)      NOT NULL DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_case_image_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL sp_create_index_if_missing('case_image', 'idx_case_image_case', '(case_id)');

-- =========================
-- 5. Case analysis result (model output)
-- =========================
CREATE TABLE IF NOT EXISTS `case_analysis`
(
    `id`                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `case_id`           BIGINT UNSIGNED NOT NULL,
    `disease_index`     INT             NULL,
    `disease_type`      VARCHAR(128)    NULL,
    `confidence`        DOUBLE          NULL,
    `model_version`     VARCHAR(64)     NULL,
    `topk_indices_json` JSON            NULL,
    `topk_scores_json`  JSON            NULL,
    `raw_response_json` JSON            NULL,
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_case_analysis_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL sp_create_index_if_missing('case_analysis', 'idx_case_analysis_case', '(case_id)');

-- =========================
-- 6. Case concept score (render-ready)
-- =========================
CREATE TABLE IF NOT EXISTS `case_concept_score`
(
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `case_id`         BIGINT UNSIGNED NOT NULL,
    `concept_index`   INT             NOT NULL,
    `concept_name_en` VARCHAR(128)    NULL,
    `concept_name_cn` VARCHAR(128)    NULL,
    `score`           DOUBLE          NOT NULL,
    `rank_no`         INT             NOT NULL,
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_case_concept_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL sp_create_index_if_missing('case_concept_score', 'idx_case_concept_case_rank', '(case_id, rank_no)');

-- =========================
-- 7. Follow-up record
-- =========================
CREATE TABLE IF NOT EXISTS `case_followup`
(
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `case_id`       BIGINT UNSIGNED NOT NULL,
    `followup_time` DATETIME        NOT NULL,
    `summary`       TEXT            NULL,
    `next_plan`     TEXT            NULL,
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_case_followup_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL sp_create_index_if_missing('case_followup', 'idx_case_followup_case_time', '(case_id, followup_time)');

-- =========================
-- 8. Optional: announcement placeholder table (UI decoration only)
-- =========================
CREATE TABLE IF NOT EXISTS `announcement`
(
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title`      VARCHAR(128) NOT NULL,
    `content`    TEXT         NULL,
    `status`     VARCHAR(16)  NOT NULL DEFAULT 'DISABLED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cleanup helper procedures (optional)
DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `sp_create_index_if_missing`;

-- =========================
-- 9. Migration suggestions (manual)
-- =========================
-- A. report -> medical_case/case_analysis mapping should be done by a one-time migration script.
-- B. family table can be deprecated after patient domain is fully adopted.
-- C. Keep old APIs as compatibility layer during front-end transition.
