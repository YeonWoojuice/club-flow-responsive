CREATE TABLE club_staff_invitations (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    club_id            UUID         NOT NULL,
    email              VARCHAR(255) NOT NULL,
    role               VARCHAR(30)  NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    invited_by_user_id UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    responded_at       TIMESTAMPTZ,

    CONSTRAINT fk_club_staff_invitations_club
        FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_staff_invitations_invited_by
        FOREIGN KEY (invited_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_club_staff_invitations_role
        CHECK (role IN ('VICE_PRESIDENT', 'STAFF')),
    CONSTRAINT ck_club_staff_invitations_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED')),
    CONSTRAINT ck_club_staff_invitations_email_lowercase
        CHECK (email = LOWER(email))
);

CREATE INDEX idx_club_staff_invitations_club_id
    ON club_staff_invitations (club_id);
CREATE INDEX idx_club_staff_invitations_email_status
    ON club_staff_invitations (email, status);
CREATE INDEX idx_club_staff_invitations_invited_by_user_id
    ON club_staff_invitations (invited_by_user_id);
CREATE UNIQUE INDEX uq_club_staff_invitations_pending_club_email
    ON club_staff_invitations (club_id, email)
    WHERE status = 'PENDING';
