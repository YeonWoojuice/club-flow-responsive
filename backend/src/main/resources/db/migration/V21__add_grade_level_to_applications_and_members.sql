ALTER TABLE applications
    ADD COLUMN grade_level SMALLINT;

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_grade_level
        CHECK (grade_level IS NULL OR grade_level BETWEEN 1 AND 20);

ALTER TABLE generation_members
    ADD COLUMN grade_level SMALLINT;

ALTER TABLE generation_members
    ADD CONSTRAINT chk_generation_members_grade_level
        CHECK (grade_level IS NULL OR grade_level BETWEEN 1 AND 20);

ALTER TABLE application_import_sources
    ADD COLUMN grade_level_header VARCHAR(255);
