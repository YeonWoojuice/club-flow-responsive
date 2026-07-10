CREATE TABLE generation_member_status_histories (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_member_id  UUID        NOT NULL,
    previous_status       VARCHAR(20) NOT NULL,
    new_status            VARCHAR(20) NOT NULL,
    reason                VARCHAR(500),
    changed_by_user_id    UUID        NOT NULL,
    changed_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_member_status_histories_member
        FOREIGN KEY (generation_member_id) REFERENCES generation_members (id),
    CONSTRAINT fk_member_status_histories_changed_by
        FOREIGN KEY (changed_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_member_status_histories_previous_status
        CHECK (previous_status IN ('ACTIVE', 'INACTIVE', 'WITHDRAWN')),
    CONSTRAINT ck_member_status_histories_new_status
        CHECK (new_status IN ('ACTIVE', 'INACTIVE', 'WITHDRAWN'))
);

CREATE INDEX idx_member_status_histories_member_changed_at
    ON generation_member_status_histories (generation_member_id, changed_at DESC);

CREATE INDEX idx_member_status_histories_changed_by_user_id
    ON generation_member_status_histories (changed_by_user_id);
