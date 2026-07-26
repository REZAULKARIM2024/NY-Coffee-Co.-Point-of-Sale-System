-- Builds out the "Retail" department to match a typical coffee-shop retail wall:
-- renames/adds five colorful sub-tabs (Coffee & Tea, Mugs & Drinkware, Gift Sets &
-- Holiday, Grab & Go Treats, Gift Cards & More) and populates each with 50+ items.
-- All item names are original to this shop.
USE pos_system;

SET @dept_retail = (SELECT id FROM categories WHERE name = 'Retail' AND parent_id IS NULL);

-- Rename + re-order the existing subcategory to fit the new scheme
UPDATE categories SET name = 'Coffee & Tea', sort_order = 1
  WHERE name = 'Brew At Home' AND parent_id = @dept_retail;

SET @sub_brew_home = (SELECT id FROM categories WHERE name = 'Coffee & Tea' AND parent_id = @dept_retail);

INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Mugs & Drinkware', 'Retail', 2, @dept_retail);
SET @sub_mugs = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Gift Sets & Holiday', 'Retail', 3, @dept_retail);
SET @sub_giftholiday = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Grab & Go Treats', 'Retail', 4, @dept_retail);
SET @sub_grabgo = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Gift Cards & More', 'Retail', 5, @dept_retail);
SET @sub_giftcards = LAST_INSERT_ID();

-- Re-tag the two existing items with sections so they group cleanly (kept as distinct SKUs from the new items below)
UPDATE menu_items SET section = 'Bagged Coffee' WHERE name = 'Ground Coffee Bag' AND category_id = @sub_brew_home;
UPDATE menu_items SET section = 'K-Cups' WHERE name = 'K-Cup Pack (10ct)' AND category_id = @sub_brew_home;

-- ==== Coffee & Tea ====
-- ---- Bagged Coffee ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_brew_home, 'Original Blend Coffee 12oz Bag', 'Ground original blend coffee, bagged for home brewing', 9.99, 4.50, 'Bagged Coffee'),
 (@sub_brew_home, 'Dark Roast Coffee 12oz Bag', 'Ground dark roast coffee, bagged for home brewing', 9.99, 4.50, 'Bagged Coffee'),
 (@sub_brew_home, 'French Vanilla Coffee 12oz Bag', 'Ground french vanilla coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Hazelnut Coffee 12oz Bag', 'Ground hazelnut coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Decaf Blend Coffee 12oz Bag', 'Ground decaf blend coffee, bagged for home brewing', 9.99, 4.50, 'Bagged Coffee'),
 (@sub_brew_home, 'Pumpkin Spice Coffee 12oz Bag', 'Ground pumpkin spice coffee, bagged for home brewing', 10.99, 4.90, 'Bagged Coffee'),
 (@sub_brew_home, 'Midnight Dark Roast Coffee 12oz Bag', 'Ground midnight dark roast coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Holiday Blend Coffee 16oz Bag', 'Ground holiday blend coffee, bagged for home brewing', 11.99, 5.30, 'Bagged Coffee'),
 (@sub_brew_home, 'Colombian Coffee 12oz Bag', 'Ground colombian coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Sumatra Dark Coffee 12oz Bag', 'Ground sumatra dark coffee, bagged for home brewing', 10.99, 4.90, 'Bagged Coffee'),
 (@sub_brew_home, 'Espresso Roast Coffee 12oz Bag', 'Ground espresso roast coffee, bagged for home brewing', 10.99, 4.90, 'Bagged Coffee'),
 (@sub_brew_home, 'Breakfast Blend Coffee 12oz Bag', 'Ground breakfast blend coffee, bagged for home brewing', 9.99, 4.50, 'Bagged Coffee'),
 (@sub_brew_home, 'Caramel Coffee 12oz Bag', 'Ground caramel coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Mocha Coffee 12oz Bag', 'Ground mocha coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Cinnamon Spice Coffee 12oz Bag', 'Ground cinnamon spice coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Vanilla Bean Coffee 12oz Bag', 'Ground vanilla bean coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Toffee Coffee 12oz Bag', 'Ground toffee coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Coconut Coffee 12oz Bag', 'Ground coconut coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Maple Coffee 12oz Bag', 'Ground maple coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee'),
 (@sub_brew_home, 'Irish Cream Coffee 12oz Bag', 'Ground irish cream coffee, bagged for home brewing', 10.49, 4.70, 'Bagged Coffee');

-- ---- Teas ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_brew_home, 'Bold Breakfast Black Tea Box', 'Boxed bold breakfast black tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Harmony Green Tea Box', 'Boxed harmony green tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Cool Mint Herbal Tea Box', 'Boxed cool mint herbal tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Chamomile Fields Herbal Tea Box', 'Boxed chamomile fields herbal tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Hibiscus Kiss Herbal Tea Box', 'Boxed hibiscus kiss herbal tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Decaf Breakfast Black Tea Box', 'Boxed decaf breakfast black tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Citrus Grove Black Tea Box', 'Boxed citrus grove black tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Ginger Spice Herbal Tea Box', 'Boxed ginger spice herbal tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Lavender Dreams Herbal Tea Box', 'Boxed lavender dreams herbal tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Jasmine Green Tea Box', 'Boxed jasmine green tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Rooibos Sunset Herbal Tea Box', 'Boxed rooibos sunset herbal tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Lemon Zest Green Tea Box', 'Boxed lemon zest green tea, 20 bags', 6.49, 2.60, 'Teas'),
 (@sub_brew_home, 'Vanilla Chai Black Tea Box', 'Boxed vanilla chai black tea, 20 bags', 6.99, 2.80, 'Teas'),
 (@sub_brew_home, 'Wild Berry Herbal Tea Box', 'Boxed wild berry herbal tea, 20 bags', 6.49, 2.60, 'Teas');

