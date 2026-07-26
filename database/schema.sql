-- POS System Database Schema
-- Run with: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS pos_system;
USE pos_system;

-- ============ USERS & ROLES ============
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN','MANAGER','CASHIER') NOT NULL DEFAULT 'CASHIER',
    employee_id INT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============ EMPLOYEES ============
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(100),
    position VARCHAR(50),
    hourly_rate DECIMAL(10,2) DEFAULT 0,
    payment_preference ENUM('DIRECT_DEPOSIT','CASH','CHECK') DEFAULT 'CASH',
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_routing_number VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) REFERENCES employees(id);

-- ============ MENU / CATEGORIES ============
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    station VARCHAR(50) DEFAULT 'Main Counter',
    sort_order INT DEFAULT 0,
    parent_id INT NULL, -- NULL = top-level department tab (Beverages, Bakery, ...); set = subcategory shown in the sidebar under its department
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE menu_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    cost DECIMAL(10,2) DEFAULT 0,
    image_path VARCHAR(255),
    section VARCHAR(50) NULL, -- groups items under a header inside a subcategory's item grid (e.g. "Single Espresso")
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- ============ INVENTORY / INGREDIENTS ============
CREATE TABLE ingredients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    unit VARCHAR(20) NOT NULL,
    stock_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    low_stock_threshold DECIMAL(10,2) NOT NULL DEFAULT 0,
    unit_cost DECIMAL(10,2) DEFAULT 0
);

CREATE TABLE recipe_ingredients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity_required DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

-- ============ RECIPE STEPS (multi-language prep instructions) ============
CREATE TABLE recipe_steps (
    id INT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id INT NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    step_number INT NOT NULL,
    instruction TEXT NOT NULL,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    UNIQUE KEY uq_recipe_step (menu_item_id, language, step_number)
);

CREATE TABLE inventory_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id INT NOT NULL,
    change_amount DECIMAL(10,2) NOT NULL,
    reason ENUM('SALE','PURCHASE_IN','WASTE','CORRECTION') NOT NULL,
    reference_order_id INT NULL,
    user_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

-- ============ SIZES & MODIFIERS (global, applied at order time) ============
CREATE TABLE sizes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    price_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    sort_order INT DEFAULT 0
);

CREATE TABLE modifiers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
    modifier_group VARCHAR(50) NOT NULL, -- e.g. 'Milk', 'Sweetener', 'Flavor Swirl'
    sort_order INT DEFAULT 0
);

-- ============ SUPPLIERS ============
CREATE TABLE suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(100),
    address VARCHAR(255)
);

-- ============ CUSTOMERS / LOYALTY ============
CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(100),
    loyalty_points INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============ ORDERS ============
CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_source ENUM('IN_STORE','PHONE','ONLINE') NOT NULL DEFAULT 'IN_STORE',
    order_type ENUM('DINE_IN','PICKUP','DELIVERY') NOT NULL DEFAULT 'DINE_IN',
    customer_id INT NULL,
    user_id INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    status ENUM('OPEN','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    menu_item_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    is_loyalty_free BOOLEAN NOT NULL DEFAULT FALSE,
    station_status ENUM('PENDING','PREPARING','READY','SERVED') NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

-- ============ PAYMENTS ============
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    method ENUM('CASH','CARD','MOBILE_BANKING') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('SUCCESS','FAILED') NOT NULL,
    reference_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ============ DELIVERY ============
CREATE TABLE deliveries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    customer_address VARCHAR(255),
    assigned_to VARCHAR(100),
    status ENUM('UNASSIGNED','ASSIGNED','PICKED_UP','DELIVERED') NOT NULL DEFAULT 'UNASSIGNED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ============ PAYROLL / TIME CLOCK ============
CREATE TABLE time_clock (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    clock_in DATETIME NOT NULL,
    clock_out DATETIME NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE holidays (
    id INT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    description VARCHAR(100)
);

CREATE TABLE payroll_runs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    regular_hours DECIMAL(6,2) DEFAULT 0,
    overtime_hours DECIMAL(6,2) DEFAULT 0,
    weekend_hours DECIMAL(6,2) DEFAULT 0,
    holiday_hours DECIMAL(6,2) DEFAULT 0,
    deductions DECIMAL(10,2) DEFAULT 0,
    gross_pay DECIMAL(10,2) DEFAULT 0,
    net_pay DECIMAL(10,2) DEFAULT 0,
    federal_tax DECIMAL(10,2) DEFAULT 0,
    social_security DECIMAL(10,2) DEFAULT 0,
    medicare DECIMAL(10,2) DEFAULT 0,
    state_tax DECIMAL(10,2) DEFAULT 0,
    city_tax DECIMAL(10,2) DEFAULT 0,
    pay_date DATE NULL,
    check_number VARCHAR(20) NULL,
    payout_method ENUM('DIRECT_DEPOSIT','CASH','CHECK'),
    payment_reference VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- ============ SAMPLE DATA ============
-- Department -> Subcategory hierarchy (matches the POS screen: top department tabs +
-- a right-side subcategory sidebar). All item names below are original to this shop.

-- ---- Top-level departments (header tabs) ----
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

-- ---- Bakery / Sandwiches / Retail subcategories ----
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Bagels & Muffins', 'Bakery', 1, @dept_bakery);
SET @sub_bagels = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Donuts & Donut Holes', 'Bakery', 2, @dept_bakery);
SET @sub_donuts = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Sweet Treats', 'Bakery', 3, @dept_bakery);
SET @sub_treats = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Grab & Go', 'Bakery', 4, @dept_bakery);
SET @sub_grabgo = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Sandwiches & More', 'Sandwich Station', 1, @dept_sandwiches);
SET @sub_sandwiches = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Snacks & Wraps', 'Snack Station', 2, @dept_sandwiches);
SET @sub_snacks = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Brew At Home', 'Retail', 1, @dept_retail);
SET @sub_brew_home = LAST_INSERT_ID();

-- ---- Beverages subcategories (sidebar) ----
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

-- ---- Bakery / Sandwiches / Retail items ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_bagels, 'Plain Bagel', 'Toasted plain bagel', 2.25, 0.55, 'Bagels'),
 (@sub_bagels, 'Blueberry Muffin', 'Fresh baked blueberry muffin', 2.95, 0.80, 'Muffins'),
 (@sub_donuts, 'Glazed Donut', 'Classic glazed donut', 1.79, 0.35, 'Classic'),
 (@sub_donuts, 'Chocolate Frosted Donut', 'Chocolate frosted donut', 1.79, 0.35, 'Classic'),
 (@sub_sandwiches, 'Turkey Sandwich', 'Turkey, lettuce, tomato', 6.50, 2.50, 'Sandwiches'),
 (@sub_sandwiches, 'Grilled Cheese', 'Melted cheese on grilled bread', 5.25, 1.60, 'Sandwiches'),
 (@sub_snacks, 'Veggie Wrap', 'Grilled vegetables in a flour wrap', 5.75, 1.80, 'Wraps'),
 (@sub_snacks, 'Hash Browns', 'Crispy golden hash browns', 2.50, 0.60, 'Snacks'),
 (@sub_brew_home, 'Ground Coffee Bag', '12oz bag of ground coffee', 9.99, 4.50, 'Bagged Coffee'),
 (@sub_brew_home, 'K-Cup Pack (10ct)', 'Single-serve coffee pods', 8.99, 4.00, 'Bagged Coffee');

-- ---- Donuts & Donut Holes (expanded) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_donuts, 'Powdered Sugar Donut', 'Classic donut dusted with powdered sugar', 1.79, 0.35, 'Classic Donuts'),
 (@sub_donuts, 'Cinnamon Sugar Donut', 'Classic donut rolled in cinnamon sugar', 1.79, 0.35, 'Classic Donuts'),
 (@sub_donuts, 'Old Fashioned Donut', 'Dense cake donut with a crisp glazed edge', 1.89, 0.40, 'Classic Donuts'),
 (@sub_donuts, 'Buttermilk Old Fashioned Donut', 'Buttermilk cake donut with a crisp glazed edge', 1.89, 0.40, 'Classic Donuts'),
 (@sub_donuts, 'Chocolate Glazed Donut', 'Chocolate glazed classic donut', 1.89, 0.40, 'Classic Donuts'),
 (@sub_donuts, 'Vanilla Frosted Sprinkle Donut', 'Vanilla frosted donut with rainbow sprinkles', 1.89, 0.40, 'Classic Donuts'),
 (@sub_donuts, 'Chocolate Frosted Sprinkle Donut', 'Chocolate frosted donut with rainbow sprinkles', 1.89, 0.40, 'Classic Donuts'),
 (@sub_donuts, 'Strawberry Frosted Sprinkle Donut', 'Strawberry frosted donut with rainbow sprinkles', 1.89, 0.40, 'Classic Donuts'),
 (@sub_donuts, 'Jelly Filled Donut', 'Classic donut filled with sweet jelly', 1.99, 0.45, 'Filled Donuts'),
 (@sub_donuts, 'Boston Cream Donut', 'Donut filled with custard, topped with chocolate', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Bavarian Cream Donut', 'Donut filled with rich Bavarian cream', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Chocolate Cream Filled Donut', 'Donut filled with chocolate cream', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Lemon Filled Donut', 'Donut filled with tangy lemon custard', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Caramel Filled Donut', 'Donut filled with sweet caramel', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Glazed Donut Holes (10-pack)', 'Ten bite-sized glazed donut holes', 3.49, 0.90, 'Donut Holes'),
 (@sub_donuts, 'Chocolate Donut Holes (10-pack)', 'Ten bite-sized chocolate donut holes', 3.49, 0.90, 'Donut Holes'),
 (@sub_donuts, 'Powdered Donut Holes (10-pack)', 'Ten bite-sized powdered sugar donut holes', 3.49, 0.90, 'Donut Holes'),
 (@sub_donuts, 'Cinnamon Donut Holes (10-pack)', 'Ten bite-sized cinnamon sugar donut holes', 3.49, 0.90, 'Donut Holes'),
 (@sub_donuts, 'Assorted Donut Holes (25-pack)', 'Twenty-five assorted bite-sized donut holes', 7.49, 1.90, 'Donut Holes'),
 (@sub_donuts, 'Assorted Donut Holes (50-pack)', 'Fifty assorted bite-sized donut holes', 13.99, 3.60, 'Donut Holes'),
 (@sub_donuts, 'Apple Fritter', 'Fried pastry packed with apple pieces and glaze', 2.79, 0.65, 'Fancy Donuts'),
 (@sub_donuts, 'Long John', 'Oblong yeast donut with chocolate frosting', 2.29, 0.55, 'Fancy Donuts'),
 (@sub_donuts, 'Maple Frosted Long John', 'Oblong yeast donut with maple frosting', 2.29, 0.55, 'Fancy Donuts'),
 (@sub_donuts, 'Chocolate Eclair', 'Cream-filled pastry topped with chocolate', 2.99, 0.75, 'Fancy Donuts'),
 (@sub_donuts, 'Coffee Roll', 'Twisted yeast pastry with vanilla icing', 2.29, 0.55, 'Fancy Donuts'),
 (@sub_donuts, 'Frosted Coffee Roll', 'Twisted yeast pastry with chocolate icing', 2.39, 0.55, 'Fancy Donuts'),
 (@sub_donuts, 'Cinnamon Bun', 'Soft roll swirled with cinnamon and icing', 2.99, 0.70, 'Fancy Donuts'),
 (@sub_donuts, 'Chocolate Dipped Cruller', 'Twisted French cruller dipped in chocolate', 2.49, 0.60, 'Fancy Donuts');

-- ---- Bagels & Muffins (expanded) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_bagels, 'Everything Bagel', 'Toasted bagel topped with seeds, garlic and salt', 2.45, 0.60, 'Bagels'),
 (@sub_bagels, 'Sesame Bagel', 'Toasted bagel topped with sesame seeds', 2.35, 0.55, 'Bagels'),
 (@sub_bagels, 'Cinnamon Raisin Bagel', 'Toasted bagel with cinnamon and raisins', 2.45, 0.60, 'Bagels'),
 (@sub_bagels, 'Blueberry Bagel', 'Toasted bagel studded with blueberries', 2.45, 0.60, 'Bagels'),
 (@sub_bagels, 'Whole Wheat Bagel', 'Toasted whole wheat bagel', 2.35, 0.55, 'Bagels'),
 (@sub_bagels, 'Onion Bagel', 'Toasted bagel topped with onion flakes', 2.35, 0.55, 'Bagels'),
 (@sub_bagels, 'Chocolate Chip Muffin', 'Fresh baked muffin loaded with chocolate chips', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Banana Nut Muffin', 'Fresh baked banana muffin with walnuts', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Corn Muffin', 'Fresh baked classic corn muffin', 2.79, 0.75, 'Muffins'),
 (@sub_bagels, 'Bran Muffin', 'Fresh baked hearty bran muffin', 2.79, 0.75, 'Muffins'),
 (@sub_bagels, 'Cinnamon Streusel Muffin', 'Fresh baked muffin with cinnamon streusel topping', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Lemon Poppy Seed Muffin', 'Fresh baked lemon muffin with poppy seeds', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Plain Cream Cheese', 'Individual portion of plain cream cheese', 0.99, 0.25, 'Spreads'),
 (@sub_bagels, 'Strawberry Cream Cheese', 'Individual portion of strawberry cream cheese', 1.09, 0.30, 'Spreads'),
 (@sub_bagels, 'Chive Cream Cheese', 'Individual portion of chive cream cheese', 1.09, 0.30, 'Spreads'),
 (@sub_bagels, 'Butter', 'Individual portion of butter', 0.59, 0.15, 'Spreads');

-- ---- Sweet Treats ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_treats, 'Chocolate Chip Cookie', 'Fresh baked chocolate chip cookie', 1.99, 0.45, 'Cookies'),
 (@sub_treats, 'Oatmeal Raisin Cookie', 'Fresh baked oatmeal raisin cookie', 1.99, 0.45, 'Cookies'),
 (@sub_treats, 'Sugar Cookie', 'Fresh baked classic sugar cookie', 1.89, 0.40, 'Cookies'),
 (@sub_treats, 'Peanut Butter Cookie', 'Fresh baked peanut butter cookie', 1.99, 0.45, 'Cookies'),
 (@sub_treats, 'Double Chocolate Cookie', 'Fresh baked double chocolate cookie', 2.09, 0.50, 'Cookies'),
 (@sub_treats, 'Snickerdoodle', 'Fresh baked cinnamon sugar cookie', 1.99, 0.45, 'Cookies'),
 (@sub_treats, 'Cheese Danish', 'Flaky pastry filled with sweet cheese', 2.79, 0.70, 'Pastries'),
 (@sub_treats, 'Cherry Danish', 'Flaky pastry filled with cherry filling', 2.79, 0.70, 'Pastries'),
 (@sub_treats, 'Croissant', 'Classic buttery flaky croissant', 2.49, 0.60, 'Pastries'),
 (@sub_treats, 'Chocolate Croissant', 'Buttery flaky croissant filled with chocolate', 2.99, 0.75, 'Pastries'),
 (@sub_treats, 'Cinnamon Roll', 'Soft roll swirled with cinnamon and cream cheese icing', 3.29, 0.85, 'Pastries'),
 (@sub_treats, 'Cake Pop', 'Bite-sized cake on a stick, dipped in frosting', 1.99, 0.45, 'Pastries'),
 (@sub_treats, 'Brownie', 'Fudgy chocolate brownie square', 2.49, 0.55, 'Cakes & Bars'),
 (@sub_treats, 'Blondie', 'Chewy vanilla blondie square', 2.49, 0.55, 'Cakes & Bars'),
 (@sub_treats, 'Lemon Bar', 'Tangy lemon bar with a shortbread crust', 2.59, 0.60, 'Cakes & Bars'),
 (@sub_treats, 'Crumb Cake', 'Slice of cake with a buttery crumb topping', 2.99, 0.70, 'Cakes & Bars'),
 (@sub_treats, 'Pound Cake Slice', 'Slice of classic buttery pound cake', 2.79, 0.65, 'Cakes & Bars');

-- ---- Grab & Go ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_grabgo, 'Potato Chips', 'Single-serve bag of potato chips', 1.79, 0.55, 'Snacks'),
 (@sub_grabgo, 'Pretzel Bag', 'Single-serve bag of pretzels', 1.79, 0.55, 'Snacks'),
 (@sub_grabgo, 'Trail Mix', 'Single-serve bag of trail mix', 2.49, 0.80, 'Snacks'),
 (@sub_grabgo, 'Popcorn Bag', 'Single-serve bag of popcorn', 1.99, 0.60, 'Snacks'),
 (@sub_grabgo, 'Cheese & Cracker Pack', 'Grab-and-go cheese and cracker snack pack', 2.99, 0.90, 'Snacks'),
 (@sub_grabgo, 'Fruit Cup', 'Grab-and-go cup of fresh mixed fruit', 3.49, 1.10, 'Fresh & Healthy'),
 (@sub_grabgo, 'Yogurt Parfait', 'Layered yogurt, granola and berries', 3.99, 1.30, 'Fresh & Healthy'),
 (@sub_grabgo, 'Granola Bar', 'Grab-and-go granola bar', 1.49, 0.40, 'Fresh & Healthy'),
 (@sub_grabgo, 'String Cheese', 'Grab-and-go mozzarella string cheese', 1.49, 0.45, 'Fresh & Healthy'),
 (@sub_grabgo, 'Hard-Boiled Egg Pack', 'Two grab-and-go hard-boiled eggs', 2.49, 0.75, 'Fresh & Healthy');

-- ---- Beverages: Coffee ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_coffee, 'Original Coffee', 'Classic drip coffee', 2.75, 0.60, 'Blend'),
 (@sub_coffee, 'Decaf Coffee', 'Decaffeinated drip coffee', 2.75, 0.60, 'Blend'),
 (@sub_coffee, 'Half-Caff Coffee', 'Half regular, half decaf', 2.75, 0.60, 'Blend'),
 (@sub_coffee, 'Midnight Dark Roast', 'Extra bold dark roast blend', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Barista Mix Blend', 'House signature mixed roast', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Cold Brew', 'Slow-steeped cold brew coffee', 3.75, 0.90, 'Cold Brew'),
 (@sub_coffee, 'Nitro Cold Brew', 'Cold brew infused with nitrogen for a creamy pour', 4.25, 1.00, 'Cold Brew'),
 (@sub_coffee, 'Cold Foam Cold Brew', 'Cold brew topped with sweet cold foam', 4.50, 1.10, 'Cold Brew');

-- ---- Beverages: Espresso ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_espresso, 'Espresso', 'Single shot espresso', 2.50, 0.60, 'Single Espresso'),
 (@sub_espresso, 'Latte', 'Espresso with steamed milk', 4.00, 1.20, 'Single Espresso'),
 (@sub_espresso, 'Cappuccino', 'Espresso with steamed milk and foam', 4.00, 1.20, 'Single Espresso'),
 (@sub_espresso, 'Cafe Au Lait', 'Coffee with steamed milk', 3.75, 1.00, 'Single Espresso'),
 (@sub_espresso, 'Americano', 'Espresso with hot water', 3.50, 0.70, 'Double Espresso'),
 (@sub_espresso, 'Macchiato', 'Espresso marked with a dash of foam', 4.25, 1.20, 'Double Espresso'),
 (@sub_espresso, 'Brown Sugar Shaken Espresso', 'Espresso shaken with brown sugar and oat milk', 4.50, 1.30, 'Double Espresso'),
 (@sub_espresso, 'Vanilla Shaken Espresso', 'Espresso shaken with vanilla and milk', 4.50, 1.30, 'Double Espresso'),
 (@sub_espresso, 'Chai Latte', 'Spiced chai with steamed milk', 4.25, 1.10, 'Alternative Beverages'),
 (@sub_espresso, 'Matcha Latte', 'Matcha green tea with steamed milk', 4.75, 1.50, 'Alternative Beverages'),
 (@sub_espresso, 'Cafe Con Leche', 'Espresso with warm sweetened milk', 4.00, 1.10, 'Alternative Beverages'),
 (@sub_espresso, 'Cortado', 'Espresso cut with a small amount of steamed milk', 3.75, 1.00, 'Alternative Beverages');

-- ---- Beverages: Teas ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_teas, 'Iced Tea', 'Freshly brewed iced tea', 2.75, 0.50, 'Iced Tea'),
 (@sub_teas, 'Iced Tea with Lemonade', 'Half iced tea, half lemonade', 3.00, 0.60, 'Iced Tea'),
 (@sub_teas, 'Sweetened Iced Tea', 'Freshly brewed iced tea, sweetened', 2.75, 0.50, 'Iced Tea'),
 (@sub_teas, 'Sweetened Green Tea', 'Iced green tea, sweetened', 2.95, 0.55, 'Iced Tea'),
 (@sub_teas, 'Bold Breakfast Tea', 'Classic black breakfast tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Harmony Green Tea', 'Smooth green tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Cool Mint Tea', 'Refreshing mint herbal tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Chamomile Fields Tea', 'Calming chamomile herbal tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Hibiscus Kiss Tea', 'Tart hibiscus herbal tea', 2.95, 0.55, 'Hot Tea'),
 (@sub_teas, 'Decaf Tea', 'Decaffeinated black tea', 2.75, 0.50, 'Hot Tea');

