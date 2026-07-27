package com.possystem.tools;

import com.possystem.config.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

/**
 * One-off seeding utility: generates recipe_steps (prep instructions, in en/es/fr/zh) for
 * every active menu item, and recipe_ingredients links (for costing/inventory) for every
 * food & beverage item (retail merchandise items get handling steps instead of a culinary
 * recipe, and are skipped for ingredient linking).
 *
 * Content is template-driven per "family" (derived from each category's station), with the
 * item name interpolated in, and a small amount of per-item variation so items in the same
 * family don't read as 100% identical. This mirrors the earlier SeedEmployees/SeedPayroll/
 * SeedDeliveries approach: programmatically generated but structurally realistic data.
 *
 * Usage: java -cp target/classes;lib/mysql-connector-j-*.jar com.possystem.tools.SeedRecipes
 */
public class SeedRecipes {

    private static final String[] LANGS = {"en", "es", "fr", "zh"};
    private static final Random RND = new Random(4242);

    // ---- Ingredient catalog additions: name -> unit ----
    private static final String[][] NEW_INGREDIENTS = {
        {"Oat Milk", "ml"}, {"Almond Milk", "ml"}, {"Vanilla Syrup", "ml"}, {"Caramel Syrup", "ml"},
        {"Hazelnut Syrup", "ml"}, {"Whipped Cream", "g"}, {"Cinnamon Powder", "g"}, {"Cocoa Powder", "g"},
        {"Honey", "ml"}, {"Lemon Slice", "unit"}, {"Sugar", "g"}, {"Freeze-Dried Fruit Inclusions", "g"},
        {"Coconut Milk", "ml"}, {"Ice", "g"}, {"Chocolate Drizzle", "ml"}, {"Sparkling Water", "ml"},
        {"Flavor Syrup", "ml"}, {"Cream Cheese", "g"}, {"Butter", "g"}, {"Powdered Sugar", "g"},
        {"Bacon", "g"}, {"Egg Patty", "unit"}, {"Lettuce", "g"}, {"Tomato Slices", "unit"},
        {"Rice", "g"}, {"Grilled Chicken", "g"}, {"Ranch Dressing", "ml"}, {"Black Beans", "g"},
        {"Soup Base", "ml"}, {"Local Pastry Dough", "unit"}, {"Catering Tray", "unit"}
    };

    // ---- Per-family ingredient pools: [core ingredient names..., extra pool names...] ----
    private static final Map<String, String[]> FAMILY_CORE = new LinkedHashMap<>();
    private static final Map<String, String[]> FAMILY_POOL = new LinkedHashMap<>();
    static {
        FAMILY_CORE.put("COFFEE_BAR", new String[]{"Espresso Beans", "Milk"});
        FAMILY_POOL.put("COFFEE_BAR", new String[]{"Oat Milk", "Almond Milk", "Vanilla Syrup", "Caramel Syrup", "Hazelnut Syrup", "Whipped Cream", "Cinnamon Powder", "Cocoa Powder"});

        FAMILY_CORE.put("TEA_STATION", new String[]{"Tea Bags"});
        FAMILY_POOL.put("TEA_STATION", new String[]{"Honey", "Milk", "Lemon Slice", "Sugar"});

        FAMILY_CORE.put("REFRESHER_BAR", new String[]{"Lemonade Base"});
        FAMILY_POOL.put("REFRESHER_BAR", new String[]{"Freeze-Dried Fruit Inclusions", "Coconut Milk", "Ice"});

        FAMILY_CORE.put("FROZEN_STATION", new String[]{"Frozen Coffee Base"});
        FAMILY_POOL.put("FROZEN_STATION", new String[]{"Whipped Cream", "Chocolate Drizzle", "Ice"});

        FAMILY_CORE.put("BEVERAGES_GENERIC", new String[]{"Milk"});
        FAMILY_POOL.put("BEVERAGES_GENERIC", new String[]{"Flavor Syrup", "Ice", "Sparkling Water"});

        FAMILY_CORE.put("BAKERY", new String[]{});
        FAMILY_POOL.put("BAKERY", new String[]{"Plain Bagel Dough", "Blueberry Muffin Batter", "Glazed Donut Base", "Chocolate Frosted Donut Base", "Cream Cheese", "Butter", "Powdered Sugar"});

        FAMILY_CORE.put("SANDWICH_STATION", new String[]{"Bread"});
        FAMILY_POOL.put("SANDWICH_STATION", new String[]{"Turkey Slices", "Cheese Slices", "Bacon", "Egg Patty", "Hash Brown Patty", "Lettuce", "Tomato Slices"});

        FAMILY_CORE.put("SNACK_STATION", new String[]{});
        FAMILY_POOL.put("SNACK_STATION", new String[]{"Tortilla Wrap", "Mixed Vegetables", "Rice", "Grilled Chicken", "Ranch Dressing", "Black Beans"});

        FAMILY_CORE.put("LOCAL", new String[]{});
        FAMILY_POOL.put("LOCAL", new String[]{"Soup Base", "Local Pastry Dough", "Catering Tray"});

        // RETAIL intentionally has no ingredient pool - merchandise, not linked to ingredients.
    }