-- ---- K-Cups ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_brew_home, 'Original Blend K-Cups (12ct)', 'Single-serve original blend coffee pods', 8.99, 4.00, 'K-Cups'),
 (@sub_brew_home, 'Dark Roast K-Cups (12ct)', 'Single-serve dark roast coffee pods', 8.99, 4.00, 'K-Cups'),
 (@sub_brew_home, 'French Vanilla K-Cups (12ct)', 'Single-serve french vanilla coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Hazelnut K-Cups (12ct)', 'Single-serve hazelnut coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Pumpkin K-Cups (12ct)', 'Single-serve pumpkin coffee pods', 9.99, 4.40, 'K-Cups'),
 (@sub_brew_home, 'Hot Cocoa K-Cups (12ct)', 'Single-serve hot cocoa coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Gingerbread K-Cups (12ct)', 'Single-serve gingerbread coffee pods', 9.99, 4.40, 'K-Cups'),
 (@sub_brew_home, 'Holiday Blend K-Cups (12ct)', 'Single-serve holiday blend coffee pods', 9.99, 4.40, 'K-Cups'),
 (@sub_brew_home, 'Colombian K-Cups (12ct)', 'Single-serve colombian coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Sumatra Dark K-Cups (12ct)', 'Single-serve sumatra dark coffee pods', 9.99, 4.40, 'K-Cups'),
 (@sub_brew_home, 'Espresso Roast K-Cups (12ct)', 'Single-serve espresso roast coffee pods', 9.99, 4.40, 'K-Cups'),
 (@sub_brew_home, 'Breakfast Blend K-Cups (12ct)', 'Single-serve breakfast blend coffee pods', 8.99, 4.00, 'K-Cups'),
 (@sub_brew_home, 'Caramel K-Cups (12ct)', 'Single-serve caramel coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Mocha K-Cups (12ct)', 'Single-serve mocha coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Cinnamon Spice K-Cups (12ct)', 'Single-serve cinnamon spice coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Vanilla Bean K-Cups (12ct)', 'Single-serve vanilla bean coffee pods', 9.49, 4.20, 'K-Cups'),
 (@sub_brew_home, 'Decaf K-Cups (12ct)', 'Single-serve decaf coffee pods', 8.99, 4.00, 'K-Cups'),
 (@sub_brew_home, 'Toffee K-Cups (12ct)', 'Single-serve toffee coffee pods', 9.49, 4.20, 'K-Cups');

-- ---- Brewers ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_brew_home, 'Single-Serve Pod Brewer - Classic', 'Single-Serve Pod Brewer - Classic for home brewing', 89.99, 45.00, 'Brewers'),
 (@sub_brew_home, 'Single-Serve Pod Brewer - Compact', 'Single-Serve Pod Brewer - Compact for home brewing', 79.99, 40.00, 'Brewers'),
 (@sub_brew_home, 'Single-Serve Pod Brewer - Deluxe', 'Single-Serve Pod Brewer - Deluxe for home brewing', 129.99, 65.00, 'Brewers'),
 (@sub_brew_home, 'Single-Serve Pod Brewer - Express', 'Single-Serve Pod Brewer - Express for home brewing', 99.99, 50.00, 'Brewers'),
 (@sub_brew_home, 'Single-Serve Pod Brewer - Mini', 'Single-Serve Pod Brewer - Mini for home brewing', 59.99, 30.00, 'Brewers'),
 (@sub_brew_home, 'Single-Serve Pod Brewer - Family Size', 'Single-Serve Pod Brewer - Family Size for home brewing', 149.99, 75.00, 'Brewers'),
 (@sub_brew_home, '12-Cup Drip Brewer', '12-Cup Drip Brewer for home brewing', 69.99, 34.00, 'Brewers'),
 (@sub_brew_home, '4-Cup Drip Brewer', '4-Cup Drip Brewer for home brewing', 39.99, 19.00, 'Brewers'),
 (@sub_brew_home, 'Cold Brew Coffee Brewer', 'Cold Brew Coffee Brewer for home brewing', 54.99, 27.00, 'Brewers'),
 (@sub_brew_home, 'French Press Brewer 34oz', 'French Press Brewer 34oz for home brewing', 29.99, 14.00, 'Brewers'),
 (@sub_brew_home, 'Stovetop Percolator Brewer', 'Stovetop Percolator Brewer for home brewing', 34.99, 17.00, 'Brewers'),
 (@sub_brew_home, 'Pour-Over Coffee Brewer', 'Pour-Over Coffee Brewer for home brewing', 24.99, 12.00, 'Brewers'),
 (@sub_brew_home, 'Travel Single-Serve Brewer', 'Travel Single-Serve Brewer for home brewing', 69.99, 34.00, 'Brewers');