-- ---- Beverages: Refreshers ----
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

-- ---- Beverages: Frozen ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_frozen, 'Blue Raspberry Cooler', 'Blended blue raspberry frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Cosmic Cooler', 'Blended mixed-berry frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Strawberry Cooler', 'Blended strawberry frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Vanilla Bean Cooler', 'Blended vanilla bean frozen drink', 4.25, 1.00, 'Frozen Coolers'),
 (@sub_frozen, 'Frozen Coffee', 'Blended frozen coffee drink', 4.75, 1.30, 'Frozen Coffee'),
 (@sub_frozen, 'Caramel Cream Frozen Coffee', 'Blended frozen coffee with caramel', 4.75, 1.30, 'Frozen Coffee'),
 (@sub_frozen, 'Triple Mocha Frozen Coffee', 'Blended frozen coffee with extra mocha', 4.95, 1.35, 'Frozen Coffee'),
 (@sub_frozen, 'Frozen Lemonade', 'Blended frozen lemonade', 3.75, 0.80, 'Other Frozen'),
 (@sub_frozen, 'Frozen Chai Latte', 'Blended frozen chai latte', 4.75, 1.30, 'Other Frozen'),
 (@sub_frozen, 'Frozen Matcha Latte', 'Blended frozen matcha latte', 4.95, 1.40, 'Other Frozen');

-- ---- Beverages: Other Beverages ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_other_bev, 'Fountain Soda Small', 'Small fountain soda', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Fountain Soda Medium', 'Medium fountain soda', 2.75, 0.35, 'Coolers & Fountain'),
 (@sub_other_bev, 'Fountain Soda Large', 'Large fountain soda', 3.25, 0.40, 'Coolers & Fountain'),
 (@sub_other_bev, 'Blackberry Tangerine Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Blush Pop Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Tropical Mango Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Box of Coffee (12 cups)', 'Coffee box for meetings and events', 22.99, 9.00, 'Bulk'),
 (@sub_other_bev, 'Gallon of Iced Tea', 'Bulk iced tea for events', 14.99, 5.00, 'Bulk');

