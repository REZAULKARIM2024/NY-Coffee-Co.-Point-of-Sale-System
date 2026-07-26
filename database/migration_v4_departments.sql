-- Run this if you already created the database from an earlier version of schema.sql.
-- Restructures the menu into a 2-level Department -> Subcategory hierarchy (matching the
-- POS screen: top department tabs + a right-side subcategory sidebar), and adds a `section`
-- label to menu_items so items group under headers inside each subcategory's item grid
-- (e.g. "Single Espresso" vs "Double Espresso" vs "Alternative Beverages").
--
-- All new item names here are original to this shop (not any other brand's trademarked
-- product names), by design.
USE pos_system;

ALTER TABLE categories ADD COLUMN parent_id INT NULL;
ALTER TABLE categories ADD CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id);
ALTER TABLE menu_items ADD COLUMN section VARCHAR(50) NULL;

-- ============ TOP-LEVEL DEPARTMENTS ============
INSERT INTO categories (name, station, sort_order) VALUES ('Beverages', 'Beverages', 1);
SET @dept_beverages = LAST_INSERT_ID();

INSERT INTO categories (name, station, sort_order) VALUES ('Bakery', 'Bakery', 2);
SET @dept_bakery = LAST_INSERT_ID();

INSERT INTO categories (name, station, sort_order) VALUES ('Sandwiches', 'Sandwich Station', 3);
SET @dept_sandwiches = LAST_INSERT_ID();

INSERT INTO categories (name, station, sort_order) VALUES ('Retail', 'Retail', 4);
SET @dept_retail = LAST_INSERT_ID();

INSERT INTO categories (name, station, sort_order) VALUES ('Local', 'Local', 5);
SET @dept_local = LAST_INSERT_ID();

-- Reparent whatever already existed so nothing already on the menu disappears.
UPDATE categories SET parent_id = @dept_bakery WHERE name IN ('Bagels & Muffins', 'Donuts');
UPDATE categories SET parent_id = @dept_sandwiches WHERE name IN ('Sandwiches & More', 'Snacks & Wraps');
UPDATE categories SET parent_id = @dept_retail WHERE name = 'Brew At Home';

-- ============ BEVERAGES SUBCATEGORIES ============
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Coffee', 'Coffee Bar', 1, @dept_beverages);
SET @sub_coffee = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Espresso', 'Coffee Bar', 2, @dept_beverages);
SET @sub_espresso = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Teas', 'Tea Station', 3, @dept_beverages);
SET @sub_teas = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Refreshers', 'Refresher Bar', 4, @dept_beverages);
SET @sub_refreshers = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Frozen', 'Frozen Station', 5, @dept_beverages);
SET @sub_frozen = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Other Beverages', 'Beverages', 6, @dept_beverages);
SET @sub_other_bev = LAST_INSERT_ID();

-- Move existing beverage items into the new subcategories/sections instead of duplicating them.
UPDATE menu_items SET category_id = @sub_espresso, section = 'Single Espresso' WHERE name IN ('Espresso', 'Latte');
UPDATE menu_items SET category_id = @sub_espresso, section = 'Alternative Beverages' WHERE name = 'Chai Latte';
UPDATE menu_items SET category_id = @sub_coffee, section = 'Cold Brew' WHERE name = 'Cold Brew';
UPDATE menu_items SET category_id = @sub_teas, section = 'Iced Tea' WHERE name = 'Iced Tea';
UPDATE menu_items SET category_id = @sub_frozen, section = 'Frozen Coffee' WHERE name = 'Frozen Coffee';
UPDATE menu_items SET category_id = @sub_frozen, section = 'Frozen Coolers' WHERE name = 'Frozen Lemonade';

-- Now the old flat categories are empty — drop them so they don't show up as duplicate tabs.
DELETE FROM categories WHERE name IN ('Espresso & Coffee', 'Teas & More', 'Frozen Drinks') AND parent_id IS NULL;

-- ---- Coffee ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_coffee, 'Original Coffee', 'Classic drip coffee', 2.75, 0.60, 'Blend'),
 (@sub_coffee, 'Decaf Coffee', 'Decaffeinated drip coffee', 2.75, 0.60, 'Blend'),
 (@sub_coffee, 'Half-Caff Coffee', 'Half regular, half decaf', 2.75, 0.60, 'Blend'),
 (@sub_coffee, 'Midnight Dark Roast', 'Extra bold dark roast blend', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Barista Mix Blend', 'House signature mixed roast', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Nitro Cold Brew', 'Cold brew infused with nitrogen for a creamy pour', 4.25, 1.00, 'Cold Brew'),
 (@sub_coffee, 'Cold Foam Cold Brew', 'Cold brew topped with sweet cold foam', 4.50, 1.10, 'Cold Brew');

