-- A class group can be delivered through equivalent occurrences in every active
-- course that integrates its subject. Existing groups retain their original
-- single context as historical compatibility data.
CREATE TABLE IF NOT EXISTS class_group_course_occurrence (
    id_class_group BIGINT UNSIGNED NOT NULL,
    id_course BIGINT UNSIGNED NOT NULL,
    id_course_occurrence BIGINT UNSIGNED NOT NULL,
    id_course_occurrence_period BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_class_group, id_course),
    UNIQUE KEY uq_class_group_course_occurrence (id_class_group, id_course_occurrence),
    KEY idx_class_group_course_occurrence_course (id_course),
    KEY idx_class_group_course_occurrence_occurrence (id_course_occurrence),
    CONSTRAINT fk_cg_shared_occurrence_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cg_shared_occurrence_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cg_shared_occurrence_occurrence
        FOREIGN KEY (id_course_occurrence) REFERENCES course_occurrence (id_course_occurrence)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cg_shared_occurrence_period
        FOREIGN KEY (id_course_occurrence_period) REFERENCES course_occurrence_period (id_course_occurrence_period)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

INSERT INTO class_group_course_occurrence (
    id_class_group, id_course, id_course_occurrence, id_course_occurrence_period
)
SELECT id_class_group, id_course, id_course_occurrence, id_course_occurrence_period
FROM class_group
ON DUPLICATE KEY UPDATE id_course_occurrence_period = VALUES(id_course_occurrence_period);