-- ---- Beverages: expanded to 50+ items per subcategory ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_coffee, 'French Roast Coffee', 'Bold dark French roast', 2.85, 0.60, 'Blend'),
 (@sub_coffee, 'Breakfast Blend Coffee', 'Smooth light morning blend', 2.75, 0.60, 'Blend'),
 (@sub_coffee, 'Hazelnut Blend Coffee', 'Coffee with hazelnut notes', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Vanilla Blend Coffee', 'Coffee with vanilla notes', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Caramel Blend Coffee', 'Coffee with caramel notes', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Mocha Blend Coffee', 'Coffee with chocolate notes', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Pumpkin Blend Coffee', 'Seasonal pumpkin spice blend', 3.05, 0.70, 'Blend'),
 (@sub_coffee, 'Peppermint Blend Coffee', 'Seasonal peppermint blend', 3.05, 0.70, 'Blend'),
 (@sub_coffee, 'Toasted Almond Blend Coffee', 'Coffee with toasted almond notes', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Coconut Blend Coffee', 'Coffee with coconut notes', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Irish Cream Blend Coffee', 'Coffee with Irish cream notes', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Cinnamon Spice Blend Coffee', 'Coffee with warm cinnamon spice', 2.95, 0.65, 'Blend'),
 (@sub_coffee, 'Vanilla Cold Brew', 'Cold brew with vanilla', 4.50, 1.10, 'Cold Brew'),
 (@sub_coffee, 'Caramel Cold Brew', 'Cold brew with caramel', 4.50, 1.10, 'Cold Brew'),
 (@sub_coffee, 'Mocha Cold Brew', 'Cold brew with chocolate', 4.50, 1.10, 'Cold Brew'),
 (@sub_coffee, 'Sweet Cream Cold Brew', 'Cold brew with sweet cream', 4.50, 1.10, 'Cold Brew'),
 (@sub_coffee, 'Toasted Coconut Cold Brew', 'Cold brew with toasted coconut', 4.65, 1.15, 'Cold Brew'),
 (@sub_coffee, 'Salted Caramel Cold Brew', 'Cold brew with salted caramel', 4.65, 1.15, 'Cold Brew'),
 (@sub_coffee, 'Iced Coffee Original', 'Classic iced coffee', 3.25, 0.75, 'Iced Coffee'),
 (@sub_coffee, 'Iced Coffee Vanilla', 'Iced coffee with vanilla', 3.45, 0.80, 'Iced Coffee'),
 (@sub_coffee, 'Iced Coffee Caramel', 'Iced coffee with caramel', 3.45, 0.80, 'Iced Coffee'),
 (@sub_coffee, 'Iced Coffee Mocha', 'Iced coffee with chocolate', 3.45, 0.80, 'Iced Coffee'),
 (@sub_coffee, 'Iced Coffee Hazelnut', 'Iced coffee with hazelnut', 3.45, 0.80, 'Iced Coffee'),
 (@sub_coffee, 'Iced Coffee French Vanilla', 'Iced coffee with French vanilla', 3.55, 0.85, 'Iced Coffee'),
 (@sub_coffee, 'Iced Coffee Toasted Almond', 'Iced coffee with toasted almond', 3.55, 0.85, 'Iced Coffee'),
 (@sub_coffee, 'Iced Coffee Cinnamon', 'Iced coffee with cinnamon', 3.45, 0.80, 'Iced Coffee'),
 (@sub_coffee, 'Vanilla Hot Coffee', 'Hot coffee with vanilla', 2.95, 0.65, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Caramel Hot Coffee', 'Hot coffee with caramel', 2.95, 0.65, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Hazelnut Hot Coffee', 'Hot coffee with hazelnut', 2.95, 0.65, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Mocha Hot Coffee', 'Hot coffee with chocolate', 2.95, 0.65, 'Flavored Hot Coffee'),
 (@sub_coffee, 'French Vanilla Hot Coffee', 'Hot coffee with French vanilla', 3.05, 0.70, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Butter Pecan Hot Coffee', 'Hot coffee with butter pecan', 3.05, 0.70, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Toasted Marshmallow Hot Coffee', 'Hot coffee with toasted marshmallow', 3.05, 0.70, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Pumpkin Spice Hot Coffee', 'Seasonal pumpkin spice hot coffee', 3.15, 0.75, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Peppermint Mocha Hot Coffee', 'Seasonal peppermint mocha hot coffee', 3.15, 0.75, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Cinnamon Dolce Hot Coffee', 'Hot coffee with cinnamon dolce', 3.05, 0.70, 'Flavored Hot Coffee'),
 (@sub_coffee, 'Decaf Vanilla Coffee', 'Decaf coffee with vanilla', 2.85, 0.60, 'Decaf & Light Roast'),
 (@sub_coffee, 'Decaf Caramel Coffee', 'Decaf coffee with caramel', 2.85, 0.60, 'Decaf & Light Roast'),
 (@sub_coffee, 'Decaf Hazelnut Coffee', 'Decaf coffee with hazelnut', 2.85, 0.60, 'Decaf & Light Roast'),
 (@sub_coffee, 'Light Roast Original', 'Smooth light roast coffee', 2.75, 0.60, 'Decaf & Light Roast'),
 (@sub_coffee, 'Light Roast Breakfast Blend', 'Light roast breakfast blend', 2.75, 0.60, 'Decaf & Light Roast'),
 (@sub_coffee, 'Light Roast Hazelnut', 'Light roast with hazelnut', 2.85, 0.60, 'Decaf & Light Roast'),
 (@sub_coffee, 'Light Roast Vanilla', 'Light roast with vanilla', 2.85, 0.60, 'Decaf & Light Roast'),
 (@sub_coffee, 'Light Roast Caramel', 'Light roast with caramel', 2.85, 0.60, 'Decaf & Light Roast'),
 (@sub_espresso, 'Flat White', 'Espresso with steamed micro-foam milk', 4.25, 1.20, 'Single Espresso'),
 (@sub_espresso, 'Espresso Con Panna', 'Espresso topped with whipped cream', 3.75, 1.00, 'Single Espresso'),
 (@sub_espresso, 'Ristretto', 'Short concentrated espresso shot', 2.75, 0.65, 'Single Espresso'),
 (@sub_espresso, 'Ristretto Latte', 'Ristretto with steamed milk', 4.25, 1.20, 'Single Espresso'),
 (@sub_espresso, 'Vanilla Latte', 'Latte with vanilla', 4.50, 1.30, 'Single Espresso'),
 (@sub_espresso, 'Caramel Latte', 'Latte with caramel', 4.50, 1.30, 'Single Espresso'),
 (@sub_espresso, 'Hazelnut Latte', 'Latte with hazelnut', 4.50, 1.30, 'Single Espresso'),
 (@sub_espresso, 'Mocha Latte', 'Latte with chocolate', 4.65, 1.35, 'Single Espresso'),
 (@sub_espresso, 'White Mocha Latte', 'Latte with white chocolate', 4.75, 1.40, 'Single Espresso'),
 (@sub_espresso, 'Pumpkin Spice Latte', 'Seasonal pumpkin spice latte', 4.85, 1.45, 'Single Espresso'),
 (@sub_espresso, 'Peppermint Mocha Latte', 'Seasonal peppermint mocha latte', 4.85, 1.45, 'Single Espresso'),
 (@sub_espresso, 'Cinnamon Dolce Latte', 'Latte with cinnamon dolce', 4.65, 1.35, 'Single Espresso'),
 (@sub_espresso, 'Doppio', 'Double shot of espresso', 3.25, 0.75, 'Double Espresso'),
 (@sub_espresso, 'Caramel Espresso Macchiato', 'Espresso marked with caramel foam', 4.50, 1.25, 'Double Espresso'),
 (@sub_espresso, 'Iced Americano', 'Espresso with cold water', 3.75, 0.80, 'Double Espresso'),
 (@sub_espresso, 'Vanilla Iced Americano', 'Iced americano with vanilla', 3.95, 0.90, 'Double Espresso'),
 (@sub_espresso, 'Caramel Iced Americano', 'Iced americano with caramel', 3.95, 0.90, 'Double Espresso'),
 (@sub_espresso, 'Brown Sugar Oat Latte', 'Espresso with oat milk and brown sugar', 4.75, 1.35, 'Double Espresso'),
 (@sub_espresso, 'Honey Almondmilk Latte', 'Espresso with almond milk and honey', 4.75, 1.35, 'Double Espresso'),
 (@sub_espresso, 'Toasted Vanilla Shaken Espresso', 'Shaken espresso with toasted vanilla', 4.65, 1.30, 'Double Espresso'),
 (@sub_espresso, 'Iced Chai Latte', 'Chai latte served iced', 4.50, 1.15, 'Alternative Beverages'),
 (@sub_espresso, 'Dirty Chai Latte', 'Chai latte with a shot of espresso', 4.75, 1.30, 'Alternative Beverages'),
 (@sub_espresso, 'London Fog Latte', 'Earl grey tea latte with vanilla', 4.25, 1.10, 'Alternative Beverages'),
 (@sub_espresso, 'Golden Turmeric Latte', 'Turmeric spiced milk latte', 4.50, 1.20, 'Alternative Beverages'),
 (@sub_espresso, 'Iced Matcha Green Tea Latte', 'Matcha latte served iced', 4.75, 1.50, 'Alternative Beverages'),
 (@sub_espresso, 'Horchata Latte', 'Espresso with cinnamon horchata milk', 4.75, 1.30, 'Alternative Beverages'),
 (@sub_espresso, 'Cafe De Olla', 'Spiced cinnamon coffee', 3.95, 0.90, 'Alternative Beverages'),
 (@sub_espresso, 'Cubano', 'Sweetened espresso shot', 3.25, 0.75, 'Alternative Beverages'),
 (@sub_espresso, 'Vietnamese Style Iced Coffee', 'Iced coffee with sweet condensed milk', 4.25, 1.00, 'Alternative Beverages'),
 (@sub_espresso, 'Affogato', 'Espresso poured over ice cream', 4.95, 1.50, 'Alternative Beverages'),
 (@sub_espresso, 'Pumpkin Cream Cold Brew Latte', 'Cold brew topped with pumpkin cream', 5.25, 1.50, 'Seasonal Espresso'),
 (@sub_espresso, 'Peppermint Bark Latte', 'Latte with peppermint bark', 4.95, 1.45, 'Seasonal Espresso'),
 (@sub_espresso, 'Gingerbread Latte', 'Latte with gingerbread spice', 4.85, 1.40, 'Seasonal Espresso'),
 (@sub_espresso, 'Eggnog Latte', 'Latte with eggnog', 4.95, 1.45, 'Seasonal Espresso'),
 (@sub_espresso, 'Maple Pecan Latte', 'Latte with maple and pecan', 4.85, 1.40, 'Seasonal Espresso'),
 (@sub_espresso, 'Toasted Coconut Latte', 'Latte with toasted coconut', 4.75, 1.35, 'Seasonal Espresso'),
 (@sub_espresso, 'Irish Cream Latte', 'Latte with Irish cream', 4.75, 1.35, 'Seasonal Espresso'),
 (@sub_espresso, 'Butterscotch Latte', 'Latte with butterscotch', 4.75, 1.35, 'Seasonal Espresso'),
 (@sub_espresso, 'Caramel Apple Latte', 'Latte with caramel apple', 4.85, 1.40, 'Seasonal Espresso'),
 (@sub_espresso, 'S''mores Latte', 'Latte with chocolate and toasted marshmallow', 4.95, 1.45, 'Seasonal Espresso'),
 (@sub_teas, 'Unsweetened Iced Tea', 'Freshly brewed iced tea, unsweetened', 2.75, 0.50, 'Iced Tea'),
 (@sub_teas, 'Peach Iced Tea', 'Iced tea with peach', 2.95, 0.55, 'Iced Tea'),
 (@sub_teas, 'Raspberry Iced Tea', 'Iced tea with raspberry', 2.95, 0.55, 'Iced Tea'),
 (@sub_teas, 'Mango Iced Tea', 'Iced tea with mango', 2.95, 0.55, 'Iced Tea'),
 (@sub_teas, 'Lemon Iced Tea', 'Iced tea with lemon', 2.85, 0.55, 'Iced Tea'),
 (@sub_teas, 'Mint Iced Tea', 'Iced tea with mint', 2.85, 0.55, 'Iced Tea'),
 (@sub_teas, 'Large Half & Half', 'Large iced tea and lemonade mix', 3.25, 0.65, 'Iced Tea'),
 (@sub_teas, 'Black Iced Tea', 'Classic black iced tea', 2.75, 0.50, 'Iced Tea'),
 (@sub_teas, 'Unsweetened Green Iced Tea', 'Iced green tea, unsweetened', 2.85, 0.50, 'Iced Tea'),
 (@sub_teas, 'Passionfruit Iced Tea', 'Iced tea with passionfruit', 2.95, 0.55, 'Iced Tea'),
 (@sub_teas, 'Earl Grey Tea', 'Classic bergamot black tea', 2.85, 0.50, 'Hot Tea'),
 (@sub_teas, 'English Breakfast Tea', 'Full-bodied black breakfast tea', 2.75, 0.50, 'Hot Tea'),
 (@sub_teas, 'Jasmine Green Tea', 'Green tea with jasmine', 2.85, 0.50, 'Hot Tea'),
 (@sub_teas, 'Ginger Spice Tea', 'Herbal tea with ginger', 2.85, 0.50, 'Hot Tea'),
 (@sub_teas, 'Lemon Ginger Tea', 'Herbal tea with lemon and ginger', 2.85, 0.50, 'Hot Tea'),
 (@sub_teas, 'Vanilla Rooibos Tea', 'Caffeine-free rooibos with vanilla', 2.95, 0.55, 'Hot Tea'),
 (@sub_teas, 'Turmeric Wellness Tea', 'Herbal tea with turmeric', 2.95, 0.55, 'Hot Tea'),
 (@sub_teas, 'Lavender Dreams Tea', 'Herbal tea with lavender', 2.95, 0.55, 'Hot Tea'),
 (@sub_teas, 'Cinnamon Apple Tea', 'Herbal tea with cinnamon apple', 2.85, 0.50, 'Hot Tea'),
 (@sub_teas, 'Blackberry Sage Tea', 'Herbal tea with blackberry sage', 2.95, 0.55, 'Hot Tea'),
 (@sub_teas, 'Orange Spice Tea', 'Black tea with orange spice', 2.85, 0.50, 'Hot Tea'),
 (@sub_teas, 'White Peony Tea', 'Delicate white tea', 3.05, 0.60, 'Hot Tea'),
 (@sub_teas, 'Hot Chai Tea', 'Spiced chai tea, hot', 3.25, 0.75, 'Chai & Matcha'),
 (@sub_teas, 'Iced Chai Tea', 'Spiced chai tea, iced', 3.25, 0.75, 'Chai & Matcha'),
 (@sub_teas, 'Vanilla Chai Tea', 'Chai tea with vanilla', 3.45, 0.85, 'Chai & Matcha'),
 (@sub_teas, 'Spiced Chai Tea', 'Extra spiced chai tea', 3.45, 0.85, 'Chai & Matcha'),
 (@sub_teas, 'Hot Matcha Green Tea', 'Whisked matcha green tea, hot', 3.95, 1.00, 'Chai & Matcha'),
 (@sub_teas, 'Iced Matcha Green Tea', 'Whisked matcha green tea, iced', 4.15, 1.05, 'Chai & Matcha'),
 (@sub_teas, 'Honey Matcha Tea', 'Matcha tea with honey', 4.25, 1.10, 'Chai & Matcha'),
 (@sub_teas, 'Coconut Matcha Tea', 'Matcha tea with coconut milk', 4.25, 1.10, 'Chai & Matcha'),
 (@sub_teas, 'Iced Milk Tea', 'Classic milk tea, iced', 4.25, 1.10, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Taro Milk Tea', 'Milk tea with taro', 4.50, 1.20, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Thai Milk Tea', 'Spiced Thai-style milk tea', 4.50, 1.20, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Honey Lemon Tea', 'Black tea with honey and lemon', 3.25, 0.65, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Peach Green Tea', 'Green tea with peach', 3.25, 0.65, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Strawberry Green Tea', 'Green tea with strawberry', 3.25, 0.65, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Mango Black Tea', 'Black tea with mango', 3.25, 0.65, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Lychee Tea', 'Black tea with lychee', 3.25, 0.65, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Watermelon Tea', 'Iced tea with watermelon', 3.25, 0.65, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Blueberry Tea', 'Iced tea with blueberry', 3.25, 0.65, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Vanilla Milk Tea', 'Milk tea with vanilla', 4.25, 1.10, 'Bubble & Specialty Tea'),
 (@sub_teas, 'Coconut Milk Tea', 'Milk tea with coconut', 4.25, 1.10, 'Bubble & Specialty Tea'),
 (@sub_refreshers, 'Peach Citrus Refresher', 'Fruit-flavored refresher with peach citrus', 3.75, 0.90, 'Classic'),
 (@sub_refreshers, 'Watermelon Refresher', 'Watermelon-flavored refresher', 3.75, 0.90, 'Classic'),
 (@sub_refreshers, 'Blueberry Pomegranate Refresher', 'Fruit-flavored refresher with blueberry pomegranate', 3.85, 0.95, 'Classic'),
 (@sub_refreshers, 'Kiwi Berry Refresher', 'Fruit-flavored refresher with kiwi berry', 3.85, 0.95, 'Classic'),
 (@sub_refreshers, 'Passionfruit Refresher', 'Passionfruit-flavored refresher', 3.85, 0.95, 'Classic'),
 (@sub_refreshers, 'Raspberry Lemonade Refresher', 'Refresher with raspberry lemonade', 3.85, 0.95, 'Classic'),
 (@sub_refreshers, 'Green Apple Refresher', 'Green apple-flavored refresher', 3.75, 0.90, 'Classic'),
 (@sub_refreshers, 'Lychee Rose Refresher', 'Fruit-flavored refresher with lychee rose', 3.95, 1.00, 'Classic'),
 (@sub_refreshers, 'Peach Daydream Refresher', 'Creamy peach refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Watermelon Daydream Refresher', 'Creamy watermelon refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Blueberry Daydream Refresher', 'Creamy blueberry refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Kiwi Daydream Refresher', 'Creamy kiwi refresher', 4.25, 1.10, 'Daydream'),
 (@sub_refreshers, 'Passionfruit Daydream Refresher', 'Creamy passionfruit refresher', 4.35, 1.15, 'Daydream'),
 (@sub_refreshers, 'Coconut Daydream Refresher', 'Creamy coconut refresher', 4.35, 1.15, 'Daydream'),
 (@sub_refreshers, 'Lychee Daydream Refresher', 'Creamy lychee refresher', 4.35, 1.15, 'Daydream'),
 (@sub_refreshers, 'Guava Daydream Refresher', 'Creamy guava refresher', 4.35, 1.15, 'Daydream'),
 (@sub_refreshers, 'Citrus Energy Refresher', 'Energizing citrus refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Berry Energy Refresher', 'Energizing mixed berry refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Tropical Energy Refresher', 'Energizing tropical refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Mint Energy Refresher', 'Energizing mint refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Ginger Energy Refresher', 'Energizing ginger refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Peach Energy Refresher', 'Energizing peach refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Mango Energy Refresher', 'Energizing mango refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Pineapple Energy Refresher', 'Energizing pineapple refresher', 4.25, 1.05, 'Energy Refreshers'),
 (@sub_refreshers, 'Strawberry Splash', 'Sparkling strawberry fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Mango Splash', 'Sparkling mango fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Dragonfruit Splash', 'Sparkling dragonfruit fruit splash', 4.05, 1.00, 'Fruit Splash'),
 (@sub_refreshers, 'Kiwi Splash', 'Sparkling kiwi fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Watermelon Splash', 'Sparkling watermelon fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Blueberry Splash', 'Sparkling blueberry fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Passionfruit Splash', 'Sparkling passionfruit fruit splash', 4.05, 1.00, 'Fruit Splash'),
 (@sub_refreshers, 'Peach Splash', 'Sparkling peach fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Cherry Splash', 'Sparkling cherry fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Pineapple Splash', 'Sparkling pineapple fruit splash', 3.95, 0.95, 'Fruit Splash'),
 (@sub_refreshers, 'Half & Half Refresher', 'Two refresher flavors combined', 3.85, 0.95, 'Mix It'),
 (@sub_refreshers, 'Double Berry Mix Refresher', 'Mixed berry refresher combo', 3.95, 1.00, 'Mix It'),
 (@sub_refreshers, 'Tropical Mix Refresher', 'Mixed tropical refresher combo', 3.95, 1.00, 'Mix It'),
 (@sub_refreshers, 'Citrus Mix Refresher', 'Mixed citrus refresher combo', 3.85, 0.95, 'Mix It'),
 (@sub_refreshers, 'Build Your Own Zero Sugar Refresher', 'Choose a zero-sugar flavor combination', 3.85, 0.95, 'Mix It'),
 (@sub_refreshers, 'Sparkling Strawberry Refresher', 'Lightly sparkling strawberry refresher', 4.15, 1.05, 'Sparkling Refreshers'),
 (@sub_refreshers, 'Sparkling Peach Refresher', 'Lightly sparkling peach refresher', 4.15, 1.05, 'Sparkling Refreshers'),
 (@sub_refreshers, 'Sparkling Citrus Refresher', 'Lightly sparkling citrus refresher', 4.15, 1.05, 'Sparkling Refreshers'),
 (@sub_refreshers, 'Sparkling Berry Refresher', 'Lightly sparkling mixed berry refresher', 4.15, 1.05, 'Sparkling Refreshers'),
 (@sub_refreshers, 'Sparkling Mango Refresher', 'Lightly sparkling mango refresher', 4.15, 1.05, 'Sparkling Refreshers'),
 (@sub_refreshers, 'Sparkling Watermelon Refresher', 'Lightly sparkling watermelon refresher', 4.15, 1.05, 'Sparkling Refreshers'),
 (@sub_frozen, 'Watermelon Cooler', 'Blended watermelon frozen drink', 4.35, 1.05, 'Frozen Coolers'),
 (@sub_frozen, 'Mango Cooler', 'Blended mango frozen drink', 4.35, 1.05, 'Frozen Coolers'),
 (@sub_frozen, 'Peach Cooler', 'Blended peach frozen drink', 4.35, 1.05, 'Frozen Coolers'),
 (@sub_frozen, 'Green Apple Cooler', 'Blended green apple frozen drink', 4.35, 1.05, 'Frozen Coolers'),
 (@sub_frozen, 'Pina Colada Cooler', 'Blended pineapple coconut frozen drink', 4.45, 1.10, 'Frozen Coolers'),
 (@sub_frozen, 'Kiwi Cooler', 'Blended kiwi frozen drink', 4.35, 1.05, 'Frozen Coolers'),
 (@sub_frozen, 'Passionfruit Cooler', 'Blended passionfruit frozen drink', 4.45, 1.10, 'Frozen Coolers'),
 (@sub_frozen, 'Sour Cherry Cooler', 'Blended sour cherry frozen drink', 4.35, 1.05, 'Frozen Coolers'),
 (@sub_frozen, 'Vanilla Bean Frozen Coffee', 'Blended frozen coffee with vanilla bean', 4.85, 1.35, 'Frozen Coffee'),
 (@sub_frozen, 'Hazelnut Frozen Coffee', 'Blended frozen coffee with hazelnut', 4.85, 1.35, 'Frozen Coffee'),
 (@sub_frozen, 'Cookies and Cream Frozen Coffee', 'Blended frozen coffee with cookies and cream', 4.95, 1.40, 'Frozen Coffee'),
 (@sub_frozen, 'White Chocolate Frozen Coffee', 'Blended frozen coffee with white chocolate', 4.95, 1.40, 'Frozen Coffee'),
 (@sub_frozen, 'Toasted Coconut Frozen Coffee', 'Blended frozen coffee with toasted coconut', 4.85, 1.35, 'Frozen Coffee'),
 (@sub_frozen, 'Salted Caramel Frozen Coffee', 'Blended frozen coffee with salted caramel', 4.95, 1.40, 'Frozen Coffee'),
 (@sub_frozen, 'Butterscotch Frozen Coffee', 'Blended frozen coffee with butterscotch', 4.85, 1.35, 'Frozen Coffee'),
 (@sub_frozen, 'Cinnamon Frozen Coffee', 'Blended frozen coffee with cinnamon', 4.85, 1.35, 'Frozen Coffee'),
 (@sub_frozen, 'Frozen Hot Chocolate', 'Blended frozen hot chocolate', 4.75, 1.30, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen White Hot Chocolate', 'Blended frozen white hot chocolate', 4.85, 1.35, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen Caramel Macchiato', 'Blended frozen caramel macchiato', 4.95, 1.40, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen Mocha', 'Blended frozen mocha', 4.85, 1.35, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen Vanilla Latte', 'Blended frozen vanilla latte', 4.85, 1.35, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen Honey Latte', 'Blended frozen honey latte', 4.85, 1.35, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen Toffee Latte', 'Blended frozen toffee latte', 4.95, 1.40, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen Cinnamon Latte', 'Blended frozen cinnamon latte', 4.85, 1.35, 'Frozen Specialty'),
 (@sub_frozen, 'Frozen Strawberry Lemonade', 'Blended frozen strawberry lemonade', 4.25, 1.00, 'Other Frozen'),
 (@sub_frozen, 'Frozen Mango Lemonade', 'Blended frozen mango lemonade', 4.25, 1.00, 'Other Frozen'),
 (@sub_frozen, 'Frozen Peach Tea', 'Blended frozen peach tea', 4.15, 0.95, 'Other Frozen'),
 (@sub_frozen, 'Frozen Hibiscus Tea', 'Blended frozen hibiscus tea', 4.15, 0.95, 'Other Frozen'),
 (@sub_frozen, 'Frozen Green Tea', 'Blended frozen green tea', 4.15, 0.95, 'Other Frozen'),
 (@sub_frozen, 'Frozen Berry Hibiscus', 'Blended frozen berry hibiscus tea', 4.25, 1.00, 'Other Frozen'),
 (@sub_frozen, 'Blueberry Frozen Blend', 'Blended blueberry frozen drink', 4.45, 1.10, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Raspberry Frozen Blend', 'Blended raspberry frozen drink', 4.45, 1.10, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Blackberry Frozen Blend', 'Blended blackberry frozen drink', 4.45, 1.10, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Tropical Frozen Blend', 'Blended tropical fruit frozen drink', 4.45, 1.10, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Citrus Frozen Blend', 'Blended citrus frozen drink', 4.35, 1.05, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Melon Frozen Blend', 'Blended melon frozen drink', 4.35, 1.05, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Grape Frozen Blend', 'Blended grape frozen drink', 4.35, 1.05, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Pomegranate Frozen Blend', 'Blended pomegranate frozen drink', 4.45, 1.10, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Apple Cinnamon Frozen Blend', 'Blended apple cinnamon frozen drink', 4.35, 1.05, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Coconut Cream Frozen Blend', 'Blended coconut cream frozen drink', 4.45, 1.10, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Banana Frozen Blend', 'Blended banana frozen drink', 4.35, 1.05, 'Frozen Fruit Blends'),
 (@sub_frozen, 'Guava Frozen Blend', 'Blended guava frozen drink', 4.45, 1.10, 'Frozen Fruit Blends'),
 (@sub_other_bev, 'Cola', 'Classic cola fountain drink', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Diet Cola', 'Diet cola fountain drink', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Lemon Lime Soda', 'Lemon lime fountain soda', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Root Beer', 'Root beer fountain drink', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Orange Soda', 'Orange fountain soda', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Ginger Ale', 'Ginger ale fountain drink', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Fruit Punch Soda', 'Fruit punch fountain drink', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Grape Soda', 'Grape fountain soda', 2.25, 0.30, 'Coolers & Fountain'),
 (@sub_other_bev, 'Citrus Splash Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Berry Burst Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Peach Fizz Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Watermelon Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Mixed Berry Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Lime Mint Zero', 'Zero-sugar sparkling water', 2.95, 0.60, 'Zero Sugar'),
 (@sub_other_bev, 'Box of Tea (12 cups)', 'Tea box for meetings and events', 21.99, 8.50, 'Bulk'),
 (@sub_other_bev, 'Gallon of Lemonade', 'Bulk lemonade for events', 14.99, 5.00, 'Bulk'),
 (@sub_other_bev, 'Gallon of Refresher', 'Bulk refresher for events', 17.99, 6.50, 'Bulk'),
 (@sub_other_bev, 'Party Box of Coffee (24 cups)', 'Large coffee box for big events', 39.99, 16.00, 'Bulk'),
 (@sub_other_bev, 'Airpot of Coffee', 'Insulated airpot of fresh coffee', 26.99, 10.00, 'Bulk'),
 (@sub_other_bev, 'Box of Hot Chocolate (12 cups)', 'Hot chocolate box for events', 23.99, 9.50, 'Bulk'),
 (@sub_other_bev, 'Bottled Water', 'Single-serve bottled water', 1.99, 0.40, 'Bottled & Juices'),
 (@sub_other_bev, 'Sparkling Water', 'Single-serve sparkling water', 2.49, 0.50, 'Bottled & Juices'),
 (@sub_other_bev, 'Orange Juice', 'Bottled orange juice', 2.99, 0.90, 'Bottled & Juices'),
 (@sub_other_bev, 'Apple Juice', 'Bottled apple juice', 2.99, 0.90, 'Bottled & Juices'),
 (@sub_other_bev, 'Cranberry Juice', 'Bottled cranberry juice', 2.99, 0.90, 'Bottled & Juices'),
 (@sub_other_bev, 'Grapefruit Juice', 'Bottled grapefruit juice', 2.99, 0.90, 'Bottled & Juices'),
 (@sub_other_bev, 'Pineapple Juice', 'Bottled pineapple juice', 2.99, 0.90, 'Bottled & Juices'),
 (@sub_other_bev, 'Tomato Juice', 'Bottled tomato juice', 2.99, 0.90, 'Bottled & Juices'),
 (@sub_other_bev, 'Bottled Lemonade', 'Bottled fresh lemonade', 3.25, 1.00, 'Bottled & Juices'),
 (@sub_other_bev, 'Fruit Punch Juice', 'Bottled fruit punch juice', 2.99, 0.90, 'Bottled & Juices'),
 (@sub_other_bev, 'Whole Milk', 'Single-serve whole milk', 1.99, 0.50, 'Milk & Extras'),
 (@sub_other_bev, 'Chocolate Milk', 'Single-serve chocolate milk', 2.25, 0.55, 'Milk & Extras'),
 (@sub_other_bev, 'Oat Milk Carton', 'Single-serve oat milk', 2.75, 0.90, 'Milk & Extras'),
 (@sub_other_bev, 'Almond Milk Carton', 'Single-serve almond milk', 2.75, 0.90, 'Milk & Extras'),
 (@sub_other_bev, 'Hot Chocolate', 'Rich hot chocolate', 3.25, 0.80, 'Milk & Extras'),
 (@sub_other_bev, 'White Hot Chocolate', 'Rich white hot chocolate', 3.45, 0.90, 'Milk & Extras'),
 (@sub_other_bev, 'Steamed Milk', 'Warm steamed milk', 2.75, 0.60, 'Milk & Extras'),
 (@sub_other_bev, 'Vanilla Steamer', 'Steamed milk with vanilla', 3.25, 0.80, 'Milk & Extras'),
 (@sub_other_bev, 'Caramel Steamer', 'Steamed milk with caramel', 3.25, 0.80, 'Milk & Extras'),
 (@sub_other_bev, 'Honey Steamer', 'Steamed milk with honey', 3.25, 0.80, 'Milk & Extras'),
 (@sub_other_bev, 'Kids Hot Chocolate', 'Small hot chocolate for kids', 2.25, 0.55, 'Kids Drinks'),
 (@sub_other_bev, 'Kids Chocolate Milk', 'Small chocolate milk for kids', 1.99, 0.50, 'Kids Drinks'),
 (@sub_other_bev, 'Kids Apple Juice', 'Small apple juice for kids', 1.75, 0.45, 'Kids Drinks'),
 (@sub_other_bev, 'Kids Lemonade', 'Small lemonade for kids', 1.75, 0.45, 'Kids Drinks'),
 (@sub_other_bev, 'Kids Fruit Punch', 'Small fruit punch for kids', 1.75, 0.45, 'Kids Drinks');

-- ---- Featured department: colorful sub-tabs (New Arrivals, Fan Favorites, Limited Time
-- Offers, Value Bundles). A 5th "News & Promos" sidebar entry is added client-side in
-- POSPanel.java (not DB-backed). All item names are original to this shop. ----
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

INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_bundles, 'Iced Coffee Bundle (5-pack)', 'Five iced coffees to go', 14.99, 4.50, 'Bundles'),
 (@sub_bundles, 'Decaf Iced Coffee Bundle (5-pack)', 'Five decaf iced coffees to go', 14.99, 4.50, 'Bundles'),
 (@sub_bundles, 'Refresher Bundle (4-pack)', 'Four refreshers to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Strawberry Dragonfruit Lemonade Bundle (4-pack)', 'Four strawberry dragonfruit lemonades to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Matcha Bundle (3-pack)', 'Three matcha drinks to go', 13.99, 5.00, 'Bundles'),
 (@sub_bundles, 'Frozen Coffee Bundle (4-pack)', 'Four frozen coffees to go', 18.99, 6.00, 'Bundles');

-- ---- Featured sub-tabs expanded to 50+ items each ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_new, 'Peach Mango Refresher', 'Fruit-flavored refresher with peach and mango', 3.95, 0.95, 'Refreshers'),
 (@sub_new, 'Watermelon Basil Refresher', 'Fruit-flavored refresher with watermelon and basil', 3.95, 0.95, 'Refreshers'),
 (@sub_new, 'Blueberry Lavender Refresher', 'Fruit-flavored refresher with blueberry and lavender', 4.05, 1.00, 'Refreshers'),
 (@sub_new, 'Pineapple Coconut Refresher', 'Fruit-flavored refresher with pineapple and coconut', 4.05, 1.00, 'Refreshers'),
 (@sub_new, 'Raspberry Rose Refresher', 'Fruit-flavored refresher with raspberry and rose', 4.05, 1.00, 'Refreshers'),
 (@sub_new, 'Kiwi Melon Refresher', 'Fruit-flavored refresher with kiwi and melon', 3.95, 0.95, 'Refreshers'),
 (@sub_new, 'Passionfruit Guava Refresher', 'Fruit-flavored refresher with passionfruit and guava', 4.05, 1.00, 'Refreshers'),
 (@sub_new, 'Salted Caramel Cloud Latte', 'Latte with salted caramel and cream', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_new, 'Pistachio Cloud Latte', 'Latte with pistachio and cream', 4.99, 1.45, 'Lattes & Espresso'),
 (@sub_new, 'Honey Cinnamon Latte', 'Latte with honey and cinnamon', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_new, 'Brown Butter Cloud Latte', 'Latte with brown butter and cream', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_new, 'Espresso Con Panna', 'Espresso topped with whipped cream', 3.95, 1.05, 'Lattes & Espresso'),
 (@sub_new, 'Vanilla Bean Shakin'' Espresso', 'Shaken espresso with vanilla bean', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_new, 'Iced Cascara Latte', 'Iced latte with cascara sweetener', 4.75, 1.35, 'Lattes & Espresso'),
 (@sub_new, 'Maple Walnut Cloud Latte', 'Latte with maple walnut and cream', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_new, 'Vanilla Bean Matcha', 'Whisked matcha with vanilla bean', 4.75, 1.50, 'Matcha'),
 (@sub_new, 'Strawberry Matcha Cloud', 'Matcha with strawberry cream', 4.85, 1.50, 'Matcha'),
 (@sub_new, 'Honey Matcha Latte', 'Matcha latte sweetened with honey', 4.75, 1.50, 'Matcha'),
 (@sub_new, 'Blackberry Matcha Limeade', 'Matcha limeade with blackberry', 4.65, 1.35, 'Matcha'),
 (@sub_new, 'Coconut Cream Matcha', 'Matcha with coconut cream', 4.85, 1.50, 'Matcha'),
 (@sub_new, 'Peach Matcha Fizz', 'Sparkling matcha with peach', 4.65, 1.35, 'Matcha'),
 (@sub_new, 'Raspberry Mint Lemonade', 'Fresh-squeezed lemonade with raspberry and mint', 3.65, 0.85, 'Lemonade'),
 (@sub_new, 'Blueberry Basil Lemonade', 'Fresh-squeezed lemonade with blueberry and basil', 3.65, 0.85, 'Lemonade'),
 (@sub_new, 'Peach Ginger Lemonade', 'Fresh-squeezed lemonade with peach and ginger', 3.65, 0.85, 'Lemonade'),
 (@sub_new, 'Watermelon Lemonade', 'Fresh-squeezed lemonade with watermelon', 3.55, 0.80, 'Lemonade'),
 (@sub_new, 'Cucumber Lime Limeade', 'Limeade with cucumber and lime', 3.65, 0.85, 'Lemonade'),
 (@sub_new, 'Strawberry Kiwi Limeade', 'Limeade with strawberry and kiwi', 3.75, 0.90, 'Lemonade'),
 (@sub_new, 'Dawn Peach Zero', 'Zero-sugar sparkling water with peach', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Horizon Mint Zero', 'Zero-sugar sparkling water with mint', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Ocean Breeze Zero', 'Zero-sugar sparkling water with sea salt citrus', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Sunset Grape Zero', 'Zero-sugar sparkling water with grape', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Meadow Lime Zero', 'Zero-sugar sparkling water with lime', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Frost Melon Zero', 'Zero-sugar sparkling water with melon', 2.95, 0.60, 'Zero Sugar'),
 (@sub_new, 'Vanilla Bean Chiller', 'Blended frozen vanilla bean drink', 4.75, 1.30, 'Chillers'),
 (@sub_new, 'Caramel Pecan Chiller', 'Blended frozen coffee with caramel pecan', 4.95, 1.40, 'Chillers'),
 (@sub_new, 'White Chocolate Chiller', 'Blended frozen white chocolate drink', 4.95, 1.40, 'Chillers'),
 (@sub_new, 'Toasted Coconut Chiller', 'Blended frozen coffee with toasted coconut', 4.95, 1.40, 'Chillers'),
 (@sub_new, 'Cinnamon Dolce Chiller', 'Blended frozen coffee with cinnamon dolce', 4.95, 1.40, 'Chillers');

INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_fan, 'Georgia Peach Refresher', 'Peach-flavored refresher', 3.85, 0.95, 'Refreshers'),
 (@sub_fan, 'Wild Berry Refresher', 'Mixed wild berry refresher', 3.85, 0.95, 'Refreshers'),
 (@sub_fan, 'Citrus Sunburst Refresher', 'Citrus-flavored refresher', 3.85, 0.95, 'Refreshers'),
 (@sub_fan, 'Coconut Lime Refresher', 'Coconut lime refresher', 3.95, 1.00, 'Refreshers'),
 (@sub_fan, 'Pomegranate Refresher', 'Pomegranate-flavored refresher', 3.95, 1.00, 'Refreshers'),
 (@sub_fan, 'Dragonfruit Refresher', 'Dragonfruit-flavored refresher', 4.05, 1.05, 'Refreshers'),
 (@sub_fan, 'Blood Orange Refresher', 'Blood orange-flavored refresher', 3.95, 1.00, 'Refreshers'),
 (@sub_fan, 'Classic Caramel Cloud Latte', 'Latte with classic caramel and cream', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_fan, 'Hazelnut Cloud Latte', 'Latte with hazelnut and cream', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_fan, 'Mocha Cloud Latte', 'Latte with chocolate mocha and cream', 4.75, 1.35, 'Lattes & Espresso'),
 (@sub_fan, 'Cinnamon Cloud Latte', 'Latte with cinnamon and cream', 4.65, 1.30, 'Lattes & Espresso'),
 (@sub_fan, 'Double Shot Espresso', 'Double shot of espresso', 3.25, 0.85, 'Lattes & Espresso'),
 (@sub_fan, 'Oat Milk Cloud Latte', 'Latte made with oat milk and cream', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_fan, 'White Mocha Cloud Latte', 'Latte with white mocha and cream', 4.85, 1.35, 'Lattes & Espresso'),
 (@sub_fan, 'Classic Matcha Latte', 'Traditional matcha latte', 4.65, 1.45, 'Matcha'),
 (@sub_fan, 'Brown Sugar Matcha', 'Matcha with brown sugar', 4.75, 1.50, 'Matcha'),
 (@sub_fan, 'Coconut Matcha Cloud', 'Matcha with coconut cream', 4.85, 1.50, 'Matcha'),
 (@sub_fan, 'Rose Matcha Latte', 'Matcha latte with rose', 4.85, 1.50, 'Matcha'),
 (@sub_fan, 'Honeydew Matcha', 'Matcha with honeydew', 4.75, 1.50, 'Matcha'),
 (@sub_fan, 'Mango Matcha Fizz', 'Sparkling matcha with mango', 4.65, 1.35, 'Matcha'),
 (@sub_fan, 'Classic Pink Lemonade', 'Classic pink lemonade', 3.45, 0.75, 'Lemonade'),
 (@sub_fan, 'Mint Lemonade', 'Lemonade with fresh mint', 3.55, 0.80, 'Lemonade'),
 (@sub_fan, 'Blackberry Limeade', 'Limeade with blackberry', 3.85, 0.90, 'Lemonade'),
 (@sub_fan, 'Peach Lemonade', 'Lemonade with peach', 3.65, 0.85, 'Lemonade'),
 (@sub_fan, 'Cherry Limeade', 'Limeade with cherry', 3.75, 0.90, 'Lemonade'),
 (@sub_fan, 'Ginger Lemonade', 'Lemonade with ginger', 3.65, 0.85, 'Lemonade'),
 (@sub_fan, 'Classic Splash Zero', 'Zero-sugar sparkling water, classic', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Citrus Wave Zero', 'Zero-sugar sparkling water with citrus', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Berry Bliss Zero', 'Zero-sugar sparkling water with berry', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Tropical Tide Zero', 'Zero-sugar sparkling water with tropical fruit', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Cool Mint Zero', 'Zero-sugar sparkling water with mint', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Apple Crisp Zero', 'Zero-sugar sparkling water with apple', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Grapefruit Glow Zero', 'Zero-sugar sparkling water with grapefruit', 2.95, 0.60, 'Zero Sugar'),
 (@sub_fan, 'Classic Coffee Chiller', 'Blended frozen classic coffee', 4.65, 1.30, 'Chillers'),
 (@sub_fan, 'Java Chip Chiller', 'Blended frozen coffee with chocolate chips', 4.95, 1.40, 'Chillers'),
 (@sub_fan, 'Hazelnut Chiller', 'Blended frozen coffee with hazelnut', 4.85, 1.35, 'Chillers'),
 (@sub_fan, 'Caramel Macchiato Chiller', 'Blended frozen caramel macchiato', 4.95, 1.40, 'Chillers'),
 (@sub_fan, 'Double Mocha Chiller', 'Blended frozen coffee with double mocha', 4.99, 1.45, 'Chillers');

INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_lto, 'Grapefruit Rosemary Spritz', 'Sparkling grapefruit rosemary refresher', 4.25, 1.05, 'Spritz'),
 (@sub_lto, 'Strawberry Basil Spritz', 'Sparkling strawberry basil refresher', 4.25, 1.05, 'Spritz'),
 (@sub_lto, 'Mango Chili Spritz', 'Sparkling mango chili refresher', 4.35, 1.10, 'Spritz'),
 (@sub_lto, 'Watermelon Mint Spritz', 'Sparkling watermelon mint refresher', 4.25, 1.05, 'Spritz'),
 (@sub_lto, 'Blood Orange Ginger Spritz', 'Sparkling blood orange ginger refresher', 4.35, 1.10, 'Spritz'),
 (@sub_lto, 'Raspberry Lime Spritz', 'Sparkling raspberry lime refresher', 4.25, 1.05, 'Spritz'),
 (@sub_lto, 'Pineapple Jalapeno Spritz', 'Sparkling pineapple jalapeno refresher', 4.45, 1.15, 'Spritz'),
 (@sub_lto, 'Pomegranate Sage Spritz', 'Sparkling pomegranate sage refresher', 4.35, 1.10, 'Spritz'),
 (@sub_lto, 'Passionfruit Citrus Spritz', 'Sparkling passionfruit citrus refresher', 4.35, 1.10, 'Spritz'),
 (@sub_lto, 'Cranberry Orange Spritz', 'Sparkling cranberry orange refresher', 4.25, 1.05, 'Spritz'),
 (@sub_lto, 'Gingerbread Cloud Latte', 'Seasonal gingerbread latte', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Peppermint Mocha Cloud Latte', 'Seasonal peppermint mocha latte', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Eggnog Cloud Latte', 'Seasonal eggnog latte', 4.99, 1.45, 'Lattes & Espresso'),
 (@sub_lto, 'Toasted Marshmallow Latte', 'Latte with toasted marshmallow', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Apple Pie Cloud Latte', 'Seasonal apple pie latte', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Cinnamon Roll Cloud Latte', 'Latte with cinnamon roll flavor', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Butter Pecan Cloud Latte', 'Latte with butter pecan flavor', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Chestnut Praline Latte', 'Seasonal chestnut praline latte', 4.99, 1.45, 'Lattes & Espresso'),
 (@sub_lto, 'Caramel Apple Cloud Latte', 'Seasonal caramel apple latte', 4.95, 1.40, 'Lattes & Espresso'),
 (@sub_lto, 'Spiced Chai Cloud Latte', 'Latte with spiced chai and cream', 4.85, 1.35, 'Lattes & Espresso'),
 (@sub_lto, 'Cranberry Spice Zero', 'Zero-sugar sparkling water with cranberry spice', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Winter Mint Zero', 'Zero-sugar sparkling water with winter mint', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Autumn Apple Zero', 'Zero-sugar sparkling water with apple', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Spiced Pear Zero', 'Zero-sugar sparkling water with spiced pear', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Frosted Berry Zero', 'Zero-sugar sparkling water with frosted berry', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Citrus Chill Zero', 'Zero-sugar sparkling water with citrus chill', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Peppermint Zero', 'Zero-sugar sparkling water with peppermint', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Ginger Zero', 'Zero-sugar sparkling water with ginger', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Pomegranate Zero', 'Zero-sugar sparkling water with pomegranate', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Maple Zero', 'Zero-sugar sparkling water with maple', 2.95, 0.60, 'Zero Sugar'),
 (@sub_lto, 'Peppermint Mocha Chiller', 'Blended frozen peppermint mocha', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Pumpkin Spice Chiller', 'Blended frozen pumpkin spice', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Gingerbread Chiller', 'Blended frozen gingerbread', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Eggnog Chiller', 'Blended frozen eggnog', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Caramel Apple Chiller', 'Blended frozen caramel apple', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Toffee Nut Chiller', 'Blended frozen toffee nut', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Butterscotch Chiller', 'Blended frozen butterscotch', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Spiced Cinnamon Chiller', 'Blended frozen spiced cinnamon', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Maple Pecan Chiller', 'Blended frozen maple pecan', 4.99, 1.45, 'Chillers'),
 (@sub_lto, 'Hot Cocoa Chiller', 'Blended frozen hot cocoa', 4.99, 1.45, 'Chillers');

INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_bundles, 'Hot Coffee Bundle (5-pack)', 'Five hot coffees to go', 13.99, 4.20, 'Bundles'),
 (@sub_bundles, 'Cold Brew Bundle (5-pack)', 'Five cold brews to go', 16.99, 5.10, 'Bundles'),
 (@sub_bundles, 'Nitro Cold Brew Bundle (4-pack)', 'Four nitro cold brews to go', 17.99, 5.40, 'Bundles'),
 (@sub_bundles, 'Vanilla Latte Bundle (4-pack)', 'Four vanilla lattes to go', 17.99, 5.40, 'Bundles'),
 (@sub_bundles, 'Caramel Latte Bundle (4-pack)', 'Four caramel lattes to go', 17.99, 5.40, 'Bundles'),
 (@sub_bundles, 'Mocha Latte Bundle (4-pack)', 'Four mocha lattes to go', 17.99, 5.40, 'Bundles'),
 (@sub_bundles, 'Espresso Shot Bundle (6-pack)', 'Six espresso shots to go', 12.99, 3.90, 'Bundles'),
 (@sub_bundles, 'Chai Latte Bundle (4-pack)', 'Four chai lattes to go', 16.99, 5.10, 'Bundles'),
 (@sub_bundles, 'Green Tea Matcha Bundle (3-pack)', 'Three matcha drinks to go', 13.99, 5.00, 'Bundles'),
 (@sub_bundles, 'Strawberry Refresher Bundle (4-pack)', 'Four strawberry refreshers to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Mango Refresher Bundle (4-pack)', 'Four mango refreshers to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Black Cherry Refresher Bundle (4-pack)', 'Four black cherry refreshers to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Citrus Refresher Bundle (4-pack)', 'Four citrus refreshers to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Pink Lemonade Bundle (4-pack)', 'Four pink lemonades to go', 14.99, 4.50, 'Bundles'),
 (@sub_bundles, 'Classic Lemonade Bundle (4-pack)', 'Four classic lemonades to go', 13.99, 4.20, 'Bundles'),
 (@sub_bundles, 'Coconut Limeade Bundle (4-pack)', 'Four coconut limeades to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Zero Sugar Variety Bundle (4-pack)', 'Four assorted zero-sugar sparkling waters', 11.99, 3.60, 'Bundles'),
 (@sub_bundles, 'Berry Zero Bundle (4-pack)', 'Four berry zero-sugar sparkling waters', 11.99, 3.60, 'Bundles'),
 (@sub_bundles, 'Citrus Zero Bundle (4-pack)', 'Four citrus zero-sugar sparkling waters', 11.99, 3.60, 'Bundles'),
 (@sub_bundles, 'Mocha Chiller Bundle (4-pack)', 'Four mocha chillers to go', 19.99, 6.00, 'Bundles'),
 (@sub_bundles, 'Caramel Chiller Bundle (4-pack)', 'Four caramel chillers to go', 19.99, 6.00, 'Bundles'),
 (@sub_bundles, 'Vanilla Chiller Bundle (4-pack)', 'Four vanilla chillers to go', 19.99, 6.00, 'Bundles'),
 (@sub_bundles, 'Spritz Sampler Bundle (4-pack)', 'Four assorted spritz refreshers', 16.99, 5.10, 'Bundles'),
 (@sub_bundles, 'Family Coffee Box (12-cup)', 'Coffee box for family gatherings', 24.99, 9.50, 'Bundles'),
 (@sub_bundles, 'Party Refresher Box (10-pack)', 'Ten assorted refreshers for parties', 32.99, 12.50, 'Bundles'),
 (@sub_bundles, 'Office Coffee Box (20-cup)', 'Large coffee box for the office', 39.99, 15.00, 'Bundles'),
 (@sub_bundles, 'Kids Juice Bundle (4-pack)', 'Four kids juices to go', 6.99, 2.10, 'Bundles'),
 (@sub_bundles, 'Kids Chocolate Milk Bundle (4-pack)', 'Four kids chocolate milks to go', 6.99, 2.10, 'Bundles'),
 (@sub_bundles, 'Kids Lemonade Bundle (4-pack)', 'Four kids lemonades to go', 6.99, 2.10, 'Bundles'),
 (@sub_bundles, 'Weekend Warrior Bundle (6-pack)', 'Six assorted drinks for the weekend', 22.99, 8.70, 'Bundles'),
 (@sub_bundles, 'Morning Starter Bundle (6-pack)', 'Six coffee and espresso drinks to start the day', 21.99, 8.30, 'Bundles'),
 (@sub_bundles, 'Afternoon Pick-Me-Up Bundle (4-pack)', 'Four assorted afternoon drinks', 17.99, 5.40, 'Bundles'),
 (@sub_bundles, 'Iced Tea Bundle (5-pack)', 'Five iced teas to go', 13.99, 4.20, 'Bundles'),
 (@sub_bundles, 'Sweet Tea Bundle (5-pack)', 'Five sweet teas to go', 13.99, 4.20, 'Bundles'),
 (@sub_bundles, 'Unsweetened Tea Bundle (5-pack)', 'Five unsweetened teas to go', 13.99, 4.20, 'Bundles'),
 (@sub_bundles, 'Hazelnut Latte Bundle (4-pack)', 'Four hazelnut lattes to go', 17.99, 5.40, 'Bundles'),
 (@sub_bundles, 'Oat Milk Latte Bundle (4-pack)', 'Four oat milk lattes to go', 18.99, 5.70, 'Bundles'),
 (@sub_bundles, 'Almond Milk Latte Bundle (4-pack)', 'Four almond milk lattes to go', 18.99, 5.70, 'Bundles'),
 (@sub_bundles, 'Decaf Latte Bundle (4-pack)', 'Four decaf lattes to go', 17.99, 5.40, 'Bundles'),
 (@sub_bundles, 'Cold Brew Concentrate Bundle (2-pack)', 'Two cold brew concentrates, makes 8 drinks', 19.99, 6.00, 'Bundles'),
 (@sub_bundles, 'Matcha Latte Bundle (4-pack)', 'Four matcha lattes to go', 18.99, 5.70, 'Bundles'),
 (@sub_bundles, 'Tropical Refresher Bundle (4-pack)', 'Four tropical refreshers to go', 15.99, 4.80, 'Bundles'),
 (@sub_bundles, 'Grab & Go Variety Bundle (6-pack)', 'Six assorted drinks, grab and go', 23.99, 9.10, 'Bundles'),
 (@sub_bundles, 'Ultimate Sampler Bundle (10-pack)', 'Ten assorted drinks, the ultimate sampler', 34.99, 13.30, 'Bundles');

INSERT INTO ingredients (name, unit, stock_quantity, low_stock_threshold, unit_cost) VALUES
 ('Espresso Beans', 'g', 5000, 500, 0.02),
 ('Milk', 'ml', 10000, 1000, 0.001),
 ('Cold Brew Concentrate', 'ml', 6000, 500, 0.015),
 ('Tea Bags', 'unit', 200, 30, 0.10),
 ('Chai Concentrate', 'ml', 4000, 400, 0.02),
 ('Frozen Coffee Base', 'ml', 5000, 500, 0.02),
 ('Lemonade Base', 'ml', 5000, 500, 0.015),
 ('Turkey Slices', 'g', 3000, 300, 0.03),
 ('Bread', 'unit', 40, 10, 0.50),
 ('Cheese Slices', 'unit', 100, 20, 0.25),
 ('Tortilla Wrap', 'unit', 60, 10, 0.35),
 ('Mixed Vegetables', 'g', 4000, 400, 0.01),
 ('Hash Brown Patty', 'unit', 80, 15, 0.40),
 ('Plain Bagel Dough', 'unit', 50, 10, 0.55),
 ('Blueberry Muffin Batter', 'unit', 40, 10, 0.70),
 ('Glazed Donut Base', 'unit', 60, 12, 0.35),
 ('Chocolate Frosted Donut Base', 'unit', 60, 12, 0.35),
 ('Ground Coffee Bag Stock', 'unit', 30, 5, 4.50),
 ('K-Cup Box Stock', 'unit', 25, 5, 4.00);

-- Name-based lookups (not positional IDs, since item order now depends on the
-- department/subcategory layout above) — safe here since menu_items/ingredients
-- are already fully populated before this INSERT runs.
INSERT INTO recipe_ingredients (menu_item_id, ingredient_id, quantity_required)
SELECT mi.id, ing.id, q.qty FROM (
  SELECT 'Espresso' AS item, 'Espresso Beans' AS ing, 18 AS qty
  UNION ALL SELECT 'Latte', 'Espresso Beans', 18
  UNION ALL SELECT 'Latte', 'Milk', 150
  UNION ALL SELECT 'Cold Brew', 'Cold Brew Concentrate', 300
  UNION ALL SELECT 'Iced Tea', 'Tea Bags', 1
  UNION ALL SELECT 'Chai Latte', 'Chai Concentrate', 200
  UNION ALL SELECT 'Chai Latte', 'Milk', 100
  UNION ALL SELECT 'Frozen Coffee', 'Frozen Coffee Base', 300
  UNION ALL SELECT 'Frozen Lemonade', 'Lemonade Base', 300
  UNION ALL SELECT 'Turkey Sandwich', 'Turkey Slices', 100
  UNION ALL SELECT 'Turkey Sandwich', 'Bread', 1
  UNION ALL SELECT 'Grilled Cheese', 'Bread', 2
  UNION ALL SELECT 'Grilled Cheese', 'Cheese Slices', 2
  UNION ALL SELECT 'Veggie Wrap', 'Tortilla Wrap', 1
  UNION ALL SELECT 'Veggie Wrap', 'Mixed Vegetables', 150
  UNION ALL SELECT 'Hash Browns', 'Hash Brown Patty', 1
  UNION ALL SELECT 'Plain Bagel', 'Plain Bagel Dough', 1
  UNION ALL SELECT 'Blueberry Muffin', 'Blueberry Muffin Batter', 1
  UNION ALL SELECT 'Glazed Donut', 'Glazed Donut Base', 1
  UNION ALL SELECT 'Chocolate Frosted Donut', 'Chocolate Frosted Donut Base', 1
  UNION ALL SELECT 'Ground Coffee Bag', 'Ground Coffee Bag Stock', 1
  UNION ALL SELECT 'K-Cup Pack (10ct)', 'K-Cup Box Stock', 1
) q
JOIN menu_items mi ON mi.name = q.item
JOIN ingredients ing ON ing.name = q.ing;

-- ---- Bakery: expand all four sub-tabs to 50+ items each ----
-- ---- Donuts & Donut Holes (+22) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_donuts, 'Maple Frosted Donut', 'Classic donut topped with maple frosting', 1.89, 0.40, 'Classic Donuts'),
 (@sub_donuts, 'Vanilla Glazed Donut', 'Classic donut dipped in vanilla glaze', 1.79, 0.35, 'Classic Donuts'),
 (@sub_donuts, 'Coconut Flake Donut', 'Glazed donut topped with toasted coconut', 1.99, 0.45, 'Classic Donuts'),
 (@sub_donuts, 'Blueberry Cake Donut', 'Cake donut baked with real blueberries', 1.99, 0.45, 'Classic Donuts'),
 (@sub_donuts, 'Red Velvet Donut', 'Red velvet cake donut with cream cheese glaze', 2.09, 0.50, 'Classic Donuts'),
 (@sub_donuts, 'Birthday Cake Donut', 'Vanilla donut with sprinkle frosting and sprinkles', 1.99, 0.45, 'Classic Donuts'),
 (@sub_donuts, 'Raspberry Filled Donut', 'Classic donut filled with raspberry preserves', 1.99, 0.45, 'Filled Donuts'),
 (@sub_donuts, 'Peanut Butter Filled Donut', 'Donut filled with creamy peanut butter', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Key Lime Filled Donut', 'Donut filled with tangy key lime custard', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Cookies and Cream Filled Donut', 'Donut filled with cookies and cream', 2.19, 0.55, 'Filled Donuts'),
 (@sub_donuts, 'Pumpkin Filled Donut', 'Donut filled with spiced pumpkin cream', 2.09, 0.50, 'Filled Donuts'),
 (@sub_donuts, 'Maple Donut Holes (10-pack)', 'Ten bite-sized maple frosted donut holes', 3.49, 0.90, 'Donut Holes'),
 (@sub_donuts, 'Jelly Donut Holes (10-pack)', 'Ten bite-sized jelly filled donut holes', 3.79, 0.95, 'Donut Holes'),
 (@sub_donuts, 'Birthday Cake Donut Holes (10-pack)', 'Ten bite-sized birthday cake donut holes', 3.49, 0.90, 'Donut Holes'),
 (@sub_donuts, 'Cookies and Cream Donut Holes (10-pack)', 'Ten bite-sized cookies and cream donut holes', 3.79, 0.95, 'Donut Holes'),
 (@sub_donuts, 'Assorted Donut Holes (12-pack)', 'Twelve assorted bite-sized donut holes', 4.29, 1.10, 'Donut Holes'),
 (@sub_donuts, 'Bear Claw', 'Almond-filled pastry shaped like a bear claw', 2.79, 0.65, 'Fancy Donuts'),
 (@sub_donuts, 'French Cruller', 'Light and airy ridged glazed cruller', 1.99, 0.45, 'Fancy Donuts'),
 (@sub_donuts, 'Honey Dip Twist', 'Twisted yeast donut dipped in honey glaze', 2.19, 0.50, 'Fancy Donuts'),
 (@sub_donuts, 'Chocolate Frosted Long John', 'Oblong yeast donut with rich chocolate frosting', 2.29, 0.55, 'Fancy Donuts'),
 (@sub_donuts, 'Maple Bacon Donut', 'Maple frosted donut topped with candied bacon bits', 2.99, 0.75, 'Fancy Donuts'),
 (@sub_donuts, 'Croissant Donut', 'Flaky croissant pastry fried donut-style with glaze', 3.29, 0.85, 'Fancy Donuts');

-- ---- Bagels & Muffins (+33) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_bagels, 'Salt Bagel', 'Toasted bagel topped with coarse salt', 2.35, 0.55, 'Bagels'),
 (@sub_bagels, 'Poppy Seed Bagel', 'Toasted bagel topped with poppy seeds', 2.35, 0.55, 'Bagels'),
 (@sub_bagels, 'Multigrain Bagel', 'Toasted bagel made with mixed whole grains', 2.45, 0.60, 'Bagels'),
 (@sub_bagels, 'Garlic Bagel', 'Toasted bagel topped with roasted garlic', 2.35, 0.55, 'Bagels'),
 (@sub_bagels, 'Asiago Cheese Bagel', 'Toasted bagel topped with baked asiago cheese', 2.65, 0.65, 'Bagels'),
 (@sub_bagels, 'Jalapeno Cheddar Bagel', 'Toasted bagel with jalapeno and cheddar', 2.65, 0.65, 'Bagels'),
 (@sub_bagels, 'Chocolate Chip Bagel', 'Toasted bagel studded with chocolate chips', 2.55, 0.60, 'Bagels'),
 (@sub_bagels, 'Pumpernickel Bagel', 'Toasted dark rye pumpernickel bagel', 2.45, 0.60, 'Bagels'),
 (@sub_bagels, 'Rainbow Bagel', 'Toasted bagel swirled with rainbow-colored dough', 2.75, 0.70, 'Bagels'),
 (@sub_bagels, 'Sun-Dried Tomato Bagel', 'Toasted bagel with sun-dried tomato pieces', 2.55, 0.60, 'Bagels'),
 (@sub_bagels, 'Apple Cinnamon Muffin', 'Fresh baked muffin with apples and cinnamon', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Double Chocolate Muffin', 'Fresh baked muffin loaded with double chocolate', 3.09, 0.80, 'Muffins'),
 (@sub_bagels, 'Pumpkin Spice Muffin', 'Fresh baked muffin with warm pumpkin spice', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Carrot Muffin', 'Fresh baked carrot muffin with a hint of spice', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Zucchini Muffin', 'Fresh baked zucchini muffin', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Raspberry White Chocolate Muffin', 'Fresh baked muffin with raspberry and white chocolate', 3.19, 0.85, 'Muffins'),
 (@sub_bagels, 'Peach Muffin', 'Fresh baked muffin with real peach pieces', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Maple Pecan Muffin', 'Fresh baked muffin with maple glaze and pecans', 3.19, 0.85, 'Muffins'),
 (@sub_bagels, 'Coffee Cake Muffin', 'Fresh baked muffin with cinnamon crumb topping', 2.99, 0.80, 'Muffins'),
 (@sub_bagels, 'Oat Bran Muffin', 'Fresh baked hearty oat bran muffin', 2.79, 0.75, 'Muffins'),
 (@sub_bagels, 'Blueberry Cream Cheese', 'Individual portion of blueberry cream cheese', 1.09, 0.30, 'Spreads'),
 (@sub_bagels, 'Honey Walnut Cream Cheese', 'Individual portion of honey walnut cream cheese', 1.19, 0.30, 'Spreads'),
 (@sub_bagels, 'Garden Vegetable Cream Cheese', 'Individual portion of garden vegetable cream cheese', 1.09, 0.30, 'Spreads'),
 (@sub_bagels, 'Jalapeno Cream Cheese', 'Individual portion of jalapeno cream cheese', 1.09, 0.30, 'Spreads'),
 (@sub_bagels, 'Whipped Butter', 'Individual portion of whipped butter', 0.59, 0.15, 'Spreads'),
 (@sub_bagels, 'Peanut Butter', 'Individual portion of peanut butter', 0.79, 0.20, 'Spreads'),
 (@sub_bagels, 'Grape Jelly', 'Individual portion of grape jelly', 0.59, 0.15, 'Spreads'),
 (@sub_bagels, 'Bacon Egg and Cheese Bagel', 'Bacon, egg and cheese on a toasted bagel', 5.49, 2.00, 'Bagel Sandwiches'),
 (@sub_bagels, 'Sausage Egg and Cheese Bagel', 'Sausage, egg and cheese on a toasted bagel', 5.49, 2.00, 'Bagel Sandwiches'),
 (@sub_bagels, 'Ham Egg and Cheese Bagel', 'Ham, egg and cheese on a toasted bagel', 5.49, 2.00, 'Bagel Sandwiches'),
 (@sub_bagels, 'Veggie Egg and Cheese Bagel', 'Egg, cheese and grilled vegetables on a toasted bagel', 5.29, 1.90, 'Bagel Sandwiches'),
 (@sub_bagels, 'Lox and Cream Cheese Bagel', 'Smoked salmon and cream cheese on a toasted bagel', 6.99, 2.80, 'Bagel Sandwiches'),
 (@sub_bagels, 'Turkey Bacon Egg and Cheese Bagel', 'Turkey bacon, egg and cheese on a toasted bagel', 5.49, 2.00, 'Bagel Sandwiches');

-- ---- Sweet Treats (+34) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_treats, 'White Chocolate Macadamia Cookie', 'Fresh baked white chocolate macadamia nut cookie', 2.09, 0.50, 'Cookies'),
 (@sub_treats, 'M&M Cookie', 'Fresh baked cookie loaded with candy pieces', 2.09, 0.50, 'Cookies'),
 (@sub_treats, 'Triple Chocolate Cookie', 'Fresh baked cookie with three kinds of chocolate', 2.19, 0.55, 'Cookies'),
 (@sub_treats, 'Molasses Cookie', 'Fresh baked spiced molasses cookie', 1.89, 0.40, 'Cookies'),
 (@sub_treats, 'Shortbread Cookie', 'Fresh baked classic butter shortbread cookie', 1.79, 0.40, 'Cookies'),
 (@sub_treats, 'Gingerbread Cookie', 'Fresh baked spiced gingerbread cookie', 1.89, 0.40, 'Cookies'),
 (@sub_treats, 'Coconut Macaroon', 'Fresh baked chewy coconut macaroon', 2.09, 0.50, 'Cookies'),
 (@sub_treats, 'Biscotti', 'Crisp twice-baked almond biscotti', 2.29, 0.55, 'Cookies'),
 (@sub_treats, 'Apple Danish', 'Flaky pastry filled with spiced apple filling', 2.79, 0.70, 'Pastries'),
 (@sub_treats, 'Raspberry Danish', 'Flaky pastry filled with raspberry preserves', 2.79, 0.70, 'Pastries'),
 (@sub_treats, 'Almond Croissant', 'Buttery croissant filled with almond cream', 3.19, 0.80, 'Pastries'),
 (@sub_treats, 'Ham and Cheese Croissant', 'Buttery croissant filled with ham and cheese', 3.99, 1.30, 'Pastries'),
 (@sub_treats, 'Pecan Sticky Bun', 'Soft roll topped with caramel and pecans', 3.29, 0.85, 'Pastries'),
 (@sub_treats, 'Cream Cheese Danish', 'Flaky pastry filled with sweet cream cheese', 2.79, 0.70, 'Pastries'),
 (@sub_treats, 'Turnover - Apple', 'Puff pastry turnover filled with spiced apple', 2.69, 0.65, 'Pastries'),
 (@sub_treats, 'Turnover - Cherry', 'Puff pastry turnover filled with sweet cherry', 2.69, 0.65, 'Pastries'),
 (@sub_treats, 'Seven Layer Bar', 'Layered bar with chocolate, coconut and caramel', 2.79, 0.65, 'Cakes & Bars'),
 (@sub_treats, 'Rice Krispie Treat', 'Classic marshmallow crispy rice treat', 1.99, 0.40, 'Cakes & Bars'),
 (@sub_treats, 'Chocolate Fudge Cake Slice', 'Slice of rich chocolate fudge layer cake', 3.29, 0.80, 'Cakes & Bars'),
 (@sub_treats, 'Carrot Cake Slice', 'Slice of carrot cake with cream cheese frosting', 3.29, 0.80, 'Cakes & Bars'),
 (@sub_treats, 'Cheesecake Slice', 'Slice of classic New York style cheesecake', 3.49, 0.90, 'Cakes & Bars'),
 (@sub_treats, 'Marble Cake Slice', 'Slice of vanilla and chocolate marble cake', 2.99, 0.70, 'Cakes & Bars'),
 (@sub_treats, 'Raspberry Crumb Bar', 'Buttery crumb bar with raspberry filling', 2.59, 0.60, 'Cakes & Bars'),
 (@sub_treats, 'Peanut Butter Bar', 'Chewy peanut butter and chocolate bar', 2.59, 0.60, 'Cakes & Bars'),
 (@sub_treats, 'Pumpkin Roll Slice', 'Slice of spiced pumpkin cake rolled with cream cheese', 3.29, 0.80, 'Seasonal Sweets'),
 (@sub_treats, 'Gingerbread Loaf Slice', 'Slice of spiced gingerbread loaf cake', 2.99, 0.70, 'Seasonal Sweets'),
 (@sub_treats, 'Peppermint Bark Bar', 'Layered chocolate bar topped with crushed peppermint', 2.79, 0.65, 'Seasonal Sweets'),
 (@sub_treats, 'Strawberry Shortcake Cup', 'Layered strawberries, cake and whipped cream', 3.49, 0.90, 'Seasonal Sweets'),
 (@sub_treats, 'Key Lime Tart', 'Individual tart filled with tangy key lime custard', 3.29, 0.85, 'Seasonal Sweets'),
 (@sub_treats, 'Chocolate Mousse Cup', 'Individual cup of rich chocolate mousse', 3.49, 0.90, 'Seasonal Sweets'),
 (@sub_treats, 'Tiramisu Cup', 'Individual cup of espresso-soaked tiramisu', 3.79, 1.00, 'Seasonal Sweets'),
 (@sub_treats, 'Banana Pudding Cup', 'Individual cup of layered banana pudding', 3.29, 0.85, 'Seasonal Sweets'),
 (@sub_treats, 'S''mores Bar', 'Graham, chocolate and toasted marshmallow bar', 2.79, 0.65, 'Seasonal Sweets'),
 (@sub_treats, 'Churro Bites', 'Cinnamon sugar churro bites (6-pack)', 3.29, 0.80, 'Seasonal Sweets');

-- ---- Grab & Go (+41) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_grabgo, 'Tortilla Chips', 'Single-serve bag of tortilla chips', 1.99, 0.60, 'Snacks'),
 (@sub_grabgo, 'Cheese Puffs', 'Single-serve bag of cheese puffs', 1.79, 0.55, 'Snacks'),
 (@sub_grabgo, 'Veggie Straws', 'Single-serve bag of veggie straws', 2.29, 0.70, 'Snacks'),
 (@sub_grabgo, 'Rice Cakes', 'Single-serve pack of lightly salted rice cakes', 1.99, 0.55, 'Snacks'),
 (@sub_grabgo, 'Beef Jerky', 'Single-serve bag of beef jerky', 3.99, 1.40, 'Snacks'),
 (@sub_grabgo, 'Fruit Snacks', 'Single-serve pack of fruit snacks', 1.49, 0.40, 'Snacks'),
 (@sub_grabgo, 'Chocolate Covered Pretzels', 'Single-serve bag of chocolate covered pretzels', 2.49, 0.75, 'Snacks'),
 (@sub_grabgo, 'Mixed Nuts', 'Single-serve bag of roasted mixed nuts', 2.99, 0.95, 'Snacks'),
 (@sub_grabgo, 'Sea Salt Crackers', 'Single-serve pack of sea salt crackers', 1.79, 0.50, 'Snacks'),
 (@sub_grabgo, 'Pita Chips', 'Single-serve bag of pita chips', 2.29, 0.70, 'Snacks'),
 (@sub_grabgo, 'Hummus and Pretzel Cup', 'Grab-and-go cup of hummus with pretzels', 3.49, 1.10, 'Fresh & Healthy'),
 (@sub_grabgo, 'Veggie Cup with Ranch', 'Grab-and-go cup of fresh veggies with ranch dip', 3.49, 1.10, 'Fresh & Healthy'),
 (@sub_grabgo, 'Apple Slices', 'Grab-and-go cup of fresh sliced apples', 2.49, 0.75, 'Fresh & Healthy'),
 (@sub_grabgo, 'Banana', 'Fresh whole banana', 0.99, 0.30, 'Fresh & Healthy'),
 (@sub_grabgo, 'Overnight Oats Cup', 'Grab-and-go cup of overnight oats', 3.79, 1.20, 'Fresh & Healthy'),
 (@sub_grabgo, 'Chia Pudding Cup', 'Grab-and-go cup of chia seed pudding', 3.79, 1.20, 'Fresh & Healthy'),
 (@sub_grabgo, 'Protein Box', 'Grab-and-go box with cheese, nuts and fruit', 4.99, 1.90, 'Fresh & Healthy'),
 (@sub_grabgo, 'Edamame Cup', 'Grab-and-go cup of lightly salted edamame', 2.99, 0.90, 'Fresh & Healthy'),
 (@sub_grabgo, 'Bottled Water', 'Single-serve bottled water', 1.79, 0.40, 'Bottled Drinks'),
 (@sub_grabgo, 'Sparkling Water', 'Single-serve bottled sparkling water', 2.09, 0.55, 'Bottled Drinks'),
 (@sub_grabgo, 'Orange Juice Bottle', 'Single-serve bottled orange juice', 2.99, 0.90, 'Bottled Drinks'),
 (@sub_grabgo, 'Apple Juice Bottle', 'Single-serve bottled apple juice', 2.99, 0.90, 'Bottled Drinks'),
 (@sub_grabgo, 'Chocolate Milk Bottle', 'Single-serve bottled chocolate milk', 2.49, 0.75, 'Bottled Drinks'),
 (@sub_grabgo, 'Plain Milk Bottle', 'Single-serve bottled plain milk', 2.19, 0.65, 'Bottled Drinks'),
 (@sub_grabgo, 'Iced Tea Bottle', 'Single-serve bottled iced tea', 2.79, 0.80, 'Bottled Drinks'),
 (@sub_grabgo, 'Lemonade Bottle', 'Single-serve bottled lemonade', 2.79, 0.80, 'Bottled Drinks'),
 (@sub_grabgo, 'Sports Drink', 'Single-serve bottled sports drink', 2.99, 0.90, 'Bottled Drinks'),
 (@sub_grabgo, 'Energy Drink', 'Single-serve canned energy drink', 3.49, 1.10, 'Bottled Drinks'),
 (@sub_grabgo, 'Bottled Cold Brew Coffee', 'Single-serve bottled cold brew coffee', 3.99, 1.30, 'Bottled Drinks'),
 (@sub_grabgo, 'Kombucha Bottle', 'Single-serve bottled kombucha', 3.99, 1.30, 'Bottled Drinks'),
 (@sub_grabgo, 'Coconut Water Bottle', 'Single-serve bottled coconut water', 3.29, 1.00, 'Bottled Drinks'),
 (@sub_grabgo, 'Chocolate Bar', 'Single-serve chocolate candy bar', 1.99, 0.60, 'Candy & Gum'),
 (@sub_grabgo, 'Gummy Bears', 'Single-serve bag of gummy bears', 1.79, 0.55, 'Candy & Gum'),
 (@sub_grabgo, 'Mint Tin', 'Pocket tin of breath mints', 1.49, 0.40, 'Candy & Gum'),
 (@sub_grabgo, 'Chewing Gum Pack', 'Single pack of chewing gum', 1.29, 0.35, 'Candy & Gum'),
 (@sub_grabgo, 'Fruit Chews', 'Single-serve bag of chewy fruit candy', 1.49, 0.40, 'Candy & Gum'),
 (@sub_grabgo, 'Peanut Butter Cups', 'Single-serve pack of peanut butter cups', 1.99, 0.60, 'Candy & Gum'),
 (@sub_grabgo, 'Licorice Twists', 'Single-serve bag of licorice twists', 1.79, 0.55, 'Candy & Gum'),
 (@sub_grabgo, 'Caramel Chews', 'Single-serve bag of soft caramel chews', 1.79, 0.55, 'Candy & Gum'),
 (@sub_grabgo, 'Sour Candy Bag', 'Single-serve bag of assorted sour candy', 1.79, 0.55, 'Candy & Gum'),
 (@sub_grabgo, 'Chocolate Covered Raisins', 'Single-serve bag of chocolate covered raisins', 1.99, 0.60, 'Candy & Gum');

-- ---- Sandwiches: rename/add five colorful sub-tabs and populate with a dense item grid ----
UPDATE categories SET name = 'Specialty Sandwiches', sort_order = 2
  WHERE name = 'Sandwiches & More' AND parent_id = @dept_sandwiches;
UPDATE categories SET name = 'Wraps & Bowls', sort_order = 3
  WHERE name = 'Snacks & Wraps' AND parent_id = @dept_sandwiches;

SET @sub_specialty = @sub_sandwiches;
SET @sub_wraps = @sub_snacks;

INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Breakfast Sandwiches', 'Sandwich Station', 1, @dept_sandwiches);
SET @sub_breakfast = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Sides & Bites', 'Sandwich Station', 4, @dept_sandwiches);
SET @sub_sides = LAST_INSERT_ID();
INSERT INTO categories (name, station, sort_order, parent_id) VALUES ('Snacking', 'Sandwich Station', 5, @dept_sandwiches);
SET @sub_snacking = LAST_INSERT_ID();

-- Move the original Hash Browns item into the new Sides & Bites tab
UPDATE menu_items SET category_id = @sub_sides, section = 'Hash Browns'
  WHERE name = 'Hash Browns' AND category_id = @sub_wraps;

-- ---- Breakfast Sandwiches ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_breakfast, 'Bacon Egg and Cheese on English Muffin', 'Bacon, egg and cheese on a toasted English muffin', 4.99, 1.80, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheese on English Muffin', 'Sausage, egg and cheese on a toasted English muffin', 4.99, 1.80, 'Classic Breakfast'),
 (@sub_breakfast, 'Egg and Cheese on English Muffin', 'Egg and cheese on a toasted English muffin', 3.99, 1.30, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheese on Croissant', 'Bacon, egg and cheese on a buttery croissant', 5.29, 1.90, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheese on Biscuit', 'Sausage, egg and cheese on a warm biscuit', 5.29, 1.90, 'Classic Breakfast'),
 (@sub_breakfast, 'Ham Egg and Cheese on English Muffin', 'Ham, egg and cheese on a toasted English muffin', 4.99, 1.80, 'Classic Breakfast'),
 (@sub_breakfast, 'Seasoned Sausage Egg and Cheese', 'Seasoned sausage, egg and cheese on a toasted roll', 5.19, 1.85, 'Specialty Breakfast'),
 (@sub_breakfast, 'Turkey Sausage Egg and Cheese', 'Turkey sausage, egg and cheese on a toasted roll', 5.19, 1.85, 'Specialty Breakfast'),
 (@sub_breakfast, 'Veggie Egg White and Cheese', 'Egg whites and cheese with grilled vegetables', 4.79, 1.60, 'Specialty Breakfast'),
 (@sub_breakfast, 'Double Sausage Breakfast Sandwich', 'Double sausage, egg and cheese on a toasted roll', 5.99, 2.20, 'Specialty Breakfast'),
 (@sub_breakfast, 'Steak Egg and Cheese', 'Grilled steak, egg and cheese on a toasted roll', 6.49, 2.50, 'Specialty Breakfast'),
 (@sub_breakfast, 'Western Omelet Sandwich', 'Ham, peppers, onions and egg on a toasted roll', 5.29, 1.90, 'Specialty Breakfast'),
 (@sub_breakfast, 'Spinach Feta Egg White Sandwich', 'Egg whites, spinach and feta on a toasted roll', 5.29, 1.90, 'Specialty Breakfast');

-- ---- Specialty Sandwiches (keeps existing Turkey Sandwich / Grilled Cheese) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_specialty, 'Ham and Swiss Sandwich', 'Ham and swiss cheese on fresh bread', 6.79, 2.60, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef Sandwich', 'Sliced roast beef with lettuce and tomato', 7.29, 2.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Chicken Salad Sandwich', 'House-made chicken salad on fresh bread', 6.99, 2.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Tuna Salad Sandwich', 'House-made tuna salad on fresh bread', 6.99, 2.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Club Sandwich', 'Turkey, ham, bacon, lettuce and tomato triple-decker', 7.99, 3.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Italian Style Sandwich', 'Salami, ham and provolone with Italian dressing', 7.49, 2.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Bacon Grilled Cheese', 'Grilled cheese sandwich with crispy bacon', 5.99, 2.10, 'Grilled & Melts'),
 (@sub_specialty, 'Tomato Grilled Cheese', 'Grilled cheese sandwich with fresh tomato', 5.49, 1.90, 'Grilled & Melts'),
 (@sub_specialty, 'Ham and Cheese Melt', 'Grilled ham and cheese melt on fresh bread', 6.29, 2.30, 'Grilled & Melts'),
 (@sub_specialty, 'Turkey Melt', 'Grilled turkey and cheese melt on fresh bread', 6.49, 2.40, 'Grilled & Melts'),
 (@sub_specialty, 'Philly Style Steak Melt', 'Grilled steak, peppers, onions and cheese melt', 7.79, 3.00, 'Grilled & Melts'),
 (@sub_specialty, 'Chicken Bacon Ranch Melt', 'Grilled chicken, bacon, ranch and cheese melt', 7.49, 2.90, 'Grilled & Melts');