-- ---- Espresso ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_espresso, 'Cappuccino', 'Espresso with steamed milk and foam', 4.00, 1.20, 'Single Espresso'),
 (@sub_espresso, 'Cafe Au Lait', 'Coffee with steamed milk', 3.75, 1.00, 'Single Espresso'),
 (@sub_espresso, 'Americano', 'Espresso with hot water', 3.50, 0.70, 'Double Espresso'),
 (@sub_espresso, 'Macchiato', 'Espresso marked with a dash of foam', 4.25, 1.20, 'Double Espresso'),
 (@sub_espresso, 'Brown Sugar Shaken Espresso', 'Espresso shaken with brown sugar and oat milk', 4.50, 1.30, 'Double Espresso'),
 (@sub_espresso, 'Vanilla Shaken Espresso', 'Espresso shaken with vanilla and milk', 4.50, 1.30, 'Double Espresso'),
 (@sub_espresso, 'Matcha Latte', 'Matcha green tea with steamed milk', 4.75, 1.50, 'Alternative Beverages'),
 (@sub_espresso, 'Cafe Con Leche', 'Espresso with warm sweetened milk', 4.00, 1.10, 'Alternative Beverages'),
 (@sub_espresso, 'Cortado', 'Espresso cut with a small amount of steamed milk', 3.75, 1.00, 'Alternative Beverages');

-- ---- Teas ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_teas, 'Iced Tea with Lemonade', 'Half iced tea, half lemonade', 3.00, 0.60, 'Iced Tea'),
 (@sub_teas, 'Sweetened Iced Tea', 'Freshly brewed iced tea, sweetened', 2.75, 0.50, 'Iced Tea'),
 (@sub_teas, 'Sweetened Green Tea', 'Iced green tea, sweetened', 2.95, 0.55, 'Iced Tea'),
 (@sub_teas, 'Bold Breakfast Tea', 'Classic black breakfast tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Harmony Green Tea', 'Smooth green tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Cool Mint Tea', 'Refreshing mint herbal tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Chamomile Fields Tea', 'Calming chamomile herbal tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Hibiscus Kiss Tea', 'Tart hibiscus herbal tea', 2.95, 0.55, 'Hot Tea'),
 (@sub_teas, 'Decaf Tea', 'Decaffeinated black tea', 2.75, 0.50, 'Hot Tea');

-- ---- Refreshers ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_refreshers, 'Strawberry Dragonfruit Refresher', 'Fruit-flavored refresher with real fruit pieces', 3.75, 0.90, 'Classic'),
 (@sub_refreshers, 'Mango Pineapple Refresher', 'Tropical fruit-flavored refresher', 3.75, 0.90, 'Classic'),
 (@sub_refreshers, 'Black Cherry Refresher', 'Black cherry-flavored refresher', 3.75, 0.90, 'Classic'),
 (@sub_refreshers, 'Pink Pineapple Refresher', 'Pineapple-flavored refresher', 3.75, 0.90, 'Classic'),
 (@sub_refreshers, 'Strawberry Daydream Refresher', 'Creamy strawberry refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Mango Daydream Refresher', 'Creamy mango refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Cherry Daydream Refresher', 'Creamy cherry refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Pink Pineapple Daydream Refresher', 'Creamy pineapple refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Build Your Own Refresher', 'Choose your own flavor combination', 3.75, 0.90, 'Mix It');

-- ---- Frozen ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_frozen, 'Blue Raspberry Cooler', 'Blended blue raspberry frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Cosmic Cooler', 'Blended mixed-berry frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Strawberry Cooler', 'Blended strawberry frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Vanilla Bean Cooler', 'Blended vanilla bean frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Caramel Cream Frozen Coffee', 'Blended frozen coffee with caramel', 4.75, 1.30, 'Frozen Coffee'),
 (@sub_frozen, 'Triple Mocha Frozen Coffee', 'Blended frozen coffee with extra mocha', 4.95, 1.35, 'Frozen Coffee'),
 (@sub_frozen, 'Frozen Chai Latte', 'Blended frozen chai latte', 4.75, 1.30, 'Other Frozen'),
 (@sub_frozen, 'Frozen Matcha Latte', 'Blended frozen matcha latte', 4.95, 1.40, 'Other Frozen');

-- ---- Other Beverages ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_other_bev, 'Fountain Soda Small', 'Small fountain soda', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Fountain Soda Medium', 'Medium fountain soda', 2.75, 0.35, 'Coolers & Fountain'),
 (@sub_other_bev, 'Fountain Soda Large', 'Large fountain soda', 3.25, 0.40, 'Coolers & Fountain'),
 (@sub_other_bev, 'Blackberry Tangerine Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Blush Pop Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Tropical Mango Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Box of Coffee (12 cups)', 'Coffee box for meetings and events', 22.99, 9.00, 'Bulk'),
 (@sub_other_bev, 'Gallon of Iced Tea', 'Bulk iced tea for events', 14.99, 5.00, 'Bulk');
