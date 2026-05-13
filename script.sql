-- skinAI unified schema (minimal doctor/patient model)
-- MySQL 8.0+
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信 openid',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(64) NULL COMMENT '昵称',
  `avatar` TEXT NULL COMMENT '头像 URL',
  `mobile` VARCHAR(20) NULL COMMENT '手机号',
  `job_number` VARCHAR(64) NULL COMMENT '医工号',
  `password_hash` VARCHAR(128) NULL COMMENT '密码摘要',
  `department` VARCHAR(128) NULL COMMENT '科室',
  `title` VARCHAR(128) NULL COMMENT '职称',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_openid` (`openid`),
  UNIQUE KEY `uk_user_job_number` (`job_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `patient` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `doctor_id` BIGINT UNSIGNED NOT NULL COMMENT 'owner doctor id',
  `patient_name` VARCHAR(100) NOT NULL COMMENT 'patient name',
  `gender` VARCHAR(16) NULL COMMENT 'MALE/FEMALE or 男/女',
  `age` INT NULL COMMENT 'patient age',
  `phone` VARCHAR(32) NULL COMMENT 'patient phone',
  `id_card_masked` VARCHAR(64) NULL COMMENT 'masked id card',
  `notes` TEXT NULL COMMENT 'extra notes',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_patient_doctor_name` (`doctor_id`, `patient_name`),
  CONSTRAINT `fk_patient_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `medical_case` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `doctor_id` BIGINT UNSIGNED NOT NULL COMMENT 'doctor id',
  `patient_id` BIGINT UNSIGNED NOT NULL COMMENT 'patient id',
  `case_no` VARCHAR(64) NOT NULL COMMENT 'business case number',
  `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/FOLLOWUP/DONE/CLOSED',
  `chief_complaint` TEXT NULL,
  `present_history` TEXT NULL,
  `treatment_history` TEXT NULL,
  `duration` VARCHAR(100) NULL,
  `extra_notes` TEXT NULL,
  `diagnosed_type` VARCHAR(128) NULL,
  `ai_advice` TEXT NULL,
  `ai_introduction` TEXT NULL,
  `check_time` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_medical_case_case_no` (`case_no`),
  KEY `idx_medical_case_doctor_status_time` (`doctor_id`, `status`, `created_at`),
  KEY `idx_medical_case_patient_time` (`patient_id`, `created_at`),
  CONSTRAINT `fk_medical_case_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_medical_case_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `case_analysis` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id` BIGINT UNSIGNED NOT NULL,
  `disease_index` INT NULL,
  `disease_type` VARCHAR(128) NULL,
  `confidence` DOUBLE NULL,
  `model_version` VARCHAR(64) NULL,
  `topk_indices_json` JSON NULL,
  `topk_scores_json` JSON NULL,
  `raw_response_json` JSON NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case_analysis_case` (`case_id`),
  CONSTRAINT `fk_case_analysis_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `case_image` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id` BIGINT UNSIGNED NOT NULL,
  `object_key` VARCHAR(255) NOT NULL,
  `public_url` TEXT NULL,
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case_image_case` (`case_id`),
  CONSTRAINT `fk_case_image_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `case_concept_score` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id` BIGINT UNSIGNED NOT NULL,
  `concept_index` INT NOT NULL,
  `concept_name_en` VARCHAR(128) NULL,
  `concept_name_cn` VARCHAR(128) NULL,
  `score` DOUBLE NOT NULL,
  `rank_no` INT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case_concept_case_rank` (`case_id`, `rank_no`),
  CONSTRAINT `fk_case_concept_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `case_followup` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id` BIGINT UNSIGNED NOT NULL,
  `followup_time` DATETIME NOT NULL,
  `summary` TEXT NULL,
  `next_plan` TEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case_followup_case_time` (`case_id`, `followup_time` DESC),
  CONSTRAINT `fk_case_followup_case` FOREIGN KEY (`case_id`) REFERENCES `medical_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `concept_dictionary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `concept_index` INT NOT NULL,
  `name_en` VARCHAR(128) NULL,
  `name_cn` VARCHAR(128) NULL,
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_concept_dictionary_index` (`concept_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `disease_concept_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `disease_type` VARCHAR(128) NOT NULL,
  `concept_index` INT NOT NULL,
  `weight` DOUBLE NOT NULL DEFAULT 1,
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_disease_concept` (`disease_type`, `concept_index`),
  CONSTRAINT `fk_disease_concept_dictionary` FOREIGN KEY (`concept_index`) REFERENCES `concept_dictionary` (`concept_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `chat_id` VARCHAR(64) NOT NULL,
  `title` VARCHAR(128) NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_activity_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_chat_session_user_chat` (`user_id`, `chat_id`),
  CONSTRAINT `fk_user_chat_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `chat_id` VARCHAR(64) NOT NULL,
  `role` VARCHAR(16) NOT NULL,
  `content` TEXT NOT NULL,
  `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_message_chat_create` (`chat_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL,
  `content` TEXT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DISABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_announcement_status_updated` (`status`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TRIGGER IF EXISTS `trg_after_chat_message_insert_update_session_activity`;
DELIMITER $$
CREATE TRIGGER `trg_after_chat_message_insert_update_session_activity`
AFTER INSERT ON `chat_message`
FOR EACH ROW
BEGIN
  UPDATE `user_chat_session`
  SET `last_activity_time` = NEW.`create_time`,
      `updated_at` = NEW.`create_time`
  WHERE `chat_id` = NEW.`chat_id`;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS `trg_before_user_update_prevent_openid_change`;
DELIMITER $$
CREATE TRIGGER `trg_before_user_update_prevent_openid_change`
BEFORE UPDATE ON `user`
FOR EACH ROW
BEGIN
  IF OLD.`openid` <> NEW.`openid` THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'openid cannot be changed';
  END IF;
END$$
DELIMITER ;

-- Drop legacy transitional tables (run only when migration is confirmed)
DROP TABLE IF EXISTS `report_concept_score`;
DROP TABLE IF EXISTS `report`;
DROP TABLE IF EXISTS `family`;

SET FOREIGN_KEY_CHECKS = 1;