-- ---- Wraps & Bowls (keeps existing Veggie Wrap) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_wraps, 'Chicken Caesar Wrap', 'Grilled chicken, romaine and caesar dressing in a wrap', 6.99, 2.60, 'Wraps'),
 (@sub_wraps, 'Buffalo Chicken Wrap', 'Crispy buffalo chicken with lettuce and ranch in a wrap', 6.99, 2.60, 'Wraps'),
 (@sub_wraps, 'Southwest Chicken Wrap', 'Grilled chicken, corn and black beans in a wrap', 6.99, 2.60, 'Wraps'),
 (@sub_wraps, 'Turkey Bacon Wrap', 'Turkey, bacon, lettuce and tomato in a wrap', 6.79, 2.50, 'Wraps'),
 (@sub_wraps, 'Egg White Veggie Bowl', 'Egg whites, grilled vegetables and rice bowl', 6.29, 2.20, 'Bowls'),
 (@sub_wraps, 'Protein Power Bowl', 'Grilled chicken, quinoa and roasted vegetables bowl', 7.49, 2.80, 'Bowls'),
 (@sub_wraps, 'Breakfast Burrito Bowl', 'Scrambled egg, potatoes, cheese and salsa bowl', 6.49, 2.30, 'Bowls'),
 (@sub_wraps, 'Steak and Egg Bowl', 'Grilled steak, egg and roasted potatoes bowl', 7.99, 3.10, 'Bowls'),
 (@sub_wraps, 'Grilled Chicken and Rice Bowl', 'Grilled chicken, rice and mixed vegetables bowl', 6.99, 2.60, 'Bowls');

