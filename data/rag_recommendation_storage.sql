-- Existing installations with schema auto-update disabled need this nullable column.
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS recommendation_json JSON NULL;
