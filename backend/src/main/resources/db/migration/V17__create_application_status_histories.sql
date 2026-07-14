CREATE TABLE application_status_histories (
    id                 UUID         PRIMARY KEY,
    application_id     UUID         NOT NULL,
    previous_status    VARCHAR(20)  NOT NULL,
    new_status         VARCHAR(20)  NOT NULL,
    reason             VARCHAR(500),
    changed_by_user_id UUID         NOT NULL,
    changed_at         TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_application_status_histories_application
        FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_application_status_histories_changed_by
        FOREIGN KEY (changed_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_application_status_histories_previous_status
        CHECK (previous_status IN ('SUBMITTED', 'REVIEWING', 'ACCEPTED', 'REJECTED', 'CANCELED')),
    CONSTRAINT ck_application_status_histories_new_status
        CHECK (new_status IN ('SUBMITTED', 'REVIEWING', 'ACCEPTED', 'REJECTED', 'CANCELED'))
);

CREATE INDEX idx_application_status_histories_application_changed
    ON application_status_histories (application_id, changed_at DESC);