-- ---- Sides & Bites (keeps existing Hash Browns) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_sides, 'Loaded Hash Browns', 'Crispy hash browns topped with cheese and bacon', 3.49, 1.10, 'Hash Browns'),
 (@sub_sides, 'Cheesy Hash Browns', 'Crispy hash browns topped with melted cheese', 3.19, 0.95, 'Hash Browns'),
 (@sub_sides, 'Spicy Hash Browns', 'Crispy hash browns tossed in spicy seasoning', 2.99, 0.80, 'Hash Browns'),
 (@sub_sides, 'Egg and Cheese Bites', 'Bite-sized baked egg and cheese bites (5-pack)', 3.49, 1.10, 'Bites'),
 (@sub_sides, 'Bacon and Cheese Bites', 'Bite-sized baked bacon and cheese bites (5-pack)', 3.79, 1.20, 'Bites'),
 (@sub_sides, 'Veggie and Cheese Bites', 'Bite-sized baked veggie and cheese bites (5-pack)', 3.49, 1.10, 'Bites'),
 (@sub_sides, 'Side of Bacon', 'Three strips of crispy bacon', 2.79, 0.90, 'Sides'),
 (@sub_sides, 'Side of Sausage', 'Two savory sausage patties', 2.79, 0.90, 'Sides'),
 (@sub_sides, 'Side of Fresh Fruit', 'A cup of fresh seasonal fruit', 2.99, 1.00, 'Sides'),
 (@sub_sides, 'Side of Turkey Bacon', 'Three strips of crispy turkey bacon', 2.99, 0.95, 'Sides');