    // ---- Step templates: family -> lang -> steps (with {ITEM} placeholder) ----
    private static final Map<String, Map<String, String[]>> TEMPLATES = new LinkedHashMap<>();
    static {
        addFamily("COFFEE_BAR",
            new String[]{
                "Grind fresh beans and pull the shots needed for the {ITEM}.",
                "Steam or pour milk as required for the {ITEM}.",
                "Combine the espresso, milk, and any syrups called for in the {ITEM}.",
                "Top, garnish, and label the {ITEM} before handing it off."
            },
            new String[]{
                "Muele granos frescos y prepara los shots necesarios para el {ITEM}.",
                "Vaporiza o vierte la leche según se requiera para el {ITEM}.",
                "Combina el espresso, la leche y los jarabes indicados para el {ITEM}.",
                "Decora, tapa y etiqueta el {ITEM} antes de entregarlo."
            },
            new String[]{
                "Moulez des grains frais et tirez les shots nécessaires pour le {ITEM}.",
                "Faites chauffer ou versez le lait requis pour le {ITEM}.",
                "Combinez l'espresso, le lait et les sirops indiqués pour le {ITEM}.",
                "Décorez, fermez et étiquetez le {ITEM} avant de le remettre."
            },
            new String[]{
                "研磨新鲜咖啡豆，萃取{ITEM}所需的浓缩咖啡。",
                "根据{ITEM}的要求蒸煮或倒入牛奶。",
                "将浓缩咖啡、牛奶和{ITEM}所需的糖浆混合。",
                "为{ITEM}加盖、装饰并贴标签后即可交付。"
            });

        addFamily("TEA_STATION",
            new String[]{
                "Select the tea base called for in the {ITEM} and steep to the correct temperature and time.",
                "Add any milk, sweetener, or flavoring the {ITEM} requires.",
                "Strain, cap, and label the {ITEM}."
            },
            new String[]{
                "Selecciona la base de té indicada para el {ITEM} y déjala reposar a la temperatura y el tiempo correctos.",
                "Añade la leche, el endulzante o el saborizante que requiera el {ITEM}.",
                "Cuela, tapa y etiqueta el {ITEM}."
            },
            new String[]{
                "Choisissez la base de thé indiquée pour le {ITEM} et laissez infuser à la bonne température et durée.",
                "Ajoutez le lait, l'édulcorant ou l'arôme requis pour le {ITEM}.",
                "Filtrez, fermez et étiquetez le {ITEM}."
            },
            new String[]{
                "选择{ITEM}所需的茶底，按正确的温度和时间泡制。",
                "根据{ITEM}的要求加入牛奶、甜味剂或调味料。",
                "过滤、加盖并为{ITEM}贴标签。"
            });

        addFamily("REFRESHER_BAR",
            new String[]{
                "Shake the base and fruit inclusions called for in the {ITEM} over ice.",
                "Add any juice or flavoring the {ITEM} requires.",
                "Pour, top with ice, and label the {ITEM}."
            },
            new String[]{
                "Agita la base y las frutas indicadas para el {ITEM} con hielo.",
                "Añade el jugo o saborizante que requiera el {ITEM}.",
                "Sirve, agrega hielo y etiqueta el {ITEM}."
            },
            new String[]{
                "Secouez la base et les fruits indiqués pour le {ITEM} avec de la glace.",
                "Ajoutez le jus ou l'arôme requis pour le {ITEM}.",
                "Versez, ajoutez de la glace et étiquetez le {ITEM}."
            },
            new String[]{
                "将{ITEM}所需的基底和水果配料与冰混合摇匀。",
                "根据{ITEM}的要求加入果汁或调味料。",
                "倒入杯中，加冰并贴上{ITEM}的标签。"
            });

        addFamily("FROZEN_STATION",
            new String[]{
                "Add the frozen base and flavoring called for in the {ITEM} to the blender.",
                "Blend the {ITEM} until smooth and the right consistency.",
                "Pour, add toppings, and label the {ITEM}."
            },
            new String[]{
                "Agrega la base congelada y el saborizante indicados para el {ITEM} a la licuadora.",
                "Licúa el {ITEM} hasta lograr una consistencia uniforme.",
                "Sirve, añade los toppings y etiqueta el {ITEM}."
            },
            new String[]{
                "Ajoutez la base glacée et l'arôme indiqués pour le {ITEM} dans le mixeur.",
                "Mixez le {ITEM} jusqu'à obtenir la bonne consistance.",
                "Versez, ajoutez les garnitures et étiquetez le {ITEM}."
            },
            new String[]{
                "将{ITEM}所需的冰冻基底和调味料放入搅拌机。",
                "搅拌{ITEM}至顺滑并达到合适的稠度。",
                "倒出、加上配料并贴上{ITEM}的标签。"
            });

        addFamily("BEVERAGES_GENERIC",
            new String[]{
                "Gather the ingredients needed for the {ITEM}.",
                "Prepare the {ITEM} following the standard beverage build.",
                "Finish, cap, and label the {ITEM} before serving."
            },
            new String[]{
                "Reúne los ingredientes necesarios para el {ITEM}.",
                "Prepara el {ITEM} siguiendo el procedimiento estándar de bebidas.",
                "Termina, tapa y etiqueta el {ITEM} antes de servir."
            },
            new String[]{
                "Rassemblez les ingrédients nécessaires pour le {ITEM}.",
                "Préparez le {ITEM} selon la procédure standard des boissons.",
                "Terminez, fermez et étiquetez le {ITEM} avant de servir."
            },
            new String[]{
                "准备{ITEM}所需的原料。",
                "按照标准饮品制作流程制作{ITEM}。",
                "完成后加盖并为{ITEM}贴标签，随后即可出品。"
            });

        addFamily("BAKERY",
            new String[]{
                "Pull the {ITEM} from the case or warm it per station guidelines.",
                "Slice, toast, or plate the {ITEM} as requested.",
                "Add any spread, filling, or topping the {ITEM} calls for.",
                "Wrap or plate and label the {ITEM} for service."
            },
            new String[]{
                "Saca el {ITEM} de la vitrina o caliéntalo según las indicaciones de la estación.",
                "Corta, tuesta o emplata el {ITEM} según se solicite.",
                "Añade el untable, relleno o cobertura que requiera el {ITEM}.",
                "Envuelve o emplata y etiqueta el {ITEM} para servirlo."
            },
            new String[]{
                "Sortez le {ITEM} de la vitrine ou réchauffez-le selon les consignes du poste.",
                "Tranchez, faites griller ou dressez le {ITEM} selon la demande.",
                "Ajoutez la garniture, la tartinade ou la couverture requise pour le {ITEM}.",
                "Emballez ou dressez et étiquetez le {ITEM} avant le service."
            },
            new String[]{
                "从展示柜中取出{ITEM}，或按工作台标准加热。",
                "根据要求切片、烘烤或装盘{ITEM}。",
                "加入{ITEM}所需的涂抹酱、馅料或配料。",
                "包装或装盘并贴标签，准备出品{ITEM}。"
            });

        addFamily("SANDWICH_STATION",
            new String[]{
                "Toast or grill the bread or base called for in the {ITEM}.",
                "Cook or warm the protein and fillings for the {ITEM}.",
                "Assemble the {ITEM} in the order listed on the build card.",
                "Cut, wrap, and label the {ITEM} for pickup."
            },
            new String[]{
                "Tuesta o asa el pan o la base indicados para el {ITEM}.",
                "Cocina o calienta la proteína y los rellenos del {ITEM}.",
                "Arma el {ITEM} en el orden indicado en la tarjeta de preparación.",
                "Corta, envuelve y etiqueta el {ITEM} para su entrega."
            },
            new String[]{
                "Faites griller le pain ou la base indiqués pour le {ITEM}.",
                "Cuisez ou réchauffez la protéine et la garniture du {ITEM}.",
                "Assemblez le {ITEM} dans l'ordre indiqué sur la fiche de préparation.",
                "Coupez, emballez et étiquetez le {ITEM} pour le service."
            },
            new String[]{
                "烘烤或煎烤{ITEM}所需的面包或底料。",
                "烹饪或加热{ITEM}的蛋白质和馅料。",
                "按照制作卡上的顺序组装{ITEM}。",
                "切好、包装并贴标签，准备取餐{ITEM}。"
            });

        addFamily("SNACK_STATION",
            new String[]{
                "Prep the base (rice, greens, or wrap) called for in the {ITEM}.",
                "Add the proteins and vegetables listed for the {ITEM}.",
                "Add sauce or dressing and assemble the {ITEM}.",
                "Fold, box, or bag and label the {ITEM}."
            },
            new String[]{
                "Prepara la base (arroz, verduras o tortilla) indicada para el {ITEM}.",
                "Añade las proteínas y verduras indicadas para el {ITEM}.",
                "Agrega la salsa o aderezo y arma el {ITEM}.",
                "Enrolla, empaca y etiqueta el {ITEM}."
            },
            new String[]{
                "Préparez la base (riz, légumes verts ou galette) indiquée pour le {ITEM}.",
                "Ajoutez les protéines et légumes indiqués pour le {ITEM}.",
                "Ajoutez la sauce ou la vinaigrette et assemblez le {ITEM}.",
                "Pliez, emballez et étiquetez le {ITEM}."
            },
            new String[]{
                "准备{ITEM}所需的底料（米饭、蔬菜或卷饼皮）。",
                "加入{ITEM}所需的蛋白质和蔬菜。",
                "加入酱汁并组装{ITEM}。",
                "卷好、装盒或装袋并贴标签，完成{ITEM}。"
            });

        addFamily("LOCAL",
            new String[]{
                "Confirm the quantity and any special instructions for the {ITEM}.",
                "Prepare or portion the {ITEM} per the local recipe card.",
                "Package and label the {ITEM} for pickup or delivery."
            },
            new String[]{
                "Confirma la cantidad y cualquier instrucción especial para el {ITEM}.",
                "Prepara o porciona el {ITEM} según la tarjeta de receta local.",
                "Empaca y etiqueta el {ITEM} para recogida o entrega."
            },
            new String[]{
                "Confirmez la quantité et toute instruction spéciale pour le {ITEM}.",
                "Préparez ou portionnez le {ITEM} selon la fiche de recette locale.",
                "Emballez et étiquetez le {ITEM} pour le retrait ou la livraison."
            },
            new String[]{
                "确认{ITEM}的数量及任何特殊要求。",
                "根据本地配方卡准备或分装{ITEM}。",
                "包装并贴标签，准备取货或配送{ITEM}。"
            });

        addFamily("RETAIL",
            new String[]{
                "Check that the {ITEM} is in stock and undamaged.",
                "Scan or confirm the price tag on the {ITEM}.",
                "Bag or box the {ITEM} for the customer."
            },
            new String[]{
                "Verifica que el {ITEM} esté en existencia y sin daños.",
                "Escanea o confirma la etiqueta de precio del {ITEM}.",
                "Empaca el {ITEM} en bolsa o caja para el cliente."
            },
            new String[]{
                "Vérifiez que le {ITEM} est en stock et en bon état.",
                "Scannez ou confirmez l'étiquette de prix du {ITEM}.",
                "Emballez le {ITEM} dans un sac ou une boîte pour le client."
            },
            new String[]{
                "检查{ITEM}是否有库存且完好无损。",
                "扫描或确认{ITEM}的价格标签。",
                "为顾客将{ITEM}装袋或装盒。"
            });
    }