-- ==== Mugs & Drinkware ====
-- ---- Ceramic Mugs ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_mugs, 'Classic Logo Ceramic Mug', 'Ceramic coffee mug, classic logo style', 8.99, 3.50, 'Ceramic Mugs'),
 (@sub_mugs, 'Seasonal Ceramic Mug', 'Ceramic coffee mug, seasonal style', 9.99, 3.90, 'Ceramic Mugs'),
 (@sub_mugs, 'Holiday Ceramic Mug', 'Ceramic coffee mug, holiday style', 10.99, 4.30, 'Ceramic Mugs'),
 (@sub_mugs, 'Color-Changing Ceramic Mug', 'Ceramic coffee mug, color-changing style', 12.99, 5.20, 'Ceramic Mugs'),
 (@sub_mugs, 'Travel Ceramic Mug', 'Ceramic coffee mug, travel style', 11.99, 4.80, 'Ceramic Mugs'),
 (@sub_mugs, 'Espresso Ceramic Mug', 'Ceramic coffee mug, espresso style', 7.99, 3.00, 'Ceramic Mugs'),
 (@sub_mugs, 'Latte Ceramic Mug', 'Ceramic coffee mug, latte style', 9.49, 3.70, 'Ceramic Mugs'),
 (@sub_mugs, 'Oversized Ceramic Mug', 'Ceramic coffee mug, oversized style', 10.99, 4.30, 'Ceramic Mugs'),
 (@sub_mugs, 'Pastel Ceramic Mug', 'Ceramic coffee mug, pastel style', 8.99, 3.50, 'Ceramic Mugs'),
 (@sub_mugs, 'Striped Ceramic Mug', 'Ceramic coffee mug, striped style', 8.99, 3.50, 'Ceramic Mugs'),
 (@sub_mugs, 'Polka Dot Ceramic Mug', 'Ceramic coffee mug, polka dot style', 8.99, 3.50, 'Ceramic Mugs'),
 (@sub_mugs, 'Sunrise Ceramic Mug', 'Ceramic coffee mug, sunrise style', 9.49, 3.70, 'Ceramic Mugs'),
 (@sub_mugs, 'Sunset Ceramic Mug', 'Ceramic coffee mug, sunset style', 9.49, 3.70, 'Ceramic Mugs'),
 (@sub_mugs, 'Anniversary Ceramic Mug', 'Ceramic coffee mug, anniversary style', 11.99, 4.80, 'Ceramic Mugs'),
 (@sub_mugs, 'Employee Appreciation Ceramic Mug', 'Ceramic coffee mug, employee appreciation style', 9.99, 3.90, 'Ceramic Mugs');

-- ---- Plastic Mugs ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_mugs, 'Reusable Plastic Cold Cup', 'Reusable Plastic Cold Cup, durable everyday drinkware', 4.99, 1.80, 'Plastic Mugs'),
 (@sub_mugs, 'Insulated Plastic Tumbler Mug', 'Insulated Plastic Tumbler Mug, durable everyday drinkware', 7.99, 3.00, 'Plastic Mugs'),
 (@sub_mugs, 'Kids Plastic Mug', 'Kids Plastic Mug, durable everyday drinkware', 4.49, 1.60, 'Plastic Mugs'),
 (@sub_mugs, 'Color-Changing Plastic Mug', 'Color-Changing Plastic Mug, durable everyday drinkware', 6.99, 2.60, 'Plastic Mugs'),
 (@sub_mugs, 'Picnic Plastic Cup Set', 'Picnic Plastic Cup Set, durable everyday drinkware', 6.49, 2.40, 'Plastic Mugs'),
 (@sub_mugs, 'Stackable Plastic Mug', 'Stackable Plastic Mug, durable everyday drinkware', 5.49, 2.00, 'Plastic Mugs'),
 (@sub_mugs, 'Two-Tone Plastic Mug', 'Two-Tone Plastic Mug, durable everyday drinkware', 5.99, 2.20, 'Plastic Mugs'),
 (@sub_mugs, 'Glitter Plastic Cup', 'Glitter Plastic Cup, durable everyday drinkware', 5.99, 2.20, 'Plastic Mugs'),
 (@sub_mugs, 'Sports Plastic Bottle', 'Sports Plastic Bottle, durable everyday drinkware', 6.99, 2.60, 'Plastic Mugs'),
 (@sub_mugs, 'Kids Character Plastic Mug', 'Kids Character Plastic Mug, durable everyday drinkware', 5.49, 2.00, 'Plastic Mugs');

-- ---- Stainless ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_mugs, 'Stainless Steel Travel Mug', 'Stainless Steel Travel Mug, insulated stainless steel drinkware', 16.99, 7.50, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Tumbler 24oz', 'Stainless Steel Tumbler 24oz, insulated stainless steel drinkware', 19.99, 8.80, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Water Bottle', 'Stainless Steel Water Bottle, insulated stainless steel drinkware', 18.99, 8.30, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Cold Cup', 'Stainless Steel Cold Cup, insulated stainless steel drinkware', 17.99, 7.90, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Coffee Carafe', 'Stainless Steel Coffee Carafe, insulated stainless steel drinkware', 24.99, 11.00, 'Stainless'),
 (@sub_mugs, 'Stainless Steel French Press', 'Stainless Steel French Press, insulated stainless steel drinkware', 27.99, 12.50, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Espresso Cup Set', 'Stainless Steel Espresso Cup Set, insulated stainless steel drinkware', 22.99, 10.00, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Straw Tumbler', 'Stainless Steel Straw Tumbler, insulated stainless steel drinkware', 19.99, 8.80, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Mini Thermos', 'Stainless Steel Mini Thermos, insulated stainless steel drinkware', 15.99, 7.00, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Camp Mug', 'Stainless Steel Camp Mug, insulated stainless steel drinkware', 12.99, 5.60, 'Stainless'),
 (@sub_mugs, 'Stainless Steel Iced Coffee Cup', 'Stainless Steel Iced Coffee Cup, insulated stainless steel drinkware', 16.99, 7.50, 'Stainless');

