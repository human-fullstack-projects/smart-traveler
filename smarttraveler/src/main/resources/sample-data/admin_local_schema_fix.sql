-- Local smart-travel-db only. Hibernate update had converted these two existing
-- lodging TEXT fields to VARCHAR(255) before the entity length was corrected.
-- Reapply the original ERD types without dropping or truncating any rows.
ALTER TABLE lodging
    MODIFY COLUMN description TEXT NULL,
    MODIFY COLUMN refund_policy TEXT NULL;