-- ---- Snacking ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_snacking, 'Mozzarella Sticks', 'Breaded mozzarella sticks with marinara (6-pack)', 5.49, 1.90, 'Snacks'),
 (@sub_snacking, 'Onion Rings', 'Crispy battered onion rings', 4.49, 1.50, 'Snacks'),
 (@sub_snacking, 'French Fries', 'Golden crispy French fries', 3.49, 1.10, 'Snacks'),
 (@sub_snacking, 'Pretzel Poppers', 'Bite-sized soft pretzel poppers with cheese sauce', 4.99, 1.70, 'Snacks'),
 (@sub_snacking, 'Chicken Tenders', 'Crispy breaded chicken tenders (4-pack)', 6.49, 2.40, 'Snacks'),
 (@sub_snacking, 'Loaded Fries', 'French fries topped with cheese and bacon', 5.49, 1.90, 'Snacks'),
 (@sub_snacking, 'Soft Pretzel', 'Warm soft pretzel with salt', 3.29, 1.00, 'Sweet & Savory'),
 (@sub_snacking, 'Cheese Curds', 'Crispy fried cheese curds', 5.29, 1.85, 'Sweet & Savory'),
 (@sub_snacking, 'Popcorn Chicken', 'Bite-sized crispy popcorn chicken', 5.99, 2.10, 'Sweet & Savory');

-- ---- Sandwiches: expand all five sub-tabs to 100+ items each ----
-- ---- Breakfast Sandwiches (+92) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_breakfast, 'Bacon Egg and Cheddar on English Muffin', 'Bacon, egg and cheddar cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Croissant', 'Bacon, egg and cheddar cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Biscuit', 'Bacon, egg and cheddar cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Plain Bagel', 'Bacon, egg and cheddar cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Flatbread', 'Bacon, egg and cheddar cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Ciabatta Roll', 'Bacon, egg and cheddar cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Sourdough Toast', 'Bacon, egg and cheddar cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Multigrain Roll', 'Bacon, egg and cheddar cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Brioche Bun', 'Bacon, egg and cheddar cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Cheddar on Kaiser Roll', 'Bacon, egg and cheddar cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on English Muffin', 'Bacon, egg and american cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Croissant', 'Bacon, egg and american cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Biscuit', 'Bacon, egg and american cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Plain Bagel', 'Bacon, egg and american cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Flatbread', 'Bacon, egg and american cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Ciabatta Roll', 'Bacon, egg and american cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Sourdough Toast', 'Bacon, egg and american cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Multigrain Roll', 'Bacon, egg and american cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Brioche Bun', 'Bacon, egg and american cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and American on Kaiser Roll', 'Bacon, egg and american cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on English Muffin', 'Bacon, egg and swiss cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Croissant', 'Bacon, egg and swiss cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Biscuit', 'Bacon, egg and swiss cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Plain Bagel', 'Bacon, egg and swiss cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Flatbread', 'Bacon, egg and swiss cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Ciabatta Roll', 'Bacon, egg and swiss cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Sourdough Toast', 'Bacon, egg and swiss cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Multigrain Roll', 'Bacon, egg and swiss cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Brioche Bun', 'Bacon, egg and swiss cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Swiss on Kaiser Roll', 'Bacon, egg and swiss cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on English Muffin', 'Bacon, egg and pepper jack cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Croissant', 'Bacon, egg and pepper jack cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Biscuit', 'Bacon, egg and pepper jack cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Plain Bagel', 'Bacon, egg and pepper jack cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Flatbread', 'Bacon, egg and pepper jack cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Ciabatta Roll', 'Bacon, egg and pepper jack cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Sourdough Toast', 'Bacon, egg and pepper jack cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Multigrain Roll', 'Bacon, egg and pepper jack cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Brioche Bun', 'Bacon, egg and pepper jack cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Pepper Jack on Kaiser Roll', 'Bacon, egg and pepper jack cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on English Muffin', 'Bacon, egg and provolone cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Croissant', 'Bacon, egg and provolone cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Biscuit', 'Bacon, egg and provolone cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Plain Bagel', 'Bacon, egg and provolone cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Flatbread', 'Bacon, egg and provolone cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Ciabatta Roll', 'Bacon, egg and provolone cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Sourdough Toast', 'Bacon, egg and provolone cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Multigrain Roll', 'Bacon, egg and provolone cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Brioche Bun', 'Bacon, egg and provolone cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Bacon Egg and Provolone on Kaiser Roll', 'Bacon, egg and provolone cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on English Muffin', 'Sausage, egg and cheddar cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Croissant', 'Sausage, egg and cheddar cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Biscuit', 'Sausage, egg and cheddar cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Plain Bagel', 'Sausage, egg and cheddar cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Flatbread', 'Sausage, egg and cheddar cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Ciabatta Roll', 'Sausage, egg and cheddar cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Sourdough Toast', 'Sausage, egg and cheddar cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Multigrain Roll', 'Sausage, egg and cheddar cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Brioche Bun', 'Sausage, egg and cheddar cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Cheddar on Kaiser Roll', 'Sausage, egg and cheddar cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on English Muffin', 'Sausage, egg and american cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Croissant', 'Sausage, egg and american cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Biscuit', 'Sausage, egg and american cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Plain Bagel', 'Sausage, egg and american cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Flatbread', 'Sausage, egg and american cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Ciabatta Roll', 'Sausage, egg and american cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Sourdough Toast', 'Sausage, egg and american cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Multigrain Roll', 'Sausage, egg and american cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Brioche Bun', 'Sausage, egg and american cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and American on Kaiser Roll', 'Sausage, egg and american cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on English Muffin', 'Sausage, egg and swiss cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Croissant', 'Sausage, egg and swiss cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Biscuit', 'Sausage, egg and swiss cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Plain Bagel', 'Sausage, egg and swiss cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Flatbread', 'Sausage, egg and swiss cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Ciabatta Roll', 'Sausage, egg and swiss cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Sourdough Toast', 'Sausage, egg and swiss cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Multigrain Roll', 'Sausage, egg and swiss cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Brioche Bun', 'Sausage, egg and swiss cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Swiss on Kaiser Roll', 'Sausage, egg and swiss cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on English Muffin', 'Sausage, egg and pepper jack cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Croissant', 'Sausage, egg and pepper jack cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Biscuit', 'Sausage, egg and pepper jack cheese on a toasted biscuit', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Plain Bagel', 'Sausage, egg and pepper jack cheese on a toasted plain bagel', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Flatbread', 'Sausage, egg and pepper jack cheese on a toasted flatbread', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Ciabatta Roll', 'Sausage, egg and pepper jack cheese on a toasted ciabatta roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Sourdough Toast', 'Sausage, egg and pepper jack cheese on a toasted sourdough toast', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Multigrain Roll', 'Sausage, egg and pepper jack cheese on a toasted multigrain roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Brioche Bun', 'Sausage, egg and pepper jack cheese on a toasted brioche bun', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Pepper Jack on Kaiser Roll', 'Sausage, egg and pepper jack cheese on a toasted kaiser roll', 6.09, 2.70, 'Specialty Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Provolone on English Muffin', 'Sausage, egg and provolone cheese on a toasted english muffin', 6.09, 2.70, 'Classic Breakfast'),
 (@sub_breakfast, 'Sausage Egg and Provolone on Croissant', 'Sausage, egg and provolone cheese on a toasted croissant', 6.09, 2.70, 'Classic Breakfast');

