-- Run this if you already created the database from an earlier version of schema.sql.
-- Adds size-based and modifier-based pricing without touching existing data.
USE pos_system;

CREATE TABLE IF NOT EXISTS sizes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    price_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS modifiers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    modifier_group VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0
);

INSERT INTO sizes (name, price_delta, sort_order) VALUES
 ('Small', 0.00, 1), ('Medium', 0.50, 2), ('Large', 1.00, 3), ('Extra Large', 1.50, 4)
ON DUPLICATE KEY UPDATE name = name;

INSERT INTO modifiers (name, price_delta, modifier_group, sort_order) VALUES
 ('Regular - Cream & Sugar', 0.00, 'Dairy/Sweetener', 1),
 ('Black - No Cream or Sugar', 0.00, 'Dairy/Sweetener', 2),
 ('Cream Only', 0.00, 'Dairy/Sweetener', 3),
 ('Sugar Only', 0.00, 'Dairy/Sweetener', 4),
 ('Oat Milk', 0.60, 'Dairy/Sweetener', 5),
 ('Almond Milk', 0.60, 'Dairy/Sweetener', 6),
 ('Caramel Swirl', 0.50, 'Flavor Swirl', 7),
 ('Vanilla Swirl', 0.50, 'Flavor Swirl', 8),
 ('Mocha Swirl', 0.50, 'Flavor Swirl', 9),
 ('Extra Espresso Shot', 0.75, 'Add-On', 10),
 ('Whipped Cream', 0.50, 'Add-On', 11);
