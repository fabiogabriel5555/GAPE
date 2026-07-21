-- Activity-log authorization needs the context that existed at write time.
-- The snapshot intentionally has no foreign keys to Organization, Subject,
-- Class_Group or User: a later delete must never erase the audit trail.
-- The two legacy activity-log foreign keys have the same problem: their
-- ON DELETE SET NULL action rewrites an immutable row when a user or session
-- expires.  Audit identifiers are historical values, so detach them before
-- enabling immutability.
DELIMITER $$
DROP PROCEDURE IF EXISTS detach_activity_log_history_foreign_keys$$
CREATE PROCEDURE detach_activity_log_history_foreign_keys()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'activity_log'
          AND constraint_name = 'fk_activity_log_user'
    ) THEN
        ALTER TABLE activity_log DROP FOREIGN KEY fk_activity_log_user;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'activity_log'
          AND constraint_name = 'fk_activity_log_session'
    ) THEN
        ALTER TABLE activity_log DROP FOREIGN KEY fk_activity_log_session;
    END IF;
END$$
CALL detach_activity_log_history_foreign_keys()$$
DROP PROCEDURE detach_activity_log_history_foreign_keys$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS activity_log_scope (
    id_activity_log BIGINT UNSIGNED NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    scope_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_activity_log, scope_type, scope_id),
    KEY idx_activity_log_scope_lookup (scope_type, scope_id, id_activity_log),
    CONSTRAINT fk_activity_log_scope_activity_log
        FOREIGN KEY (id_activity_log) REFERENCES activity_log (id_activity_log)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_activity_log_scope_type
        CHECK (scope_type IN ('ORGANIZATION', 'SUBJECT', 'CLASS_GROUP', 'USER')),
    CONSTRAINT ck_activity_log_scope_id
        CHECK (scope_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Backfill the actor first.  This is the safe fallback for every historical
-- row for which an old, already-deleted contextual entity can no longer be
-- resolved.
INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT id_activity_log, 'USER', id_user
FROM activity_log
WHERE id_user IS NOT NULL AND id_user > 0;

-- Direct contextual rows retained by a version-30 database.
INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'ORGANIZATION', organization.id_organization
FROM activity_log log
JOIN organization
  ON LOWER(TRIM(log.affected_entity_type)) = 'organization'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND organization.id_organization = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'SUBJECT', subject.id_subject
FROM activity_log log
JOIN subject
  ON LOWER(TRIM(log.affected_entity_type)) = 'subject'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND subject.id_subject = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'ORGANIZATION', subject.id_organization
FROM activity_log log
JOIN subject
  ON LOWER(TRIM(log.affected_entity_type)) = 'subject'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND subject.id_subject = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'CLASS_GROUP', class_group.id_class_group
FROM activity_log log
JOIN class_group
  ON LOWER(TRIM(log.affected_entity_type)) = 'class_group'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND class_group.id_class_group = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'SUBJECT', class_group.id_subject
FROM activity_log log
JOIN class_group
  ON LOWER(TRIM(log.affected_entity_type)) = 'class_group'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND class_group.id_class_group = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'ORGANIZATION', course.id_organization
FROM activity_log log
JOIN class_group
  ON LOWER(TRIM(log.affected_entity_type)) = 'class_group'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND class_group.id_class_group = CAST(log.affected_entity_identifier AS UNSIGNED)
JOIN course ON course.id_course = class_group.id_course;

-- Lessons, blocks and attendance inherit their class-group context.
INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'CLASS_GROUP', class_group.id_class_group
FROM activity_log log
JOIN lesson
  ON LOWER(TRIM(log.affected_entity_type)) = 'lesson'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND lesson.id_lesson = CAST(log.affected_entity_identifier AS UNSIGNED)
JOIN class_group ON class_group.id_class_group = lesson.id_class_group;

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'SUBJECT', class_group.id_subject
FROM activity_log log
JOIN lesson
  ON LOWER(TRIM(log.affected_entity_type)) = 'lesson'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND lesson.id_lesson = CAST(log.affected_entity_identifier AS UNSIGNED)
JOIN class_group ON class_group.id_class_group = lesson.id_class_group;

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'ORGANIZATION', course.id_organization
FROM activity_log log
JOIN lesson
  ON LOWER(TRIM(log.affected_entity_type)) = 'lesson'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND lesson.id_lesson = CAST(log.affected_entity_identifier AS UNSIGNED)
JOIN class_group ON class_group.id_class_group = lesson.id_class_group
JOIN course ON course.id_course = class_group.id_course;

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'CLASS_GROUP', class_group.id_class_group
FROM activity_log log
JOIN content_block
  ON LOWER(TRIM(log.affected_entity_type)) = 'content_block'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND content_block.id_content_block = CAST(log.affected_entity_identifier AS UNSIGNED)
JOIN class_group ON class_group.id_class_group = content_block.id_class_group;

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'USER', attendance_record.id_user_student
FROM activity_log log
JOIN attendance_record
  ON LOWER(TRIM(log.affected_entity_type)) = 'attendance_record'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND attendance_record.id_attendance_record = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'CLASS_GROUP', class_group.id_class_group
FROM activity_log log
JOIN attendance_record
  ON LOWER(TRIM(log.affected_entity_type)) = 'attendance_record'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND attendance_record.id_attendance_record = CAST(log.affected_entity_identifier AS UNSIGNED)
JOIN lesson ON lesson.id_lesson = attendance_record.id_lesson
JOIN class_group ON class_group.id_class_group = lesson.id_class_group;

-- Assessments retain their subject and each assigned class group.  Attempts
-- additionally preserve the student affected by the operation.
INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'SUBJECT', assessment.id_subject
FROM activity_log log
JOIN assessment
  ON LOWER(TRIM(log.affected_entity_type)) = 'assessment'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND assessment.id_assessment = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'CLASS_GROUP', association.id_class_group
FROM activity_log log
JOIN assessment_class_group association
  ON LOWER(TRIM(log.affected_entity_type)) = 'assessment'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND association.id_assessment = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'USER', attempt.id_student_user
FROM activity_log log
JOIN attempt
  ON LOWER(TRIM(log.affected_entity_type)) = 'attempt'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND attempt.id_attempt = CAST(log.affected_entity_identifier AS UNSIGNED);

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'SUBJECT', assessment.id_subject
FROM activity_log log
JOIN attempt
  ON LOWER(TRIM(log.affected_entity_type)) = 'attempt'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND attempt.id_attempt = CAST(log.affected_entity_identifier AS UNSIGNED)
JOIN assessment ON assessment.id_assessment = attempt.id_assessment;

INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'USER', certificate.id_user_student
FROM activity_log log
JOIN certificate
  ON LOWER(TRIM(log.affected_entity_type)) = 'certificate'
 AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$'
 AND certificate.id_certificate = CAST(log.affected_entity_identifier AS UNSIGNED);

-- The actor's own history and direct user operations remain visible even when
-- an old user row was deleted or anonymised.
INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
SELECT log.id_activity_log, 'USER', CAST(log.affected_entity_identifier AS UNSIGNED)
FROM activity_log log
WHERE LOWER(TRIM(log.affected_entity_type)) IN ('user', 'user_account')
  AND log.affected_entity_identifier REGEXP '^[1-9][0-9]*$';

DELIMITER $$
DROP TRIGGER IF EXISTS ai_activity_log_scope_actor$$
CREATE TRIGGER ai_activity_log_scope_actor
AFTER INSERT ON activity_log
FOR EACH ROW
BEGIN
    IF NEW.id_user IS NOT NULL AND NEW.id_user > 0 THEN
        INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
        VALUES (NEW.id_activity_log, 'USER', NEW.id_user);
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_activity_log_immutable$$
CREATE TRIGGER bu_activity_log_immutable
BEFORE UPDATE ON activity_log
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Activity_Log records are immutable';
END$$

DROP TRIGGER IF EXISTS bd_activity_log_immutable$$
CREATE TRIGGER bd_activity_log_immutable
BEFORE DELETE ON activity_log
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Activity_Log records are immutable';
END$$

DROP TRIGGER IF EXISTS bu_activity_log_scope_immutable$$
CREATE TRIGGER bu_activity_log_scope_immutable
BEFORE UPDATE ON activity_log_scope
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Activity_Log scope snapshots are immutable';
END$$

DROP TRIGGER IF EXISTS bd_activity_log_scope_immutable$$
CREATE TRIGGER bd_activity_log_scope_immutable
BEFORE DELETE ON activity_log_scope
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Activity_Log scope snapshots are immutable';
END$$
DELIMITER ;