-- ---- Tumblers ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_mugs, 'Insulated Tumbler 16oz', 'Insulated Tumbler 16oz, insulated for hot or cold drinks', 13.99, 5.80, 'Tumblers'),
 (@sub_mugs, 'Insulated Tumbler 20oz', 'Insulated Tumbler 20oz, insulated for hot or cold drinks', 15.99, 6.60, 'Tumblers'),
 (@sub_mugs, 'Straw Tumbler 30oz', 'Straw Tumbler 30oz, insulated for hot or cold drinks', 17.99, 7.40, 'Tumblers'),
 (@sub_mugs, 'Travel Tumbler with Handle', 'Travel Tumbler with Handle, insulated for hot or cold drinks', 16.99, 7.00, 'Tumblers'),
 (@sub_mugs, 'Color-Changing Tumbler', 'Color-Changing Tumbler, insulated for hot or cold drinks', 18.99, 7.80, 'Tumblers'),
 (@sub_mugs, 'Glitter Tumbler', 'Glitter Tumbler, insulated for hot or cold drinks', 17.99, 7.40, 'Tumblers'),
 (@sub_mugs, 'Sport Tumbler with Handle', 'Sport Tumbler with Handle, insulated for hot or cold drinks', 16.99, 7.00, 'Tumblers'),
 (@sub_mugs, 'Mini Tumbler 12oz', 'Mini Tumbler 12oz, insulated for hot or cold drinks', 11.99, 4.80, 'Tumblers'),
 (@sub_mugs, 'Tall Tumbler 40oz', 'Tall Tumbler 40oz, insulated for hot or cold drinks', 21.99, 9.20, 'Tumblers'),
 (@sub_mugs, 'Kids Tumbler with Lid', 'Kids Tumbler with Lid, insulated for hot or cold drinks', 9.99, 4.00, 'Tumblers'),
 (@sub_mugs, 'Etched Tumbler', 'Etched Tumbler, insulated for hot or cold drinks', 19.99, 8.30, 'Tumblers'),
 (@sub_mugs, 'Matte Finish Tumbler', 'Matte Finish Tumbler, insulated for hot or cold drinks', 18.99, 7.80, 'Tumblers');

-- ---- Glass Drinkware ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_mugs, 'Glass Coffee Mug', 'Glass Coffee Mug, clear glass drinkware', 9.99, 3.90, 'Glass Drinkware'),
 (@sub_mugs, 'Glass Cold Brew Cup', 'Glass Cold Brew Cup, clear glass drinkware', 8.99, 3.50, 'Glass Drinkware'),
 (@sub_mugs, 'Glass Iced Tea Glass', 'Glass Iced Tea Glass, clear glass drinkware', 7.99, 3.00, 'Glass Drinkware'),
 (@sub_mugs, 'Glass Pour-Over Carafe', 'Glass Pour-Over Carafe, clear glass drinkware', 14.99, 6.20, 'Glass Drinkware'),
 (@sub_mugs, 'Glass Travel Cup', 'Glass Travel Cup, clear glass drinkware', 11.99, 4.80, 'Glass Drinkware');

