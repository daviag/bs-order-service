ALTER TABLE orders
    ADD COLUMN created_by VARCHAR(255);
ALTER TABLE orders
    ADD COLUMN last_modified_by VARCHAR(255);