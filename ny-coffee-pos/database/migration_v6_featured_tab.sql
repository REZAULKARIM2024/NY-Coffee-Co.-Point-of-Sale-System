-- Turns "Featured" from a coming-soon placeholder tab into a real department with
-- colorful sub-tabs (New Arrivals, Fan Favorites, Limited Time Offers, Value Bundles),
-- matching the layout style of a typical counter-service Featured board. A 5th
-- "News & Promos" sidebar entry is added client-side in POSPanel.java (not DB-backed).
-- All item names are original to this shop (not copied from any other brand's menu).
USE pos_system;

INSERT INTO categories (name, station, sort_order) VALUES ('Featured', 'Beverages', 0);
SET @dept_featured = LAST_INSERT_ID();

INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('New Arrivals', 'Beverages', 1, @dept_featured);
SET @sub_new = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Fan Favorites', 'Beverages', 2, @dept_featured);
SET @sub_fan = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Limited Time Offers', 'Beverages', 3, @dept_featured);
SET @sub_lto = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Value Bundles', 'Beverages', 4, @dept_featured);
SET @sub_bundles = LAST_INSERT_ID();

-- ---- New Arrivals ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_new, 'Sunrise Berry Refresher', 'Fruit-flavored refresher with mixed berries', 3.95, 0.95, 'Refreshers'),
 (@sub_new, 'Tropical Citrus Refresher', 'Fruit-flavored refresher with tropical citrus', 3.95, 0.95, 'Refreshers'),
 (@sub_new, 'Double Strawberry Cloud', 'Creamy double strawberry refresher', 4.35, 1.15, 'Refreshers'),
 (@sub_new, 'Cookie Crumble Cloud Latte', 'Latte with cookie crumble and cream', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_new, 'Toffee Almond Shakin'' Espresso', 'Shaken espresso with toffee almond', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_new, 'Perfect Matcha', 'Whisked matcha with steamed milk', 4.75, 1.50, 'Matcha'),
 (@sub_new, 'Coconut Matcha Limeade', 'Matcha limeade with coconut', 4.65, 1.35, 'Matcha'),
 (@sub_new, 'Golden Hour Lemonade', 'Fresh-squeezed golden lemonade', 3.45, 0.75, 'Lemonade'),
 (@sub_new, 'Sparkling Citrus Lemonade', 'Lightly sparkling citrus lemonade', 3.65, 0.85, 'Lemonade'),
 (@sub_new, 'Midnight Berry Zero', 'Zero-sugar sparkling water with berry', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Twilight Citrus Zero', 'Zero-sugar sparkling water with citrus', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Mocha Swirl Chiller', 'Blended frozen mocha with a chocolate swirl', 4.95, 1.40, 'Chillers');

-- ---- Fan Favorites ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_fan, 'Black Cherry Refresher', 'Black cherry-flavored refresher', 3.85, 0.95, 'Refreshers'),
 (@sub_fan, 'Mango Sunset Refresher', 'Mango-flavored refresher', 3.85, 0.95, 'Refreshers'),
 (@sub_fan, 'Cherry Cloud Daydream', 'Creamy cherry refresher', 4.35, 1.15, 'Refreshers'),
 (@sub_fan, 'Peanut Butter Cloud Latte', 'Latte with peanut butter and cream', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_fan, 'Vanilla Bean Cloud Latte', 'Latte with vanilla bean and cream', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_fan, 'Brown Sugar Almond Shakin'' Espresso', 'Shaken espresso with brown sugar almond', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_fan, 'Strawberry Cloud Matcha', 'Matcha with strawberry cream', 4.85, 1.50, 'Matcha'),
 (@sub_fan, 'Vanilla Cloud Matcha', 'Matcha with vanilla cream', 4.85, 1.50, 'Matcha'),
 (@sub_fan, 'Strawberry Sparkle Lemonade', 'Lemonade with strawberry sparkle', 3.65, 0.85, 'Lemonade'),
 (@sub_fan, 'Coconut Cloud Limeade', 'Limeade with coconut cream', 3.85, 0.90, 'Lemonade'),
 (@sub_fan, 'Splash Berry Zero', 'Zero-sugar sparkling water with berry splash', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Caramel Coffee Chiller', 'Blended frozen coffee with caramel', 4.95, 1.40, 'Chillers');

-- ---- Limited Time Offers ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_lto, 'Cherry Berry Hibiscus Spritz', 'Sparkling cherry berry hibiscus refresher', 4.25, 1.05, 'Spritz'),
 (@sub_lto, 'Peach Ginger Honey Spritz', 'Sparkling peach ginger honey refresher', 4.25, 1.05, 'Spritz'),
 (@sub_lto, 'Blackberry Pomegranate Rosemary Spritz', 'Sparkling blackberry pomegranate rosemary refresher', 4.35, 1.10, 'Spritz'),
 (@sub_lto, 'Pumpkin Cream Cloud Latte', 'Seasonal pumpkin cream latte', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Maple Pecan Cloud Latte', 'Seasonal maple pecan latte', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Rocky Road Cloud Latte', 'Latte with rocky road flavor and cream', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Watermelon Lime Zero', 'Zero-sugar sparkling water with watermelon lime', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Honeydew Paradise Zero', 'Zero-sugar sparkling water with honeydew', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Triple Mocha Coffee Chiller', 'Blended frozen coffee with extra mocha', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Cookies & Cream Coffee Chiller', 'Blended frozen coffee with cookies and cream', 4.99, 1.45, 'Chillers');

-- ---- Value Bundles ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_bundles, 'Iced Coffee Bundle (5-pack)', 'Five iced coffees to go', 14.99, 4.50, 'Bundles'),
 (@sub_bundles, 'Decaf Iced Coffee Bundle (5-pack)', 'Five decaf iced coffees to go', 14.99, 4.50, 'Bundles'),
 (@sub_bundles, 'Refresher Bundle (4-pack)', 'Four refreshers to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Strawberry Dragonfruit Lemonade Bundle (4-pack)', 'Four strawberry dragonfruit lemonades to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Matcha Bundle (3-pack)', 'Three matcha drinks to go', 13.99, 5.00, 'Bundles'),
 (@sub_bundles, 'Frozen Coffee Bundle (4-pack)', 'Four frozen coffees to go', 18.99, 6.00, 'Bundles');