-- ==== Gift Sets & Holiday ====
-- ---- Gift Baskets ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_giftholiday, 'Coffee Lovers Gift Basket', 'Coffee Lovers themed gift basket', 34.99, 16.00, 'Gift Baskets'),
 (@sub_giftholiday, 'Tea Lovers Gift Basket', 'Tea Lovers themed gift basket', 29.99, 13.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Mug and Coffee Gift Set', 'Mug and Coffee Gift Set themed gift basket', 19.99, 9.00, 'Gift Baskets'),
 (@sub_giftholiday, 'Deluxe Coffee Gift Basket', 'Deluxe Coffee themed gift basket', 44.99, 21.00, 'Gift Baskets'),
 (@sub_giftholiday, 'Breakfast Bundle Gift Basket', 'Breakfast Bundle themed gift basket', 32.99, 15.00, 'Gift Baskets'),
 (@sub_giftholiday, 'Espresso Lovers Gift Basket', 'Espresso Lovers themed gift basket', 36.99, 17.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Cold Brew Gift Basket', 'Cold Brew themed gift basket', 31.99, 15.00, 'Gift Baskets'),
 (@sub_giftholiday, 'New Home Gift Basket', 'New Home themed gift basket', 29.99, 13.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Thank You Gift Basket', 'Thank You themed gift basket', 27.99, 12.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Get Well Gift Basket', 'Get Well themed gift basket', 27.99, 12.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Birthday Gift Basket', 'Birthday themed gift basket', 29.99, 13.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Congratulations Gift Basket', 'Congratulations themed gift basket', 29.99, 13.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Housewarming Gift Basket', 'Housewarming themed gift basket', 32.99, 15.00, 'Gift Baskets'),
 (@sub_giftholiday, 'Corporate Gift Basket', 'Corporate themed gift basket', 39.99, 18.50, 'Gift Baskets'),
 (@sub_giftholiday, 'Anniversary Gift Basket', 'Anniversary themed gift basket', 34.99, 16.00, 'Gift Baskets'),
 (@sub_giftholiday, 'Teacher Appreciation Gift Basket', 'Teacher Appreciation themed gift basket', 24.99, 11.00, 'Gift Baskets'),
 (@sub_giftholiday, 'Client Gift Basket', 'Client themed gift basket', 44.99, 21.00, 'Gift Baskets');

-- ---- Holiday ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_giftholiday, 'Holiday Ornament Mug', 'Holiday Ornament Mug, seasonal holiday item', 6.99, 2.70, 'Holiday'),
 (@sub_giftholiday, 'Holiday Coffee Sampler', 'Holiday Coffee Sampler, seasonal holiday item', 14.99, 6.50, 'Holiday'),
 (@sub_giftholiday, 'Holiday Gift Box Set', 'Holiday Gift Box Set, seasonal holiday item', 39.99, 18.50, 'Holiday'),
 (@sub_giftholiday, 'Peppermint Mocha Gift Set', 'Peppermint Mocha Gift Set, seasonal holiday item', 24.99, 11.50, 'Holiday'),
 (@sub_giftholiday, 'Holiday Travel Mug', 'Holiday Travel Mug, seasonal holiday item', 14.99, 6.50, 'Holiday'),
 (@sub_giftholiday, 'Winter Blend Gift Bag', 'Winter Blend Gift Bag, seasonal holiday item', 12.99, 5.70, 'Holiday'),
 (@sub_giftholiday, 'Halloween Treat Mug', 'Halloween Treat Mug, seasonal holiday item', 9.99, 3.90, 'Holiday'),
 (@sub_giftholiday, 'Thanksgiving Coffee Gift Set', 'Thanksgiving Coffee Gift Set, seasonal holiday item', 26.99, 12.50, 'Holiday'),
 (@sub_giftholiday, 'Valentine\'s Day Gift Set', 'Valentine\'s Day Gift Set, seasonal holiday item', 22.99, 10.50, 'Holiday'),
 (@sub_giftholiday, 'Easter Coffee Gift Set', 'Easter Coffee Gift Set, seasonal holiday item', 22.99, 10.50, 'Holiday'),
 (@sub_giftholiday, 'Mother\'s Day Gift Set', 'Mother\'s Day Gift Set, seasonal holiday item', 27.99, 12.50, 'Holiday'),
 (@sub_giftholiday, 'Father\'s Day Gift Set', 'Father\'s Day Gift Set, seasonal holiday item', 27.99, 12.50, 'Holiday'),
 (@sub_giftholiday, 'Graduation Gift Set', 'Graduation Gift Set, seasonal holiday item', 24.99, 11.50, 'Holiday'),
 (@sub_giftholiday, 'New Year Coffee Sampler', 'New Year Coffee Sampler, seasonal holiday item', 16.99, 7.50, 'Holiday'),
 (@sub_giftholiday, 'Fourth of July Tumbler', 'Fourth of July Tumbler, seasonal holiday item', 14.99, 6.20, 'Holiday'),
 (@sub_giftholiday, 'Autumn Spice Gift Bag', 'Autumn Spice Gift Bag, seasonal holiday item', 13.99, 6.00, 'Holiday'),
 (@sub_giftholiday, 'Winter Wonderland Mug', 'Winter Wonderland Mug, seasonal holiday item', 10.99, 4.30, 'Holiday'),
 (@sub_giftholiday, 'Snowflake Ceramic Mug', 'Snowflake Ceramic Mug, seasonal holiday item', 10.99, 4.30, 'Holiday'),
 (@sub_giftholiday, 'Santa Hat Ceramic Mug', 'Santa Hat Ceramic Mug, seasonal holiday item', 10.99, 4.30, 'Holiday'),
 (@sub_giftholiday, 'Reindeer Ceramic Mug', 'Reindeer Ceramic Mug, seasonal holiday item', 10.99, 4.30, 'Holiday');

-- ---- Seasonal Sets ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_giftholiday, 'Spring Bloom Gift Set', 'Spring Bloom themed gift set', 24.99, 11.50, 'Seasonal Sets'),
 (@sub_giftholiday, 'Summer Sun Gift Set', 'Summer Sun themed gift set', 24.99, 11.50, 'Seasonal Sets'),
 (@sub_giftholiday, 'Fall Harvest Gift Set', 'Fall Harvest themed gift set', 26.99, 12.50, 'Seasonal Sets'),
 (@sub_giftholiday, 'Back to School Gift Set', 'Back to School themed gift set', 19.99, 9.00, 'Seasonal Sets'),
 (@sub_giftholiday, 'New Year Fresh Start Gift Set', 'New Year Fresh Start themed gift set', 22.99, 10.50, 'Seasonal Sets'),
 (@sub_giftholiday, 'Cozy Nights Gift Set', 'Cozy Nights themed gift set', 26.99, 12.50, 'Seasonal Sets'),
 (@sub_giftholiday, 'Weekend Brunch Gift Set', 'Weekend Brunch themed gift set', 28.99, 13.00, 'Seasonal Sets'),
 (@sub_giftholiday, 'Sunday Morning Gift Set', 'Sunday Morning themed gift set', 24.99, 11.50, 'Seasonal Sets'),
 (@sub_giftholiday, 'Date Night Gift Set', 'Date Night themed gift set', 29.99, 13.50, 'Seasonal Sets'),
 (@sub_giftholiday, 'Girls Night Gift Set', 'Girls Night themed gift set', 27.99, 12.50, 'Seasonal Sets');

-- ---- Corporate & Occasion ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_giftholiday, 'Retirement Gift Basket', 'Retirement occasion gift basket', 32.99, 15.00, 'Corporate & Occasion'),
 (@sub_giftholiday, 'Welcome Aboard Gift Basket', 'Welcome Aboard occasion gift basket', 27.99, 12.50, 'Corporate & Occasion'),
 (@sub_giftholiday, 'Sympathy Gift Basket', 'Sympathy occasion gift basket', 29.99, 13.50, 'Corporate & Occasion'),
 (@sub_giftholiday, 'Just Because Gift Basket', 'Just Because occasion gift basket', 22.99, 10.50, 'Corporate & Occasion'),
 (@sub_giftholiday, 'Promotion Gift Basket', 'Promotion occasion gift basket', 29.99, 13.50, 'Corporate & Occasion'),
 (@sub_giftholiday, 'Milestone Gift Basket', 'Milestone occasion gift basket', 34.99, 16.00, 'Corporate & Occasion');