-- ---- Specialty Sandwiches (+86) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_specialty, 'Turkey and Swiss on White Bread', 'Turkey with swiss cheese on white bread', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and Cheddar on Wheat Bread', 'Turkey with cheddar cheese on wheat bread', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and Provolone on Rye Bread', 'Turkey with provolone cheese on rye bread', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and American on Sourdough', 'Turkey with american cheese on sourdough', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and Swiss on Ciabatta', 'Turkey with swiss cheese on ciabatta', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and Cheddar on Multigrain', 'Turkey with cheddar cheese on multigrain', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and Provolone on Kaiser Roll', 'Turkey with provolone cheese on kaiser roll', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and American on Hoagie Roll', 'Turkey with american cheese on hoagie roll', 7.59, 3.80, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and Swiss on White Bread', 'Ham with swiss cheese on white bread', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and Cheddar on Wheat Bread', 'Ham with cheddar cheese on wheat bread', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and Provolone on Rye Bread', 'Ham with provolone cheese on rye bread', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and American on Sourdough', 'Ham with american cheese on sourdough', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and Swiss on Ciabatta', 'Ham with swiss cheese on ciabatta', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and Cheddar on Multigrain', 'Ham with cheddar cheese on multigrain', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and Provolone on Kaiser Roll', 'Ham with provolone cheese on kaiser roll', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Ham and American on Hoagie Roll', 'Ham with american cheese on hoagie roll', 7.49, 3.70, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and Swiss on White Bread', 'Roast Beef with swiss cheese on white bread', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and Cheddar on Wheat Bread', 'Roast Beef with cheddar cheese on wheat bread', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and Provolone on Rye Bread', 'Roast Beef with provolone cheese on rye bread', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and American on Sourdough', 'Roast Beef with american cheese on sourdough', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and Swiss on Ciabatta', 'Roast Beef with swiss cheese on ciabatta', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and Cheddar on Multigrain', 'Roast Beef with cheddar cheese on multigrain', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and Provolone on Kaiser Roll', 'Roast Beef with provolone cheese on kaiser roll', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Roast Beef and American on Hoagie Roll', 'Roast Beef with american cheese on hoagie roll', 7.89, 4.10, 'Deli Sandwiches'),
 (@sub_specialty, 'Chicken Salad and Swiss on White Bread', 'Chicken Salad with swiss cheese on white bread', 7.69, 3.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Chicken Salad and Cheddar on Wheat Bread', 'Chicken Salad with cheddar cheese on wheat bread', 7.69, 3.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Chicken Salad and Provolone on Rye Bread', 'Chicken Salad with provolone cheese on rye bread', 7.69, 3.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Chicken Salad and American on Sourdough', 'Chicken Salad with american cheese on sourdough', 7.69, 3.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Chicken Salad and Swiss on Ciabatta', 'Chicken Salad with swiss cheese on ciabatta', 7.69, 3.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Chicken Salad and Cheddar on Multigrain', 'Chicken Salad with cheddar cheese on multigrain', 7.69, 3.90, 'Deli Sandwiches'),
 (@sub_specialty, 'Turkey and Cheddar Melt', 'Grilled turkey and cheddar cheese melt on fresh bread', 7.69, 3.50, 'Grilled & Melts'),
 (@sub_specialty, 'Turkey and American Melt', 'Grilled turkey and american cheese melt on fresh bread', 7.69, 3.50, 'Grilled & Melts'),
 (@sub_specialty, 'Turkey and Swiss Melt', 'Grilled turkey and swiss cheese melt on fresh bread', 7.69, 3.50, 'Grilled & Melts'),
 (@sub_specialty, 'Turkey and Pepper Jack Melt', 'Grilled turkey and pepper jack cheese melt on fresh bread', 7.69, 3.50, 'Grilled & Melts'),
 (@sub_specialty, 'Turkey and Provolone Melt', 'Grilled turkey and provolone cheese melt on fresh bread', 7.69, 3.50, 'Grilled & Melts'),
 (@sub_specialty, 'Ham and Cheddar Melt', 'Grilled ham and cheddar cheese melt on fresh bread', 7.59, 3.40, 'Grilled & Melts'),
 (@sub_specialty, 'Ham and American Melt', 'Grilled ham and american cheese melt on fresh bread', 7.59, 3.40, 'Grilled & Melts'),
 (@sub_specialty, 'Ham and Swiss Melt', 'Grilled ham and swiss cheese melt on fresh bread', 7.59, 3.40, 'Grilled & Melts'),
 (@sub_specialty, 'Ham and Pepper Jack Melt', 'Grilled ham and pepper jack cheese melt on fresh bread', 7.59, 3.40, 'Grilled & Melts'),
 (@sub_specialty, 'Ham and Provolone Melt', 'Grilled ham and provolone cheese melt on fresh bread', 7.59, 3.40, 'Grilled & Melts'),
 (@sub_specialty, 'Steak and Cheddar Melt', 'Grilled steak and cheddar cheese melt on fresh bread', 8.29, 4.10, 'Grilled & Melts'),
 (@sub_specialty, 'Steak and American Melt', 'Grilled steak and american cheese melt on fresh bread', 8.29, 4.10, 'Grilled & Melts'),
 (@sub_specialty, 'Steak and Swiss Melt', 'Grilled steak and swiss cheese melt on fresh bread', 8.29, 4.10, 'Grilled & Melts'),
 (@sub_specialty, 'Steak and Pepper Jack Melt', 'Grilled steak and pepper jack cheese melt on fresh bread', 8.29, 4.10, 'Grilled & Melts'),
 (@sub_specialty, 'Steak and Provolone Melt', 'Grilled steak and provolone cheese melt on fresh bread', 8.29, 4.10, 'Grilled & Melts'),
 (@sub_specialty, 'Chicken and Cheddar Melt', 'Grilled chicken and cheddar cheese melt on fresh bread', 7.99, 3.80, 'Grilled & Melts'),
 (@sub_specialty, 'Chicken and American Melt', 'Grilled chicken and american cheese melt on fresh bread', 7.99, 3.80, 'Grilled & Melts'),
 (@sub_specialty, 'Chicken and Swiss Melt', 'Grilled chicken and swiss cheese melt on fresh bread', 7.99, 3.80, 'Grilled & Melts'),
 (@sub_specialty, 'Chicken and Pepper Jack Melt', 'Grilled chicken and pepper jack cheese melt on fresh bread', 7.99, 3.80, 'Grilled & Melts'),
 (@sub_specialty, 'Chicken and Provolone Melt', 'Grilled chicken and provolone cheese melt on fresh bread', 7.99, 3.80, 'Grilled & Melts'),
 (@sub_specialty, 'Bacon and Cheddar Melt', 'Grilled bacon and cheddar cheese melt on fresh bread', 7.39, 3.20, 'Grilled & Melts'),
 (@sub_specialty, 'Bacon and American Melt', 'Grilled bacon and american cheese melt on fresh bread', 7.39, 3.20, 'Grilled & Melts'),
 (@sub_specialty, 'Bacon and Swiss Melt', 'Grilled bacon and swiss cheese melt on fresh bread', 7.39, 3.20, 'Grilled & Melts'),
 (@sub_specialty, 'Bacon and Pepper Jack Melt', 'Grilled bacon and pepper jack cheese melt on fresh bread', 7.39, 3.20, 'Grilled & Melts'),
 (@sub_specialty, 'Bacon and Provolone Melt', 'Grilled bacon and provolone cheese melt on fresh bread', 7.39, 3.20, 'Grilled & Melts'),
 (@sub_specialty, 'Tuna and Cheddar Melt', 'Grilled tuna and cheddar cheese melt on fresh bread', 7.79, 3.60, 'Grilled & Melts'),
 (@sub_specialty, 'Tuna and American Melt', 'Grilled tuna and american cheese melt on fresh bread', 7.79, 3.60, 'Grilled & Melts'),
 (@sub_specialty, 'Tuna and Swiss Melt', 'Grilled tuna and swiss cheese melt on fresh bread', 7.79, 3.60, 'Grilled & Melts'),
 (@sub_specialty, 'Tuna and Pepper Jack Melt', 'Grilled tuna and pepper jack cheese melt on fresh bread', 7.79, 3.60, 'Grilled & Melts'),
 (@sub_specialty, 'Tuna and Provolone Melt', 'Grilled tuna and provolone cheese melt on fresh bread', 7.79, 3.60, 'Grilled & Melts'),
 (@sub_specialty, 'Chicken Pesto Panini', 'Chicken Pesto pressed panini-style on ciabatta', 9.39, 4.40, 'Paninis'),
 (@sub_specialty, 'Caprese Panini', 'Caprese pressed panini-style on ciabatta', 8.89, 3.90, 'Paninis'),
 (@sub_specialty, 'Turkey Club Panini', 'Turkey Club pressed panini-style on ciabatta', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Steak and Cheese Panini', 'Steak and Cheese pressed panini-style on ciabatta', 9.49, 4.50, 'Paninis'),
 (@sub_specialty, 'Ham and Brie Panini', 'Ham and Brie pressed panini-style on ciabatta', 9.39, 4.40, 'Paninis'),
 (@sub_specialty, 'Veggie and Mozzarella Panini', 'Veggie and Mozzarella pressed panini-style on ciabatta', 8.79, 3.80, 'Paninis'),
 (@sub_specialty, 'BBQ Chicken Panini', 'BBQ Chicken pressed panini-style on ciabatta', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Buffalo Chicken Panini', 'Buffalo Chicken pressed panini-style on ciabatta', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Meatball Panini', 'Meatball pressed panini-style on ciabatta', 9.09, 4.10, 'Paninis'),
 (@sub_specialty, 'Roast Beef and Cheddar Panini', 'Roast Beef and Cheddar pressed panini-style on ciabatta', 9.39, 4.40, 'Paninis'),
 (@sub_specialty, 'Southwest Chicken Panini', 'Southwest Chicken pressed panini-style on ciabatta', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Spinach Artichoke Panini', 'Spinach Artichoke pressed panini-style on ciabatta', 8.69, 3.70, 'Paninis'),
 (@sub_specialty, 'Chicken Pesto Panini on Sourdough', 'Chicken Pesto pressed panini-style on sourdough', 9.39, 4.40, 'Paninis'),
 (@sub_specialty, 'Caprese Panini on Multigrain', 'Caprese pressed panini-style on multigrain', 8.89, 3.90, 'Paninis'),
 (@sub_specialty, 'Turkey Club Panini on Rye', 'Turkey Club pressed panini-style on rye', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Steak and Cheese Panini on Focaccia', 'Steak and Cheese pressed panini-style on focaccia', 9.49, 4.50, 'Paninis'),
 (@sub_specialty, 'Ham and Brie Panini on Sourdough', 'Ham and Brie pressed panini-style on sourdough', 9.39, 4.40, 'Paninis'),
 (@sub_specialty, 'Veggie and Mozzarella Panini on Multigrain', 'Veggie and Mozzarella pressed panini-style on multigrain', 8.79, 3.80, 'Paninis'),
 (@sub_specialty, 'BBQ Chicken Panini on Rye', 'BBQ Chicken pressed panini-style on rye', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Buffalo Chicken Panini on Focaccia', 'Buffalo Chicken pressed panini-style on focaccia', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Meatball Panini on Sourdough', 'Meatball pressed panini-style on sourdough', 9.09, 4.10, 'Paninis'),
 (@sub_specialty, 'Roast Beef and Cheddar Panini on Multigrain', 'Roast Beef and Cheddar pressed panini-style on multigrain', 9.39, 4.40, 'Paninis'),
 (@sub_specialty, 'Southwest Chicken Panini on Rye', 'Southwest Chicken pressed panini-style on rye', 9.19, 4.20, 'Paninis'),
 (@sub_specialty, 'Spinach Artichoke Panini on Focaccia', 'Spinach Artichoke pressed panini-style on focaccia', 8.69, 3.70, 'Paninis'),
 (@sub_specialty, 'Toasted Chicken Pesto Panini on Sourdough', 'Toasted chicken pesto panini on sourdough', 9.39, 4.40, 'Paninis'),
 (@sub_specialty, 'Pressed Chicken Pesto Panini on Sourdough', 'Pressed chicken pesto panini on sourdough', 9.39, 4.40, 'Paninis');

-- ---- Wraps & Bowls (+90) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_wraps, 'Chicken Caesar Tortilla Wrap', 'Chicken Caesar wrapped in a tortilla wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Chicken Caesar Spinach Wrap', 'Chicken Caesar wrapped in a spinach wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Chicken Caesar Whole Wheat Wrap', 'Chicken Caesar wrapped in a whole wheat wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Buffalo Chicken Tortilla Wrap', 'Buffalo Chicken wrapped in a tortilla wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Buffalo Chicken Spinach Wrap', 'Buffalo Chicken wrapped in a spinach wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Buffalo Chicken Whole Wheat Wrap', 'Buffalo Chicken wrapped in a whole wheat wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Southwest Chicken Tortilla Wrap', 'Southwest Chicken wrapped in a tortilla wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Southwest Chicken Spinach Wrap', 'Southwest Chicken wrapped in a spinach wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Southwest Chicken Whole Wheat Wrap', 'Southwest Chicken wrapped in a whole wheat wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Turkey Bacon Tortilla Wrap', 'Turkey Bacon wrapped in a tortilla wrap', 7.49, 3.50, 'Wraps'),
 (@sub_wraps, 'Turkey Bacon Spinach Wrap', 'Turkey Bacon wrapped in a spinach wrap', 7.49, 3.50, 'Wraps'),
 (@sub_wraps, 'Turkey Bacon Whole Wheat Wrap', 'Turkey Bacon wrapped in a whole wheat wrap', 7.49, 3.50, 'Wraps'),
 (@sub_wraps, 'Steak and Cheese Wrap', 'Steak and Cheese wrapped in a wrap', 7.99, 4.00, 'Wraps'),
 (@sub_wraps, 'Steak and Cheese Tortilla Wrap', 'Steak and Cheese wrapped in a tortilla wrap', 7.99, 4.00, 'Wraps'),
 (@sub_wraps, 'Steak and Cheese Spinach Wrap', 'Steak and Cheese wrapped in a spinach wrap', 7.99, 4.00, 'Wraps'),
 (@sub_wraps, 'Steak and Cheese Whole Wheat Wrap', 'Steak and Cheese wrapped in a whole wheat wrap', 7.99, 4.00, 'Wraps'),
 (@sub_wraps, 'BBQ Chicken Wrap', 'BBQ Chicken wrapped in a wrap', 7.69, 3.70, 'Wraps'),
 (@sub_wraps, 'BBQ Chicken Tortilla Wrap', 'BBQ Chicken wrapped in a tortilla wrap', 7.69, 3.70, 'Wraps'),
 (@sub_wraps, 'BBQ Chicken Spinach Wrap', 'BBQ Chicken wrapped in a spinach wrap', 7.69, 3.70, 'Wraps'),
 (@sub_wraps, 'BBQ Chicken Whole Wheat Wrap', 'BBQ Chicken wrapped in a whole wheat wrap', 7.69, 3.70, 'Wraps'),
 (@sub_wraps, 'Chicken Bacon Ranch Wrap', 'Chicken Bacon Ranch wrapped in a wrap', 7.79, 3.80, 'Wraps'),
 (@sub_wraps, 'Chicken Bacon Ranch Tortilla Wrap', 'Chicken Bacon Ranch wrapped in a tortilla wrap', 7.79, 3.80, 'Wraps'),
 (@sub_wraps, 'Chicken Bacon Ranch Spinach Wrap', 'Chicken Bacon Ranch wrapped in a spinach wrap', 7.79, 3.80, 'Wraps'),
 (@sub_wraps, 'Chicken Bacon Ranch Whole Wheat Wrap', 'Chicken Bacon Ranch wrapped in a whole wheat wrap', 7.79, 3.80, 'Wraps'),
 (@sub_wraps, 'Veggie and Hummus Wrap', 'Veggie and Hummus wrapped in a wrap', 6.99, 3.00, 'Wraps'),
 (@sub_wraps, 'Veggie and Hummus Tortilla Wrap', 'Veggie and Hummus wrapped in a tortilla wrap', 6.99, 3.00, 'Wraps'),
 (@sub_wraps, 'Veggie and Hummus Spinach Wrap', 'Veggie and Hummus wrapped in a spinach wrap', 6.99, 3.00, 'Wraps'),
 (@sub_wraps, 'Veggie and Hummus Whole Wheat Wrap', 'Veggie and Hummus wrapped in a whole wheat wrap', 6.99, 3.00, 'Wraps'),
 (@sub_wraps, 'Falafel Wrap', 'Falafel wrapped in a wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Falafel Tortilla Wrap', 'Falafel wrapped in a tortilla wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Falafel Spinach Wrap', 'Falafel wrapped in a spinach wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Falafel Whole Wheat Wrap', 'Falafel wrapped in a whole wheat wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Mediterranean Veggie Wrap', 'Mediterranean Veggie wrapped in a wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Mediterranean Veggie Tortilla Wrap', 'Mediterranean Veggie wrapped in a tortilla wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Mediterranean Veggie Spinach Wrap', 'Mediterranean Veggie wrapped in a spinach wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Mediterranean Veggie Whole Wheat Wrap', 'Mediterranean Veggie wrapped in a whole wheat wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Ham and Swiss Wrap', 'Ham and Swiss wrapped in a wrap', 7.49, 3.50, 'Wraps'),
 (@sub_wraps, 'Ham and Swiss Tortilla Wrap', 'Ham and Swiss wrapped in a tortilla wrap', 7.49, 3.50, 'Wraps'),
 (@sub_wraps, 'Ham and Swiss Spinach Wrap', 'Ham and Swiss wrapped in a spinach wrap', 7.49, 3.50, 'Wraps'),
 (@sub_wraps, 'Ham and Swiss Whole Wheat Wrap', 'Ham and Swiss wrapped in a whole wheat wrap', 7.49, 3.50, 'Wraps'),
 (@sub_wraps, 'Tuna Salad Wrap', 'Tuna Salad wrapped in a wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Tuna Salad Tortilla Wrap', 'Tuna Salad wrapped in a tortilla wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Tuna Salad Spinach Wrap', 'Tuna Salad wrapped in a spinach wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Tuna Salad Whole Wheat Wrap', 'Tuna Salad wrapped in a whole wheat wrap', 7.59, 3.60, 'Wraps'),
 (@sub_wraps, 'Egg Salad Wrap', 'Egg Salad wrapped in a wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Egg Salad Tortilla Wrap', 'Egg Salad wrapped in a tortilla wrap', 7.09, 3.10, 'Wraps'),
 (@sub_wraps, 'Egg White Veggie Rice Bowl', 'Egg White Veggie served over a rice bowl', 7.19, 3.20, 'Bowls'),
 (@sub_wraps, 'Egg White Veggie Quinoa Bowl', 'Egg White Veggie served over a quinoa bowl', 7.19, 3.20, 'Bowls'),
 (@sub_wraps, 'Egg White Veggie Power Bowl', 'Egg White Veggie served over a power bowl', 7.19, 3.20, 'Bowls'),
 (@sub_wraps, 'Egg White Veggie Grain Bowl', 'Egg White Veggie served over a grain bowl', 7.19, 3.20, 'Bowls'),
 (@sub_wraps, 'Protein Power Rice Bowl', 'Protein Power served over a rice bowl', 7.79, 3.80, 'Bowls'),
 (@sub_wraps, 'Protein Power Quinoa Bowl', 'Protein Power served over a quinoa bowl', 7.79, 3.80, 'Bowls'),
 (@sub_wraps, 'Protein Power Power Bowl', 'Protein Power served over a power bowl', 7.79, 3.80, 'Bowls'),
 (@sub_wraps, 'Protein Power Grain Bowl', 'Protein Power served over a grain bowl', 7.79, 3.80, 'Bowls'),
 (@sub_wraps, 'Breakfast Burrito Rice Bowl', 'Breakfast Burrito served over a rice bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Breakfast Burrito Quinoa Bowl', 'Breakfast Burrito served over a quinoa bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Breakfast Burrito Power Bowl', 'Breakfast Burrito served over a power bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Breakfast Burrito Grain Bowl', 'Breakfast Burrito served over a grain bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Steak and Egg Rice Bowl', 'Steak and Egg served over a rice bowl', 8.09, 4.10, 'Bowls'),
 (@sub_wraps, 'Steak and Egg Quinoa Bowl', 'Steak and Egg served over a quinoa bowl', 8.09, 4.10, 'Bowls'),
 (@sub_wraps, 'Steak and Egg Power Bowl', 'Steak and Egg served over a power bowl', 8.09, 4.10, 'Bowls'),
 (@sub_wraps, 'Steak and Egg Grain Bowl', 'Steak and Egg served over a grain bowl', 8.09, 4.10, 'Bowls'),
 (@sub_wraps, 'Grilled Chicken and Rice Rice Bowl', 'Grilled Chicken and Rice served over a rice bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'Grilled Chicken and Rice Quinoa Bowl', 'Grilled Chicken and Rice served over a quinoa bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'Grilled Chicken and Rice Power Bowl', 'Grilled Chicken and Rice served over a power bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'Grilled Chicken and Rice Grain Bowl', 'Grilled Chicken and Rice served over a grain bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'Quinoa Veggie Rice Bowl', 'Quinoa Veggie served over a rice bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Quinoa Veggie Quinoa Bowl', 'Quinoa Veggie served over a quinoa bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Quinoa Veggie Power Bowl', 'Quinoa Veggie served over a power bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Quinoa Veggie Grain Bowl', 'Quinoa Veggie served over a grain bowl', 7.29, 3.30, 'Bowls'),
 (@sub_wraps, 'Southwest Steak Rice Bowl', 'Southwest Steak served over a rice bowl', 7.99, 4.00, 'Bowls'),
 (@sub_wraps, 'Southwest Steak Quinoa Bowl', 'Southwest Steak served over a quinoa bowl', 7.99, 4.00, 'Bowls'),
 (@sub_wraps, 'Southwest Steak Power Bowl', 'Southwest Steak served over a power bowl', 7.99, 4.00, 'Bowls'),
 (@sub_wraps, 'Southwest Steak Grain Bowl', 'Southwest Steak served over a grain bowl', 7.99, 4.00, 'Bowls'),
 (@sub_wraps, 'Buffalo Chicken Rice Bowl', 'Buffalo Chicken served over a rice bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Buffalo Chicken Quinoa Bowl', 'Buffalo Chicken served over a quinoa bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Buffalo Chicken Power Bowl', 'Buffalo Chicken served over a power bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Buffalo Chicken Grain Bowl', 'Buffalo Chicken served over a grain bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Mediterranean Chicken Rice Bowl', 'Mediterranean Chicken served over a rice bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Mediterranean Chicken Quinoa Bowl', 'Mediterranean Chicken served over a quinoa bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Mediterranean Chicken Power Bowl', 'Mediterranean Chicken served over a power bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Mediterranean Chicken Grain Bowl', 'Mediterranean Chicken served over a grain bowl', 7.69, 3.70, 'Bowls'),
 (@sub_wraps, 'Teriyaki Chicken Rice Bowl', 'Teriyaki Chicken served over a rice bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'Teriyaki Chicken Quinoa Bowl', 'Teriyaki Chicken served over a quinoa bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'Teriyaki Chicken Power Bowl', 'Teriyaki Chicken served over a power bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'Teriyaki Chicken Grain Bowl', 'Teriyaki Chicken served over a grain bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'BBQ Chicken Rice Bowl', 'BBQ Chicken served over a rice bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'BBQ Chicken Quinoa Bowl', 'BBQ Chicken served over a quinoa bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'BBQ Chicken Power Bowl', 'BBQ Chicken served over a power bowl', 7.59, 3.60, 'Bowls'),
 (@sub_wraps, 'BBQ Chicken Grain Bowl', 'BBQ Chicken served over a grain bowl', 7.59, 3.60, 'Bowls');

-- ---- Sides & Bites (+94) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_sides, 'Garlic Herb Hash Browns', 'Crispy garlic herb seasoned hash browns', 3.44, 0.95, 'Hash Browns'),
 (@sub_sides, 'Bacon Hash Browns', 'Crispy bacon seasoned hash browns', 3.59, 1.00, 'Hash Browns'),
 (@sub_sides, 'Ranch Hash Browns', 'Crispy ranch seasoned hash browns', 2.99, 0.80, 'Hash Browns'),
 (@sub_sides, 'Buffalo Hash Browns', 'Crispy buffalo seasoned hash browns', 3.14, 0.85, 'Hash Browns'),
 (@sub_sides, 'BBQ Hash Browns', 'Crispy bbq seasoned hash browns', 3.29, 0.90, 'Hash Browns'),
 (@sub_sides, 'Southwest Hash Browns', 'Crispy southwest seasoned hash browns', 3.44, 0.95, 'Hash Browns'),
 (@sub_sides, 'Everything Seasoned Hash Browns', 'Crispy everything seasoned seasoned hash browns', 3.59, 1.00, 'Hash Browns'),
 (@sub_sides, 'Jalapeno Hash Browns', 'Crispy jalapeno seasoned hash browns', 2.99, 0.80, 'Hash Browns'),
 (@sub_sides, 'Chili Cheese Hash Browns', 'Crispy chili cheese seasoned hash browns', 3.14, 0.85, 'Hash Browns'),
 (@sub_sides, 'Loaded Hash Browns (6-pack)', 'Crispy loaded seasoned hash browns', 3.29, 0.90, 'Hash Browns'),
 (@sub_sides, 'Cheesy Hash Browns (6-pack)', 'Crispy cheesy seasoned hash browns', 3.44, 0.95, 'Hash Browns'),
 (@sub_sides, 'Spicy Hash Browns (6-pack)', 'Crispy spicy seasoned hash browns', 3.59, 1.00, 'Hash Browns'),
 (@sub_sides, 'Garlic Herb Hash Browns (6-pack)', 'Crispy garlic herb seasoned hash browns', 2.99, 0.80, 'Hash Browns'),
 (@sub_sides, 'Bacon Hash Browns (6-pack)', 'Crispy bacon seasoned hash browns', 3.14, 0.85, 'Hash Browns'),
 (@sub_sides, 'Ranch Hash Browns (6-pack)', 'Crispy ranch seasoned hash browns', 3.29, 0.90, 'Hash Browns'),
 (@sub_sides, 'Buffalo Hash Browns (6-pack)', 'Crispy buffalo seasoned hash browns', 3.44, 0.95, 'Hash Browns'),
 (@sub_sides, 'BBQ Hash Browns (6-pack)', 'Crispy bbq seasoned hash browns', 3.59, 1.00, 'Hash Browns'),
 (@sub_sides, 'Southwest Hash Browns (6-pack)', 'Crispy southwest seasoned hash browns', 2.99, 0.80, 'Hash Browns'),
 (@sub_sides, 'Everything Seasoned Hash Browns (6-pack)', 'Crispy everything seasoned seasoned hash browns', 3.14, 0.85, 'Hash Browns'),
 (@sub_sides, 'Jalapeno Hash Browns (6-pack)', 'Crispy jalapeno seasoned hash browns', 3.29, 0.90, 'Hash Browns'),
 (@sub_sides, 'Chili Cheese Hash Browns (6-pack)', 'Crispy chili cheese seasoned hash browns', 3.44, 0.95, 'Hash Browns'),
 (@sub_sides, 'Loaded Hash Browns (10-pack)', 'Crispy loaded seasoned hash browns', 3.59, 1.00, 'Hash Browns'),
 (@sub_sides, 'Cheesy Hash Browns (10-pack)', 'Crispy cheesy seasoned hash browns', 2.99, 0.80, 'Hash Browns'),
 (@sub_sides, 'Spicy Hash Browns (10-pack)', 'Crispy spicy seasoned hash browns', 3.14, 0.85, 'Hash Browns'),
 (@sub_sides, 'Egg and Cheese Bites (5-pack)', 'Bite-sized baked egg and cheese bites', 3.29, 1.00, 'Bites'),
 (@sub_sides, 'Bacon and Cheese Bites (5-pack)', 'Bite-sized baked bacon and cheese bites', 3.39, 1.05, 'Bites'),
 (@sub_sides, 'Veggie and Cheese Bites (5-pack)', 'Bite-sized baked veggie and cheese bites', 3.49, 1.10, 'Bites'),
 (@sub_sides, 'Sausage and Cheese Bites (5-pack)', 'Bite-sized baked sausage and cheese bites', 3.59, 1.15, 'Bites'),
 (@sub_sides, 'Ham and Cheese Bites (5-pack)', 'Bite-sized baked ham and cheese bites', 3.69, 1.20, 'Bites'),
 (@sub_sides, 'Spinach and Feta Bites (5-pack)', 'Bite-sized baked spinach and feta bites', 3.79, 1.25, 'Bites'),
 (@sub_sides, 'Broccoli and Cheddar Bites (5-pack)', 'Bite-sized baked broccoli and cheddar bites', 3.29, 1.00, 'Bites'),
 (@sub_sides, 'Buffalo Chicken Bites (5-pack)', 'Bite-sized baked buffalo chicken bites', 3.39, 1.05, 'Bites'),
 (@sub_sides, 'BBQ Chicken Bites (5-pack)', 'Bite-sized baked bbq chicken bites', 3.49, 1.10, 'Bites'),
 (@sub_sides, 'Pepperoni and Cheese Bites (5-pack)', 'Bite-sized baked pepperoni and cheese bites', 3.59, 1.15, 'Bites'),
 (@sub_sides, 'Jalapeno and Cheese Bites (5-pack)', 'Bite-sized baked jalapeno and cheese bites', 3.69, 1.20, 'Bites'),
 (@sub_sides, 'Three Cheese Bites (5-pack)', 'Bite-sized baked three cheese bites', 3.79, 1.25, 'Bites'),
 (@sub_sides, 'Egg and Cheese Bites (8-pack)', 'Bite-sized baked egg and cheese bites', 3.29, 1.00, 'Bites'),
 (@sub_sides, 'Bacon and Cheese Bites (8-pack)', 'Bite-sized baked bacon and cheese bites', 3.39, 1.05, 'Bites'),
 (@sub_sides, 'Veggie and Cheese Bites (8-pack)', 'Bite-sized baked veggie and cheese bites', 3.49, 1.10, 'Bites'),
 (@sub_sides, 'Sausage and Cheese Bites (8-pack)', 'Bite-sized baked sausage and cheese bites', 3.59, 1.15, 'Bites'),
 (@sub_sides, 'Ham and Cheese Bites (8-pack)', 'Bite-sized baked ham and cheese bites', 3.69, 1.20, 'Bites'),
 (@sub_sides, 'Spinach and Feta Bites (8-pack)', 'Bite-sized baked spinach and feta bites', 3.79, 1.25, 'Bites'),
 (@sub_sides, 'Broccoli and Cheddar Bites (8-pack)', 'Bite-sized baked broccoli and cheddar bites', 3.29, 1.00, 'Bites'),
 (@sub_sides, 'Buffalo Chicken Bites (8-pack)', 'Bite-sized baked buffalo chicken bites', 3.39, 1.05, 'Bites'),
 (@sub_sides, 'BBQ Chicken Bites (8-pack)', 'Bite-sized baked bbq chicken bites', 3.49, 1.10, 'Bites'),
 (@sub_sides, 'Pepperoni and Cheese Bites (8-pack)', 'Bite-sized baked pepperoni and cheese bites', 3.59, 1.15, 'Bites'),
 (@sub_sides, 'Jalapeno and Cheese Bites (8-pack)', 'Bite-sized baked jalapeno and cheese bites', 3.69, 1.20, 'Bites'),
 (@sub_sides, 'Three Cheese Bites (8-pack)', 'Bite-sized baked three cheese bites', 3.79, 1.25, 'Bites'),
 (@sub_sides, 'Side of Ham', 'A portion of ham', 3.74, 1.85, 'Sides'),
 (@sub_sides, 'Side of Sausage Gravy', 'A portion of sausage gravy', 3.89, 2.00, 'Sides'),
 (@sub_sides, 'Side of Grits', 'A portion of grits', 3.64, 1.75, 'Sides'),
 (@sub_sides, 'Side of Home Fries', 'A portion of home fries', 3.69, 1.80, 'Sides'),
 (@sub_sides, 'Side of Avocado', 'A portion of avocado', 3.99, 2.10, 'Sides'),
 (@sub_sides, 'Side of Tomato Slices', 'A portion of tomato slices', 3.39, 1.50, 'Sides'),
 (@sub_sides, 'Side of Applesauce', 'A portion of applesauce', 3.34, 1.45, 'Sides'),
 (@sub_sides, 'Side of Yogurt', 'A portion of yogurt', 3.69, 1.80, 'Sides'),
 (@sub_sides, 'Side of Bacon (Large)', 'A portion of bacon', 4.19, 2.00, 'Sides'),
 (@sub_sides, 'Side of Sausage (Large)', 'A portion of sausage', 4.19, 2.00, 'Sides'),
 (@sub_sides, 'Side of Fresh Fruit (Large)', 'A portion of fresh fruit', 4.29, 2.10, 'Sides'),
 (@sub_sides, 'Side of Turkey Bacon (Large)', 'A portion of turkey bacon', 4.24, 2.05, 'Sides'),
 (@sub_sides, 'Side of Ham (Large)', 'A portion of ham', 4.24, 2.05, 'Sides'),
 (@sub_sides, 'Side of Sausage Gravy (Large)', 'A portion of sausage gravy', 4.39, 2.20, 'Sides'),
 (@sub_sides, 'Side of Grits (Large)', 'A portion of grits', 4.14, 1.95, 'Sides'),
 (@sub_sides, 'Side of Home Fries (Large)', 'A portion of home fries', 4.19, 2.00, 'Sides'),
 (@sub_sides, 'Side of Avocado (Large)', 'A portion of avocado', 4.49, 2.30, 'Sides'),
 (@sub_sides, 'Side of Tomato Slices (Large)', 'A portion of tomato slices', 3.89, 1.70, 'Sides'),
 (@sub_sides, 'Side of Applesauce (Large)', 'A portion of applesauce', 3.84, 1.65, 'Sides'),
 (@sub_sides, 'Side of Yogurt (Large)', 'A portion of yogurt', 4.19, 2.00, 'Sides'),
 (@sub_sides, 'Side of Bacon (Extra Large)', 'A portion of bacon', 4.69, 2.20, 'Sides'),
 (@sub_sides, 'Side of Sausage (Extra Large)', 'A portion of sausage', 4.69, 2.20, 'Sides'),
 (@sub_sides, 'Side of Fresh Fruit (Extra Large)', 'A portion of fresh fruit', 4.79, 2.30, 'Sides'),
 (@sub_sides, 'Side of Turkey Bacon (Extra Large)', 'A portion of turkey bacon', 4.74, 2.25, 'Sides'),
 (@sub_sides, 'Small Classic Tater Tots', 'Small portion of classic seasoned crispy tater tots', 3.29, 0.90, 'Tater Tots'),
 (@sub_sides, 'Large Classic Tater Tots', 'Large portion of classic seasoned crispy tater tots', 4.24, 1.25, 'Tater Tots'),
 (@sub_sides, 'Small Loaded Tater Tots', 'Small portion of loaded seasoned crispy tater tots', 3.59, 1.00, 'Tater Tots'),
 (@sub_sides, 'Large Loaded Tater Tots', 'Large portion of loaded seasoned crispy tater tots', 4.54, 1.35, 'Tater Tots'),
 (@sub_sides, 'Small Cheesy Tater Tots', 'Small portion of cheesy seasoned crispy tater tots', 3.29, 0.90, 'Tater Tots'),
 (@sub_sides, 'Large Cheesy Tater Tots', 'Large portion of cheesy seasoned crispy tater tots', 4.24, 1.25, 'Tater Tots'),
 (@sub_sides, 'Small Spicy Tater Tots', 'Small portion of spicy seasoned crispy tater tots', 3.59, 1.00, 'Tater Tots'),
 (@sub_sides, 'Large Spicy Tater Tots', 'Large portion of spicy seasoned crispy tater tots', 4.54, 1.35, 'Tater Tots'),
 (@sub_sides, 'Small Bacon Tater Tots', 'Small portion of bacon seasoned crispy tater tots', 3.29, 0.90, 'Tater Tots'),
 (@sub_sides, 'Large Bacon Tater Tots', 'Large portion of bacon seasoned crispy tater tots', 4.24, 1.25, 'Tater Tots'),
 (@sub_sides, 'Small Ranch Tater Tots', 'Small portion of ranch seasoned crispy tater tots', 3.59, 1.00, 'Tater Tots'),
 (@sub_sides, 'Large Ranch Tater Tots', 'Large portion of ranch seasoned crispy tater tots', 4.54, 1.35, 'Tater Tots'),
 (@sub_sides, 'Ranch Dip', 'Individual portion of ranch dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Honey Mustard Dip', 'Individual portion of honey mustard dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'BBQ Dip', 'Individual portion of bbq dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Cheese Sauce Dip', 'Individual portion of cheese sauce dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Marinara Dip', 'Individual portion of marinara dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Chipotle Mayo Dip', 'Individual portion of chipotle mayo dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Buffalo Dip', 'Individual portion of buffalo dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Ketchup Cup', 'Individual portion of ketchup cup dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Sriracha Mayo Dip', 'Individual portion of sriracha mayo dipping sauce', 0.79, 0.20, 'Dips'),
 (@sub_sides, 'Garlic Aioli Dip', 'Individual portion of garlic aioli dipping sauce', 0.79, 0.20, 'Dips');

-- ---- Snacking (+91) ----
INSERT INTO menu_items (category_id, name, description, price, cost, section) VALUES
 (@sub_snacking, 'Spicy Mozzarella Sticks', 'Mozzarella Sticks tossed in spicy seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'BBQ Mozzarella Sticks', 'Mozzarella Sticks tossed in bbq seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Mozzarella Sticks', 'Mozzarella Sticks tossed in garlic parmesan seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Buffalo Mozzarella Sticks', 'Mozzarella Sticks tossed in buffalo seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Mozzarella Sticks', 'Mozzarella Sticks tossed in honey mustard seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Spicy Onion Rings', 'Onion Rings tossed in spicy seasoning', 5.49, 2.10, 'Snacks'),
 (@sub_snacking, 'BBQ Onion Rings', 'Onion Rings tossed in bbq seasoning', 5.49, 2.10, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Onion Rings', 'Onion Rings tossed in garlic parmesan seasoning', 5.49, 2.10, 'Snacks'),
 (@sub_snacking, 'Buffalo Onion Rings', 'Onion Rings tossed in buffalo seasoning', 5.49, 2.10, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Onion Rings', 'Onion Rings tossed in honey mustard seasoning', 5.49, 2.10, 'Snacks'),
 (@sub_snacking, 'Spicy French Fries', 'French Fries tossed in spicy seasoning', 5.09, 1.70, 'Snacks'),
 (@sub_snacking, 'BBQ French Fries', 'French Fries tossed in bbq seasoning', 5.09, 1.70, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan French Fries', 'French Fries tossed in garlic parmesan seasoning', 5.09, 1.70, 'Snacks'),
 (@sub_snacking, 'Buffalo French Fries', 'French Fries tossed in buffalo seasoning', 5.09, 1.70, 'Snacks'),
 (@sub_snacking, 'Honey Mustard French Fries', 'French Fries tossed in honey mustard seasoning', 5.09, 1.70, 'Snacks'),
 (@sub_snacking, 'Spicy Pretzel Poppers', 'Pretzel Poppers tossed in spicy seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'BBQ Pretzel Poppers', 'Pretzel Poppers tossed in bbq seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Pretzel Poppers', 'Pretzel Poppers tossed in garlic parmesan seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Buffalo Pretzel Poppers', 'Pretzel Poppers tossed in buffalo seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Pretzel Poppers', 'Pretzel Poppers tossed in honey mustard seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Spicy Chicken Tenders', 'Chicken Tenders tossed in spicy seasoning', 6.39, 3.00, 'Snacks'),
 (@sub_snacking, 'BBQ Chicken Tenders', 'Chicken Tenders tossed in bbq seasoning', 6.39, 3.00, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Chicken Tenders', 'Chicken Tenders tossed in garlic parmesan seasoning', 6.39, 3.00, 'Snacks'),
 (@sub_snacking, 'Buffalo Chicken Tenders', 'Chicken Tenders tossed in buffalo seasoning', 6.39, 3.00, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Chicken Tenders', 'Chicken Tenders tossed in honey mustard seasoning', 6.39, 3.00, 'Snacks'),
 (@sub_snacking, 'Spicy Loaded Fries', 'Loaded Fries tossed in spicy seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'BBQ Loaded Fries', 'Loaded Fries tossed in bbq seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Loaded Fries', 'Loaded Fries tossed in garlic parmesan seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Buffalo Loaded Fries', 'Loaded Fries tossed in buffalo seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Loaded Fries', 'Loaded Fries tossed in honey mustard seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Jalapeno Poppers', 'Classic jalapeno poppers', 5.79, 2.40, 'Snacks'),
 (@sub_snacking, 'Spicy Jalapeno Poppers', 'Jalapeno Poppers tossed in spicy seasoning', 5.79, 2.40, 'Snacks'),
 (@sub_snacking, 'BBQ Jalapeno Poppers', 'Jalapeno Poppers tossed in bbq seasoning', 5.79, 2.40, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Jalapeno Poppers', 'Jalapeno Poppers tossed in garlic parmesan seasoning', 5.79, 2.40, 'Snacks'),
 (@sub_snacking, 'Buffalo Jalapeno Poppers', 'Jalapeno Poppers tossed in buffalo seasoning', 5.79, 2.40, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Jalapeno Poppers', 'Jalapeno Poppers tossed in honey mustard seasoning', 5.79, 2.40, 'Snacks'),
 (@sub_snacking, 'Fried Pickles', 'Classic fried pickles', 5.59, 2.20, 'Snacks'),
 (@sub_snacking, 'Spicy Fried Pickles', 'Fried Pickles tossed in spicy seasoning', 5.59, 2.20, 'Snacks'),
 (@sub_snacking, 'BBQ Fried Pickles', 'Fried Pickles tossed in bbq seasoning', 5.59, 2.20, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Fried Pickles', 'Fried Pickles tossed in garlic parmesan seasoning', 5.59, 2.20, 'Snacks'),
 (@sub_snacking, 'Buffalo Fried Pickles', 'Fried Pickles tossed in buffalo seasoning', 5.59, 2.20, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Fried Pickles', 'Fried Pickles tossed in honey mustard seasoning', 5.59, 2.20, 'Snacks'),
 (@sub_snacking, 'Zucchini Sticks', 'Classic zucchini sticks', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Spicy Zucchini Sticks', 'Zucchini Sticks tossed in spicy seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'BBQ Zucchini Sticks', 'Zucchini Sticks tossed in bbq seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Zucchini Sticks', 'Zucchini Sticks tossed in garlic parmesan seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Buffalo Zucchini Sticks', 'Zucchini Sticks tossed in buffalo seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Zucchini Sticks', 'Zucchini Sticks tossed in honey mustard seasoning', 5.69, 2.30, 'Snacks'),
 (@sub_snacking, 'Fried Ravioli', 'Classic fried ravioli', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Spicy Fried Ravioli', 'Fried Ravioli tossed in spicy seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'BBQ Fried Ravioli', 'Fried Ravioli tossed in bbq seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Fried Ravioli', 'Fried Ravioli tossed in garlic parmesan seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Buffalo Fried Ravioli', 'Fried Ravioli tossed in buffalo seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Fried Ravioli', 'Fried Ravioli tossed in honey mustard seasoning', 5.89, 2.50, 'Snacks'),
 (@sub_snacking, 'Chicken Wings', 'Classic chicken wings', 6.59, 3.20, 'Snacks'),
 (@sub_snacking, 'Spicy Chicken Wings', 'Chicken Wings tossed in spicy seasoning', 6.59, 3.20, 'Snacks'),
 (@sub_snacking, 'BBQ Chicken Wings', 'Chicken Wings tossed in bbq seasoning', 6.59, 3.20, 'Snacks'),
 (@sub_snacking, 'Garlic Parmesan Chicken Wings', 'Chicken Wings tossed in garlic parmesan seasoning', 6.59, 3.20, 'Snacks'),
 (@sub_snacking, 'Buffalo Chicken Wings', 'Chicken Wings tossed in buffalo seasoning', 6.59, 3.20, 'Snacks'),
 (@sub_snacking, 'Honey Mustard Chicken Wings', 'Chicken Wings tossed in honey mustard seasoning', 6.59, 3.20, 'Snacks'),
 (@sub_snacking, 'Small Soft Pretzel', 'Small portion of soft pretzel', 3.99, 1.40, 'Sweet & Savory'),
 (@sub_snacking, 'Large Soft Pretzel', 'Large portion of soft pretzel', 5.19, 1.80, 'Sweet & Savory'),
 (@sub_snacking, 'Small Cheese Curds', 'Small portion of cheese curds', 4.84, 2.25, 'Sweet & Savory'),
 (@sub_snacking, 'Large Cheese Curds', 'Large portion of cheese curds', 6.04, 2.65, 'Sweet & Savory'),
 (@sub_snacking, 'Small Popcorn Chicken', 'Small portion of popcorn chicken', 5.09, 2.50, 'Sweet & Savory'),
 (@sub_snacking, 'Large Popcorn Chicken', 'Large portion of popcorn chicken', 6.29, 2.90, 'Sweet & Savory'),
 (@sub_snacking, 'Small Chicken Nuggets', 'Small portion of chicken nuggets', 4.99, 2.40, 'Sweet & Savory'),
 (@sub_snacking, 'Chicken Nuggets', 'Regular portion of chicken nuggets', 5.49, 2.55, 'Sweet & Savory'),
 (@sub_snacking, 'Large Chicken Nuggets', 'Large portion of chicken nuggets', 6.19, 2.80, 'Sweet & Savory'),
 (@sub_snacking, 'Small Waffle Fries', 'Small portion of waffle fries', 4.29, 1.70, 'Sweet & Savory'),
 (@sub_snacking, 'Waffle Fries', 'Regular portion of waffle fries', 4.79, 1.85, 'Sweet & Savory'),
 (@sub_snacking, 'Large Waffle Fries', 'Large portion of waffle fries', 5.49, 2.10, 'Sweet & Savory'),
 (@sub_snacking, 'Small Curly Fries', 'Small portion of curly fries', 4.29, 1.70, 'Sweet & Savory'),
 (@sub_snacking, 'Curly Fries', 'Regular portion of curly fries', 4.79, 1.85, 'Sweet & Savory'),
 (@sub_snacking, 'Large Curly Fries', 'Large portion of curly fries', 5.49, 2.10, 'Sweet & Savory'),
 (@sub_snacking, 'Small Sweet Potato Fries', 'Small portion of sweet potato fries', 4.39, 1.80, 'Sweet & Savory'),
 (@sub_snacking, 'Sweet Potato Fries', 'Regular portion of sweet potato fries', 4.89, 1.95, 'Sweet & Savory'),
 (@sub_snacking, 'Large Sweet Potato Fries', 'Large portion of sweet potato fries', 5.59, 2.20, 'Sweet & Savory'),
 (@sub_snacking, 'Small Loaded Tots', 'Small portion of loaded tots', 4.59, 2.00, 'Sweet & Savory'),
 (@sub_snacking, 'Loaded Tots', 'Regular portion of loaded tots', 5.09, 2.15, 'Sweet & Savory'),
 (@sub_snacking, 'Large Loaded Tots', 'Large portion of loaded tots', 5.79, 2.40, 'Sweet & Savory'),
 (@sub_snacking, 'Small Fried Cheese Bites', 'Small portion of fried cheese bites', 4.69, 2.10, 'Sweet & Savory'),
 (@sub_snacking, 'Fried Cheese Bites', 'Regular portion of fried cheese bites', 5.19, 2.25, 'Sweet & Savory'),
 (@sub_snacking, 'Large Fried Cheese Bites', 'Large portion of fried cheese bites', 5.89, 2.50, 'Sweet & Savory'),
 (@sub_snacking, 'Small Breaded Shrimp', 'Small portion of breaded shrimp', 5.49, 2.90, 'Sweet & Savory'),
 (@sub_snacking, 'Breaded Shrimp', 'Regular portion of breaded shrimp', 5.99, 3.05, 'Sweet & Savory'),
 (@sub_snacking, 'Large Breaded Shrimp', 'Large portion of breaded shrimp', 6.69, 3.30, 'Sweet & Savory'),
 (@sub_snacking, 'Small Cauliflower Bites', 'Small portion of cauliflower bites', 4.49, 1.90, 'Sweet & Savory'),
 (@sub_snacking, 'Cauliflower Bites', 'Regular portion of cauliflower bites', 4.99, 2.05, 'Sweet & Savory'),
 (@sub_snacking, 'Large Cauliflower Bites', 'Large portion of cauliflower bites', 5.69, 2.30, 'Sweet & Savory'),
 (@sub_snacking, 'Small Veggie Fritters', 'Small portion of veggie fritters', 4.39, 1.80, 'Sweet & Savory');

-- ---- Retail: Coffee & Tea / Mugs & Drinkware / Gift Sets & Holiday / Grab & Go Treats / Gift Cards & More ----
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

-- ---- Local: Local Items / Catering Menu / Charity, Deposits & Pup Cups ----
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

INSERT INTO sizes (name, price_delta, sort_order) VALUES
 ('Small', 0.00, 1),
 ('Medium', 0.50, 2),
 ('Large', 1.00, 3),
 ('Extra Large', 1.50, 4);

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

-- ============ OPERATIONS / FUNCTIONS MODULE ============
-- Added to back the Functions tab + Manager sidebar (till management, drawer/safe
-- transactions, app-wide toggle settings, notifications, training log, batch ops,
-- order flags for DT/OTG workflows, and barcode lookup for retail items).

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS tills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS till_assignments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    till_id INT NOT NULL,
    employee_id INT NOT NULL,
    register_name VARCHAR(50),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMP NULL,
    FOREIGN KEY (till_id) REFERENCES tills(id),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS till_counts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    till_id INT NOT NULL,
    counted_amount DECIMAL(10,2) NOT NULL,
    expected_amount DECIMAL(10,2),
    variance DECIMAL(10,2),
    counted_by INT NULL,
    counted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(255),
    FOREIGN KEY (till_id) REFERENCES tills(id),
    FOREIGN KEY (counted_by) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS drawer_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('PAID_IN','PAID_OUT','CASH_PULL','NO_SALE') NOT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    reason VARCHAR(255),
    employee_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS safe_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('OPEN','ADD_FUNDS','COUNT','CLOSE','DEPOSIT') NOT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    employee_id INT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS employee_training (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    topic VARCHAR(100) NOT NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS batch_operations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    item_count INT NOT NULL DEFAULT 0,
    initiated_by INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (initiated_by) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS order_flags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    flag ENUM('READY','RECALLED') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