    private static void addFamily(String key, String[] en, String[] es, String[] fr, String[] zh) {
        Map<String, String[]> m = new LinkedHashMap<>();
        m.put("en", en);
        m.put("es", es);
        m.put("fr", fr);
        m.put("zh", zh);
        TEMPLATES.put(key, m);
    }

    public static void main(String[] args) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            Map<Integer, String> stationById = loadCategoryStations(conn);
            Map<String, Integer> ingredientIdByName = ensureIngredients(conn);

            List<int[]> ignored = null; // placeholder to keep structure simple
            List<Object[]> items = loadMenuItems(conn); // [id, name, categoryId]
            System.out.println("Menu items to process: " + items.size());

            int stepsInserted = seedSteps(conn, items, stationById);
            System.out.println("Recipe steps inserted: " + stepsInserted);

            int linksInserted = seedIngredientLinks(conn, items, stationById, ingredientIdByName);
            System.out.println("Recipe ingredient links inserted: " + linksInserted);

            System.out.println("DONE.");
        }
    }

    private static Map<Integer, String> loadCategoryStations(Connection conn) throws SQLException {
        Map<Integer, String> map = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, station FROM categories")) {
            while (rs.next()) map.put(rs.getInt(1), rs.getString(2));
        }
        return map;
    }

    private static List<Object[]> loadMenuItems(Connection conn) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, category_id FROM menu_items WHERE active = 1")) {
            while (rs.next()) list.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3)});
        }
        return list;
    }

    private static Map<String, Integer> ensureIngredients(Connection conn) throws SQLException {
        Map<String, Integer> byName = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM ingredients")) {
            while (rs.next()) byName.put(rs.getString(2), rs.getInt(1));
        }
        String insertSql = "INSERT INTO ingredients (name, unit, stock_quantity, low_stock_threshold, unit_cost) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            for (String[] ing : NEW_INGREDIENTS) {
                String name = ing[0];
                if (byName.containsKey(name)) continue;
                String unit = ing[1];
                double stock = 500 + RND.nextInt(1500);
                double threshold = 50 + RND.nextInt(100);
                double cost = round2(0.02 + RND.nextDouble() * 2.0);
                ps.setString(1, name);
                ps.setString(2, unit);
                ps.setBigDecimal(3, BigDecimal.valueOf(stock).setScale(2, RoundingMode.HALF_UP));
                ps.setBigDecimal(4, BigDecimal.valueOf(threshold).setScale(2, RoundingMode.HALF_UP));
                ps.setBigDecimal(5, BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) byName.put(name, keys.getInt(1));
                }
            }
        }
        return byName;
    }

    private static String familyForStation(String station) {
        if (station == null) return "BEVERAGES_GENERIC";
        switch (station) {
            case "Coffee Bar": return "COFFEE_BAR";
            case "Tea Station": return "TEA_STATION";
            case "Refresher Bar": return "REFRESHER_BAR";
            case "Frozen Station": return "FROZEN_STATION";
            case "Bakery": return "BAKERY";
            case "Sandwich Station": return "SANDWICH_STATION";
            case "Snack Station": return "SNACK_STATION";
            case "Local": return "LOCAL";
            case "Retail": return "RETAIL";
            case "Beverages":
            default: return "BEVERAGES_GENERIC";
        }
    }

    private static int seedSteps(Connection conn, List<Object[]> items, Map<Integer, String> stationById) throws SQLException {
        String deleteSql = "DELETE FROM recipe_steps WHERE menu_item_id = ? AND language = ?";
        String insertSql = "INSERT INTO recipe_steps (menu_item_id, language, step_number, instruction) VALUES (?,?,?,?)";
        int count = 0;
        try (PreparedStatement del = conn.prepareStatement(deleteSql);
             PreparedStatement ins = conn.prepareStatement(insertSql)) {
            conn.setAutoCommit(false);
            int sinceCommit = 0;
            for (Object[] item : items) {
                int id = (Integer) item[0];
                String name = (String) item[1];
                int catId = (Integer) item[2];
                String family = familyForStation(stationById.get(catId));
                Map<String, String[]> byLang = TEMPLATES.get(family);

                for (String lang : LANGS) {
                    del.setInt(1, id);
                    del.setString(2, lang);
                    del.executeUpdate();

                    String[] steps = byLang.get(lang);
                    for (int i = 0; i < steps.length; i++) {
                        String text = steps[i].replace("{ITEM}", name);
                        ins.setInt(1, id);
                        ins.setString(2, lang);
                        ins.setInt(3, i + 1);
                        ins.setString(4, text);
                        ins.addBatch();
                        count++;
                        sinceCommit++;
                    }
                }
                if (sinceCommit >= 800) {
                    ins.executeBatch();
                    conn.commit();
                    sinceCommit = 0;
                }
            }
            ins.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
        return count;
    }

    private static int seedIngredientLinks(Connection conn, List<Object[]> items, Map<Integer, String> stationById,
                                            Map<String, Integer> ingredientIdByName) throws SQLException {
        // Clear existing auto-generated links for a clean re-run (keep it simple: wipe and rebuild all).
        String insertSql = "INSERT INTO recipe_ingredients (menu_item_id, ingredient_id, quantity_required) VALUES (?,?,?)";
        int count = 0;
        try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
            conn.setAutoCommit(false);
            int sinceCommit = 0;
            for (Object[] item : items) {
                int id = (Integer) item[0];
                int catId = (Integer) item[2];
                String station = stationById.get(catId);
                String family = familyForStation(station);
                if ("RETAIL".equals(family)) continue; // merchandise - no ingredient consumption

                String[] core = FAMILY_CORE.getOrDefault(family, new String[0]);
                String[] pool = FAMILY_POOL.getOrDefault(family, new String[0]);

                Set<String> chosen = new LinkedHashSet<>(Arrays.asList(core));
                List<String> poolList = new ArrayList<>(Arrays.asList(pool));
                Collections.shuffle(poolList, RND);
                int extra = 1 + RND.nextInt(3); // 1-3 extra ingredients
                for (String p : poolList) {
                    if (chosen.size() - core.length >= extra) break;
                    chosen.add(p);
                }
                if (chosen.isEmpty()) continue;

                for (String ingName : chosen) {
                    Integer ingId = ingredientIdByName.get(ingName);
                    if (ingId == null) continue;
                    double qty = randomQuantityFor(ingName);
                    ins.setInt(1, id);
                    ins.setInt(2, ingId);
                    ins.setBigDecimal(3, BigDecimal.valueOf(qty).setScale(2, RoundingMode.HALF_UP));
                    ins.addBatch();
                    count++;
                    sinceCommit++;
                }
                if (sinceCommit >= 800) {
                    ins.executeBatch();
                    conn.commit();
                    sinceCommit = 0;
                }
            }
            ins.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
        return count;
    }

    private static double randomQuantityFor(String ingredientName) {
        // Rough heuristic by ingredient type; good enough for realistic-looking costing data.
        switch (ingredientName) {
            case "Milk": case "Oat Milk": case "Almond Milk": case "Coconut Milk":
            case "Lemonade Base": case "Frozen Coffee Base": case "Soup Base":
            case "Ranch Dressing": case "Sparkling Water":
                return 30 + RND.nextInt(220);
            case "Vanilla Syrup": case "Caramel Syrup": case "Hazelnut Syrup":
            case "Flavor Syrup": case "Honey": case "Chocolate Drizzle":
                return 5 + RND.nextInt(30);
            case "Espresso Beans": case "Cinnamon Powder": case "Cocoa Powder":
            case "Sugar": case "Powdered Sugar": case "Cream Cheese": case "Butter":
            case "Freeze-Dried Fruit Inclusions": case "Whipped Cream": case "Ice":
            case "Mixed Vegetables": case "Rice": case "Grilled Chicken": case "Black Beans":
            case "Turkey Slices": case "Bacon": case "Lettuce":
                return 10 + RND.nextInt(180);
            default: // unit-based items: Tea Bags, Bread, Cheese Slices, Tortilla Wrap, Hash Brown Patty,
                     // Egg Patty, Lemon Slice, Tomato Slices, dough/base items, Catering Tray, etc.
                return 1 + RND.nextInt(2);
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