-- ==== Grab & Go Treats ====
-- ---- Snacks ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_grabgo, 'Trail Mix Bag', 'Trail Mix Bag, grab-and-go snack', 3.49, 1.30, 'Snacks'),
 (@sub_grabgo, 'Pretzel Bag', 'Pretzel Bag, grab-and-go snack', 2.79, 1.00, 'Snacks'),
 (@sub_grabgo, 'Chips Bag', 'Chips Bag, grab-and-go snack', 2.49, 0.90, 'Snacks'),
 (@sub_grabgo, 'Popcorn Bag', 'Popcorn Bag, grab-and-go snack', 2.99, 1.05, 'Snacks'),
 (@sub_grabgo, 'Granola Bar Box', 'Granola Bar Box, grab-and-go snack', 5.99, 2.40, 'Snacks'),
 (@sub_grabgo, 'Fruit Snack Pack', 'Fruit Snack Pack, grab-and-go snack', 2.49, 0.90, 'Snacks'),
 (@sub_grabgo, 'Cheese Cracker Pack', 'Cheese Cracker Pack, grab-and-go snack', 2.49, 0.90, 'Snacks'),
 (@sub_grabgo, 'Peanut Butter Cracker Pack', 'Peanut Butter Cracker Pack, grab-and-go snack', 2.49, 0.90, 'Snacks'),
 (@sub_grabgo, 'Beef Jerky Bag', 'Beef Jerky Bag, grab-and-go snack', 4.99, 1.90, 'Snacks'),
 (@sub_grabgo, 'Veggie Chips Bag', 'Veggie Chips Bag, grab-and-go snack', 2.99, 1.05, 'Snacks'),
 (@sub_grabgo, 'Rice Cake Pack', 'Rice Cake Pack, grab-and-go snack', 2.29, 0.80, 'Snacks'),
 (@sub_grabgo, 'Nut Mix Bag', 'Nut Mix Bag, grab-and-go snack', 3.99, 1.50, 'Snacks'),
 (@sub_grabgo, 'Dried Fruit Pack', 'Dried Fruit Pack, grab-and-go snack', 3.49, 1.30, 'Snacks'),
 (@sub_grabgo, 'Pita Chips Bag', 'Pita Chips Bag, grab-and-go snack', 2.99, 1.05, 'Snacks'),
 (@sub_grabgo, 'Cheese Puffs Bag', 'Cheese Puffs Bag, grab-and-go snack', 2.49, 0.90, 'Snacks'),
 (@sub_grabgo, 'Sesame Snack Bar', 'Sesame Snack Bar, grab-and-go snack', 2.79, 1.00, 'Snacks'),
 (@sub_grabgo, 'Protein Bar Box', 'Protein Bar Box, grab-and-go snack', 6.49, 2.60, 'Snacks'),
 (@sub_grabgo, 'Oatmeal Bar Box', 'Oatmeal Bar Box, grab-and-go snack', 5.49, 2.10, 'Snacks');

-- ---- Candy ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_grabgo, 'Chocolate Bar', 'Chocolate Bar, single serving', 2.29, 0.85, 'Candy'),
 (@sub_grabgo, 'Gummy Candy Bag', 'Gummy Candy Bag, single serving', 2.49, 0.90, 'Candy'),
 (@sub_grabgo, 'Mint Tin', 'Mint Tin, single serving', 1.99, 0.70, 'Candy'),
 (@sub_grabgo, 'Chewing Gum Pack', 'Chewing Gum Pack, single serving', 1.79, 0.60, 'Candy'),
 (@sub_grabgo, 'Caramel Chews Bag', 'Caramel Chews Bag, single serving', 2.79, 1.00, 'Candy'),
 (@sub_grabgo, 'Licorice Twists Bag', 'Licorice Twists Bag, single serving', 2.49, 0.90, 'Candy'),
 (@sub_grabgo, 'Toffee Bites Bag', 'Toffee Bites Bag, single serving', 2.79, 1.00, 'Candy'),
 (@sub_grabgo, 'Peanut Butter Cups Pack', 'Peanut Butter Cups Pack, single serving', 2.99, 1.10, 'Candy'),
 (@sub_grabgo, 'Fruit Chews Bag', 'Fruit Chews Bag, single serving', 2.29, 0.85, 'Candy'),
 (@sub_grabgo, 'Sour Candy Bag', 'Sour Candy Bag, single serving', 2.49, 0.90, 'Candy'),
 (@sub_grabgo, 'Chocolate Covered Pretzels Bag', 'Chocolate Covered Pretzels Bag, single serving', 3.49, 1.30, 'Candy'),
 (@sub_grabgo, 'Chocolate Covered Espresso Beans Bag', 'Chocolate Covered Espresso Beans Bag, single serving', 3.99, 1.50, 'Candy'),
 (@sub_grabgo, 'Mint Chocolate Bar', 'Mint Chocolate Bar, single serving', 2.49, 0.90, 'Candy'),
 (@sub_grabgo, 'Dark Chocolate Bar', 'Dark Chocolate Bar, single serving', 2.49, 0.90, 'Candy'),
 (@sub_grabgo, 'Milk Chocolate Bar', 'Milk Chocolate Bar, single serving', 2.29, 0.85, 'Candy');

