-- Builds out the "Local" department: local/national specialty items, a catering
-- menu, and a charity/bag-deposit/pet-treats section, mirroring a typical coffee
-- shop's store-customizable Local tab. All item names are original to this shop.
USE pos_system;

SET @dept_local = (SELECT id FROM categories WHERE name = 'Local' AND parent_id IS NULL);

INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Local Items', 'Local', 1, @dept_local);
SET @sub_local_items = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Catering Menu', 'Local', 2, @dept_local);
SET @sub_catering = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Charity, Deposits & Pup Cups', 'Local', 3, @dept_local);
SET @sub_charity = LAST_INSERT_ID();

-- ---- Local Items ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_local_items, 'Brooklyn Bridge Blend Coffee', 'Local coffee blend named for the Brooklyn Bridge', 3.29, 1.10, 'Local Items'),
 (@sub_local_items, 'Empire Bagel Sandwich', 'Local breakfast sandwich on a fresh bagel', 5.49, 2.00, 'Local Items'),
 (@sub_local_items, 'Hudson Valley Apple Cider Donut', 'Local seasonal apple cider donut', 2.29, 0.55, 'Local Items'),
 (@sub_local_items, 'Central Park Chai Latte', 'Local chai latte specialty', 4.29, 1.60, 'Local Items'),
 (@sub_local_items, 'Queens Borough Breakfast Wrap', 'Local breakfast wrap specialty', 5.29, 1.90, 'Local Items'),
 (@sub_local_items, 'Staten Island Ferry Frappe', 'Local frozen frappe specialty', 4.79, 1.80, 'Local Items'),
 (@sub_local_items, 'Manhattan Skyline Mocha', 'Local mocha specialty', 4.49, 1.70, 'Local Items'),
 (@sub_local_items, 'Bronx Bodega Breakfast Sandwich', 'Local breakfast sandwich specialty', 5.49, 2.00, 'Local Items'),
 (@sub_local_items, 'Coney Island Frozen Frappe', 'Local frozen frappe specialty', 4.99, 1.90, 'Local Items'),
 (@sub_local_items, 'Times Square Turbo Shot', 'Local extra-caffeine espresso shot add-on', 1.49, 0.40, 'Local Items'),
 (@sub_local_items, 'Greenwich Village Green Tea Latte', 'Local green tea latte specialty', 4.29, 1.60, 'Local Items'),
 (@sub_local_items, 'Harlem Heat Spicy Chai', 'Local spicy chai specialty', 4.49, 1.70, 'Local Items');

-- ---- Catering Menu ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_catering, 'Dozen Donuts Tray', 'Catering tray of a dozen assorted donuts', 14.99, 6.00, 'Catering Menu'),
 (@sub_catering, 'Half Dozen Donuts Tray', 'Catering tray of six assorted donuts', 8.49, 3.30, 'Catering Menu'),
 (@sub_catering, 'Donut Holes Tray (50ct)', 'Catering tray of fifty assorted donut holes', 12.99, 5.20, 'Catering Menu'),
 (@sub_catering, 'Assorted Bagel Tray (12ct)', 'Catering tray of a dozen assorted bagels', 16.99, 7.00, 'Catering Menu'),
 (@sub_catering, 'Breakfast Sandwich Platter (10ct)', 'Catering platter of ten breakfast sandwiches', 39.99, 18.00, 'Catering Menu'),
 (@sub_catering, 'Coffee Traveler (96oz)', 'Catering box of coffee, serves about 10-12', 21.99, 9.00, 'Catering Menu'),
 (@sub_catering, 'Hot Chocolate Traveler (96oz)', 'Catering box of hot chocolate, serves about 10-12', 22.99, 9.50, 'Catering Menu'),
 (@sub_catering, 'Iced Coffee Jug (1 Gallon)', 'Catering jug of iced coffee, serves about 10-12', 24.99, 10.50, 'Catering Menu'),
 (@sub_catering, 'Muffin and Pastry Tray', 'Catering tray of assorted muffins and pastries', 26.99, 11.50, 'Catering Menu'),
 (@sub_catering, 'Fruit and Yogurt Parfait Tray', 'Catering tray of fruit and yogurt parfaits', 29.99, 13.00, 'Catering Menu'),
 (@sub_catering, 'Continental Breakfast Box (10 Person)', 'Catering breakfast box for a group of ten', 59.99, 27.00, 'Catering Menu'),
 (@sub_catering, 'Boxed Lunch Assortment (10 Person)', 'Catering boxed lunch assortment for a group of ten', 79.99, 36.00, 'Catering Menu');

-- ---- Charity, Deposits & Pup Cups ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_charity, 'Round-Up Charity Donation $1', 'Round up your order to donate $1 to a local charity', 1.00, 1.00, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Charity Donation $2', 'Add a $2 donation to a local charity', 2.00, 2.00, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Charity Donation $5', 'Add a $5 donation to a local charity', 5.00, 5.00, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Local Foundation Donation $10', 'Add a $10 donation to a local community foundation', 10.00, 10.00, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Reusable Bag Deposit $.05', 'Five cent deposit for a reusable bag', 0.05, 0.05, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Reusable Bag Deposit $.10', 'Ten cent deposit for a reusable bag', 0.10, 0.10, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Pup Cup - Whipped Cream', 'A small cup of whipped cream for dogs', 0.99, 0.30, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Pup Cup - Frozen Treat', 'A small frozen treat cup for dogs', 1.49, 0.50, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Dog Treat - Peanut Butter Biscuit', 'Peanut butter biscuit treat for dogs', 1.99, 0.70, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Dog Bandana - Shop Logo', 'Bandana for dogs with shop logo', 4.99, 1.80, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Dog Toy - Donut Shaped', 'Plush donut-shaped toy for dogs', 6.99, 2.70, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Dog Toy - Coffee Cup Shaped', 'Plush coffee-cup-shaped toy for dogs', 6.99, 2.70, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Dog Toy - Pretzel Shaped', 'Plush pretzel-shaped toy for dogs', 6.99, 2.70, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Seasonal Iced Coffee Dog Toy', 'Seasonal plush iced coffee toy for dogs', 7.99, 3.10, 'Charity, Deposits & Pup Cups'),
 (@sub_charity, 'Seasonal Combo Dog Toy', 'Seasonal plush combo toy for dogs', 7.99, 3.10, 'Charity, Deposits & Pup Cups');
