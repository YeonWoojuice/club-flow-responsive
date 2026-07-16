CREATE TABLE generation_dues_policies (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_id  UUID          NOT NULL,
    amount         NUMERIC(19,0) NOT NULL,
    due_date       DATE,
    created_by     UUID          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_dues_policies_generation
        FOREIGN KEY (generation_id) REFERENCES generations (id),
    CONSTRAINT fk_dues_policies_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_dues_policies_generation UNIQUE (generation_id),
    CONSTRAINT ck_dues_policies_amount CHECK (amount > 0)
);

CREATE TABLE generation_dues_refund_rules (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id        UUID        NOT NULL,
    label            VARCHAR(100) NOT NULL,
    ends_on          DATE        NOT NULL,
    refund_rate_bps  INTEGER     NOT NULL,
    sort_order       INTEGER     NOT NULL,

    CONSTRAINT fk_dues_refund_rules_policy
        FOREIGN KEY (policy_id) REFERENCES generation_dues_policies (id) ON DELETE CASCADE,
    CONSTRAINT uq_dues_refund_rules_order UNIQUE (policy_id, sort_order),
    CONSTRAINT uq_dues_refund_rules_end_date UNIQUE (policy_id, ends_on),
    CONSTRAINT ck_dues_refund_rules_rate CHECK (refund_rate_bps BETWEEN 0 AND 10000),
    CONSTRAINT ck_dues_refund_rules_order CHECK (sort_order >= 0)
);

CREATE TABLE member_dues (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id             UUID          NOT NULL,
    generation_member_id  UUID          NOT NULL,
    assessed_amount       NUMERIC(19,0) NOT NULL,
    exempted              BOOLEAN       NOT NULL DEFAULT FALSE,
    exemption_reason      VARCHAR(500),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_member_dues_policy
        FOREIGN KEY (policy_id) REFERENCES generation_dues_policies (id),
    CONSTRAINT fk_member_dues_generation_member
        FOREIGN KEY (generation_member_id) REFERENCES generation_members (id),
    CONSTRAINT uq_member_dues_generation_member UNIQUE (generation_member_id),
    CONSTRAINT ck_member_dues_amount CHECK (assessed_amount > 0),
    CONSTRAINT ck_member_dues_exemption_reason
        CHECK (NOT exempted OR exemption_reason IS NOT NULL)
);

CREATE TABLE dues_payments (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    member_due_id     UUID          NOT NULL,
    amount            NUMERIC(19,0) NOT NULL,
    paid_on           DATE,
    source            VARCHAR(30)   NOT NULL,
    idempotency_key   UUID          NOT NULL,
    recorded_by       UUID          NOT NULL,
    recorded_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    canceled_at       TIMESTAMPTZ,
    canceled_by       UUID,
    cancellation_reason VARCHAR(500),

    CONSTRAINT fk_dues_payments_member_due
        FOREIGN KEY (member_due_id) REFERENCES member_dues (id),
    CONSTRAINT fk_dues_payments_recorded_by
        FOREIGN KEY (recorded_by) REFERENCES users (id),
    CONSTRAINT fk_dues_payments_canceled_by
        FOREIGN KEY (canceled_by) REFERENCES users (id),
    CONSTRAINT uq_dues_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_dues_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_dues_payments_source CHECK (source IN ('ACTUAL', 'LEGACY_STATUS')),
    CONSTRAINT ck_dues_payments_cancellation
        CHECK ((canceled_at IS NULL AND canceled_by IS NULL AND cancellation_reason IS NULL)
            OR (canceled_at IS NOT NULL AND canceled_by IS NOT NULL AND cancellation_reason IS NOT NULL))
);

CREATE UNIQUE INDEX uq_dues_payments_active_member_due
    ON dues_payments (member_due_id)
    WHERE canceled_at IS NULL;

CREATE TABLE dues_refunds (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    member_due_id         UUID          NOT NULL,
    amount                NUMERIC(19,0) NOT NULL,
    withdrawal_date       DATE          NOT NULL,
    refund_rate_bps       INTEGER       NOT NULL,
    rule_label_snapshot   VARCHAR(100)  NOT NULL,
    idempotency_key       UUID          NOT NULL,
    processed_by          UUID          NOT NULL,
    processed_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    canceled_at           TIMESTAMPTZ,
    canceled_by           UUID,
    cancellation_reason   VARCHAR(500),

    CONSTRAINT fk_dues_refunds_member_due
        FOREIGN KEY (member_due_id) REFERENCES member_dues (id),
    CONSTRAINT fk_dues_refunds_processed_by
        FOREIGN KEY (processed_by) REFERENCES users (id),
    CONSTRAINT fk_dues_refunds_canceled_by
        FOREIGN KEY (canceled_by) REFERENCES users (id),
    CONSTRAINT uq_dues_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_dues_refunds_amount CHECK (amount >= 0),
    CONSTRAINT ck_dues_refunds_rate CHECK (refund_rate_bps BETWEEN 0 AND 10000),
    CONSTRAINT ck_dues_refunds_cancellation
        CHECK ((canceled_at IS NULL AND canceled_by IS NULL AND cancellation_reason IS NULL)
            OR (canceled_at IS NOT NULL AND canceled_by IS NOT NULL AND cancellation_reason IS NOT NULL))
);

CREATE UNIQUE INDEX uq_dues_refunds_active_member_due
    ON dues_refunds (member_due_id)
    WHERE canceled_at IS NULL;

CREATE INDEX idx_dues_refund_rules_policy ON generation_dues_refund_rules (policy_id, ends_on);
CREATE INDEX idx_member_dues_policy ON member_dues (policy_id);
CREATE INDEX idx_dues_payments_member_due ON dues_payments (member_due_id);
CREATE INDEX idx_dues_refunds_member_due ON dues_refunds (member_due_id);