-- ---- Bottled ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_grabgo, 'Bottled Water', 'Bottled water, ready to grab and go', 1.99, 0.50, 'Bottled'),
 (@sub_grabgo, 'Bottled Iced Coffee', 'Bottled iced coffee, ready to grab and go', 3.99, 1.60, 'Bottled'),
 (@sub_grabgo, 'Bottled Juice', 'Bottled juice, ready to grab and go', 2.99, 1.10, 'Bottled'),
 (@sub_grabgo, 'Bottled Sports Drink', 'Bottled sports drink, ready to grab and go', 2.79, 1.05, 'Bottled'),
 (@sub_grabgo, 'Bottled Energy Drink', 'Bottled energy drink, ready to grab and go', 3.49, 1.40, 'Bottled'),
 (@sub_grabgo, 'Bottled Sparkling Water', 'Bottled sparkling water, ready to grab and go', 2.29, 0.80, 'Bottled'),
 (@sub_grabgo, 'Bottled Lemonade', 'Bottled lemonade, ready to grab and go', 2.99, 1.10, 'Bottled'),
 (@sub_grabgo, 'Bottled Iced Tea', 'Bottled iced tea, ready to grab and go', 2.99, 1.10, 'Bottled'),
 (@sub_grabgo, 'Bottled Cold Brew Coffee', 'Bottled cold brew coffee, ready to grab and go', 4.29, 1.70, 'Bottled'),
 (@sub_grabgo, 'Bottled Protein Shake', 'Bottled protein shake, ready to grab and go', 3.99, 1.60, 'Bottled'),
 (@sub_grabgo, 'Bottled Coconut Water', 'Bottled coconut water, ready to grab and go', 3.49, 1.30, 'Bottled'),
 (@sub_grabgo, 'Bottled Fruit Smoothie', 'Bottled fruit smoothie, ready to grab and go', 3.99, 1.60, 'Bottled'),
 (@sub_grabgo, 'Bottled Chocolate Milk', 'Bottled chocolate milk, ready to grab and go', 2.79, 1.05, 'Bottled');

-- ---- Breakfast On The Go ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_grabgo, 'Yogurt Parfait Pack', 'Yogurt Parfait Pack, grab-and-go breakfast', 3.99, 1.60, 'Breakfast On The Go'),
 (@sub_grabgo, 'Overnight Oats Pack', 'Overnight Oats Pack, grab-and-go breakfast', 4.29, 1.70, 'Breakfast On The Go'),
 (@sub_grabgo, 'Breakfast Cookie', 'Breakfast Cookie, grab-and-go breakfast', 2.79, 1.00, 'Breakfast On The Go'),
 (@sub_grabgo, 'Muffin Snack Pack', 'Muffin Snack Pack, grab-and-go breakfast', 2.99, 1.10, 'Breakfast On The Go'),
 (@sub_grabgo, 'Fruit and Nut Bar', 'Fruit and Nut Bar, grab-and-go breakfast', 2.79, 1.00, 'Breakfast On The Go');

-- ==== Gift Cards & More ====
-- ---- Gift Cards ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_giftcards, '$10 Gift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards'),
 (@sub_giftcards, '$25 Gift Card', 'Prepaid store gift card', 25.00, 25.00, 'Gift Cards'),
 (@sub_giftcards, '$50 Gift Card', 'Prepaid store gift card', 50.00, 50.00, 'Gift Cards'),
 (@sub_giftcards, '$10 Espresso Gift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards'),
 (@sub_giftcards, '$10 Coffee Lovers Gift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards'),
 (@sub_giftcards, '$25 Holiday Gift Card', 'Prepaid store gift card', 25.00, 25.00, 'Gift Cards'),
 (@sub_giftcards, '$5 Gift Card', 'Prepaid store gift card', 5.00, 5.00, 'Gift Cards'),
 (@sub_giftcards, '$15 Gift Card', 'Prepaid store gift card', 15.00, 15.00, 'Gift Cards'),
 (@sub_giftcards, '$20 Gift Card', 'Prepaid store gift card', 20.00, 20.00, 'Gift Cards'),
 (@sub_giftcards, '$30 Gift Card', 'Prepaid store gift card', 30.00, 30.00, 'Gift Cards'),
 (@sub_giftcards, '$75 Gift Card', 'Prepaid store gift card', 75.00, 75.00, 'Gift Cards'),
 (@sub_giftcards, '$100 Gift Card', 'Prepaid store gift card', 100.00, 100.00, 'Gift Cards'),
 (@sub_giftcards, '$10 Tea Lovers Gift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards'),
 (@sub_giftcards, '$10 Birthday Gift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards'),
 (@sub_giftcards, '$10 Thank You Gift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards'),
 (@sub_giftcards, '$25 Congratulations Gift Card', 'Prepaid store gift card', 25.00, 25.00, 'Gift Cards'),
 (@sub_giftcards, '$25 Anniversary Gift Card', 'Prepaid store gift card', 25.00, 25.00, 'Gift Cards'),
 (@sub_giftcards, '$10 New Customer Gift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards'),
 (@sub_giftcards, '$50 Corporate Gift Card', 'Prepaid store gift card', 50.00, 50.00, 'Gift Cards'),
 (@sub_giftcards, '$20 Seasonal Gift Card', 'Prepaid store gift card', 20.00, 20.00, 'Gift Cards'),
 (@sub_giftcards, '$200 Gift Card', 'Prepaid store gift card', 200.00, 200.00, 'Gift Cards'),
 (@sub_giftcards, '$10 eGift Card', 'Prepaid store gift card', 10.00, 10.00, 'Gift Cards');

