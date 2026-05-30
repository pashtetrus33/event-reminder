CREATE TABLE notifications (
    id          VARCHAR(26)                  NOT NULL,
    notification_date DATE                   NOT NULL,
    status      VARCHAR(20)                  NOT NULL,
    message_preview VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE     NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE     NOT NULL,
    version     INTEGER                      NOT NULL DEFAULT 0,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uq_notifications_date UNIQUE (notification_date)
);
