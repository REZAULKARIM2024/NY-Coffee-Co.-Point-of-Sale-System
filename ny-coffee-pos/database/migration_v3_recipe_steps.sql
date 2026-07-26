-- Run this if you already created the database from an earlier version of schema.sql.
-- Adds multi-language recipe/prep-step instructions without touching existing data.
USE pos_system;

CREATE TABLE IF NOT EXISTS recipe_steps (
    id INT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id INT NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    step_number INT NOT NULL,
    instruction TEXT NOT NULL,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    UNIQUE KEY uq_recipe_step (menu_item_id, language, step_number)
);