-- ---- Accessories ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_giftcards, 'Reusable Straw Set', 'Reusable Straw Set for home coffee and tea brewing', 6.99, 2.70, 'Accessories'),
 (@sub_giftcards, 'Coffee Filter Pack', 'Coffee Filter Pack for home coffee and tea brewing', 4.99, 1.90, 'Accessories'),
 (@sub_giftcards, 'Travel Mug Lid Replacement', 'Travel Mug Lid Replacement for home coffee and tea brewing', 3.99, 1.50, 'Accessories'),
 (@sub_giftcards, 'Branded Tote Bag', 'Branded Tote Bag for home coffee and tea brewing', 9.99, 4.00, 'Accessories'),
 (@sub_giftcards, 'Coffee Scoop', 'Coffee Scoop for home coffee and tea brewing', 4.99, 1.90, 'Accessories'),
 (@sub_giftcards, 'Milk Frother', 'Milk Frother for home coffee and tea brewing', 12.99, 5.50, 'Accessories'),
 (@sub_giftcards, 'Travel Mug Strap', 'Travel Mug Strap for home coffee and tea brewing', 3.99, 1.50, 'Accessories'),
 (@sub_giftcards, 'Cup Sleeve Pack', 'Cup Sleeve Pack for home coffee and tea brewing', 3.49, 1.30, 'Accessories'),
 (@sub_giftcards, 'Stirring Spoon Set', 'Stirring Spoon Set for home coffee and tea brewing', 4.49, 1.70, 'Accessories'),
 (@sub_giftcards, 'Coffee Scale', 'Coffee Scale for home coffee and tea brewing', 24.99, 11.00, 'Accessories'),
 (@sub_giftcards, 'Drip Tray', 'Drip Tray for home coffee and tea brewing', 6.99, 2.70, 'Accessories'),
 (@sub_giftcards, 'Cleaning Brush Set', 'Cleaning Brush Set for home coffee and tea brewing', 5.99, 2.30, 'Accessories'),
 (@sub_giftcards, 'Storage Canister', 'Storage Canister for home coffee and tea brewing', 9.99, 4.00, 'Accessories'),
 (@sub_giftcards, 'Spill-Proof Lid Pack', 'Spill-Proof Lid Pack for home coffee and tea brewing', 4.99, 1.90, 'Accessories'),
 (@sub_giftcards, 'Reusable Cup Sleeve', 'Reusable Cup Sleeve for home coffee and tea brewing', 3.99, 1.50, 'Accessories'),
 (@sub_giftcards, 'Bamboo Coaster Set', 'Bamboo Coaster Set for home coffee and tea brewing', 7.99, 3.10, 'Accessories'),
 (@sub_giftcards, 'Coffee Bean Grinder', 'Coffee Bean Grinder for home coffee and tea brewing', 34.99, 16.00, 'Accessories'),
 (@sub_giftcards, 'Espresso Tamper', 'Espresso Tamper for home coffee and tea brewing', 9.99, 4.00, 'Accessories'),
 (@sub_giftcards, 'Pour-Over Filter Pack', 'Pour-Over Filter Pack for home coffee and tea brewing', 4.49, 1.70, 'Accessories'),
 (@sub_giftcards, 'Cold Brew Filter Bag', 'Cold Brew Filter Bag for home coffee and tea brewing', 5.99, 2.30, 'Accessories');

-- ---- Apparel & Merch ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_giftcards, 'Logo T-Shirt', 'Logo T-Shirt, shop branded merchandise', 14.99, 6.20, 'Apparel & Merch'),
 (@sub_giftcards, 'Logo Baseball Cap', 'Logo Baseball Cap, shop branded merchandise', 12.99, 5.40, 'Apparel & Merch'),
 (@sub_giftcards, 'Logo Hoodie', 'Logo Hoodie, shop branded merchandise', 29.99, 13.50, 'Apparel & Merch'),
 (@sub_giftcards, 'Logo Beanie', 'Logo Beanie, shop branded merchandise', 9.99, 4.00, 'Apparel & Merch'),
 (@sub_giftcards, 'Logo Apron', 'Logo Apron, shop branded merchandise', 16.99, 7.00, 'Apparel & Merch'),
 (@sub_giftcards, 'Enamel Pin Set', 'Enamel Pin Set, shop branded merchandise', 7.99, 3.00, 'Apparel & Merch'),
 (@sub_giftcards, 'Sticker Pack', 'Sticker Pack, shop branded merchandise', 3.99, 1.50, 'Apparel & Merch'),
 (@sub_giftcards, 'Keychain', 'Keychain, shop branded merchandise', 4.99, 1.90, 'Apparel & Merch'),
 (@sub_giftcards, 'Small Tote Bag', 'Small Tote Bag, shop branded merchandise', 8.99, 3.50, 'Apparel & Merch'),
 (@sub_giftcards, 'Branded Notebook', 'Branded Notebook, shop branded merchandise', 6.99, 2.70, 'Apparel & Merch');
