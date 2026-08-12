package com.possystem.gui;

import com.possystem.util.I18n;
import com.possystem.util.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * A Help/FAQ dialog available in all 5 supported languages. Its language dropdown drives the
 * same global {@link I18n} language used by the rest of the POS, so picking a language here
 * also switches the main screen once the dialog is closed.
 */
public class HelpDialog extends JDialog {

    // English question/answer pairs, each followed by {bn, hi, es, fr} translations in the
    // same order. Index 0 of each row = the language-neutral lookup order used below.
    private static final String[][] TOPICS_EN = {
        {"How do I add an item to an order?",
         "Tap a department tab (Beverages, Bakery, etc.), then tap the item. Choose size, temperature, and any add-ons, then tap Item Done."},
        {"How does the Beverages checkout page work?",
         "Tap the Beverages tab, then pick a section on the right (Coffee, Espresso, Teas, Refreshers, Frozen, Other Beverages) to see drinks in that group. Tapping a drink opens its customize screen: choose a Size (each size adds its own price), Hot or Iced, and any Dairy/Sweetener, Flavor Swirl, or Add-On modifiers (each adds its own price too). Tap Item Done to add it to the cart on the left — Subtotal, Tax, and Total update automatically as you add items. Enter a Discount $ amount if needed, then press PAY and choose Credit Card, CASH (enter the amount the customer hands over on the keypad first), or GC Redeem to finish the sale."},
        {"How does the Featured checkout page work?",
         "Tap the Featured tab, then pick a section on the right (New Arrivals, Fan Favorites, Limited Time Offers, Value Bundles) to browse those drinks — customization, cart, and payment work exactly like Beverages. The News & Promos button at the bottom of that sidebar isn't a drink list — it opens a read-only board of this month's promotions; tap any other sidebar section to go back to ordering."},
        {"How does the Bakery checkout page work?",
         "Tap the Bakery tab, then pick a section on the right (Sweet Treats, Grab & Go, Bagels & Muffins, Donuts & Donut Holes) to browse cookies, pastries, cakes, and more — cart, discount, and payment work exactly like Beverages. Note: the customize screen still shows a Temperature row and drink-style modifiers (Dairy/Sweetener, Flavor Swirl, Add-On) for every item, including baked goods — these are safe to ignore for items like cookies or donuts; they won't affect the order unless you tap one on purpose."},
        {"How do I apply a discount?",
         "Enter the discount amount in the Discount $ box in the cart column before pressing PAY."},
        {"How do I take a delivery order?",
         "Change Order Type to DELIVERY and enter the delivery address when prompted."},
        {"How do I redeem a gift card?",
         "Press PAY, then tap GC Redeem and enter the gift card number."},
        {"How do I change the app language?",
         "Tap the language button next to the clock at the top of the screen and choose your language."},
        {"How do I register a loyalty customer?",
         "Tap + Loyalty Customer in the cart column and enter the customer's name and phone number."},
        {"How do I cancel an order?",
         "Tap Cancel Order in the cart column, or Cancel Saved/Stored Order on the Payments screen."},
        {"Who do I contact for more help?",
         "Use the SUPPORT section under the Functions tab to call or chat with support."},
    };

    private static final String[][] TOPICS_BN = {
        {"অর্ডারে কীভাবে আইটেম যোগ করব?",
         "একটি ডিপার্টমেন্ট ট্যাবে (পানীয়, বেকারি ইত্যাদি) ট্যাপ করুন, তারপর আইটেমে ট্যাপ করুন। সাইজ, তাপমাত্রা ও অ্যাড-অন বেছে নিয়ে আইটেম সম্পন্ন-এ ট্যাপ করুন।"},
        {"POS/Checkout: Beverages পেজ কীভাবে কাজ করে?",
         "Beverages ট্যাবে ট্যাপ করুন, তারপর ডানপাশের সেকশন (Coffee, Espresso, Teas, Refreshers, Frozen, Other Beverages) থেকে একটি বেছে নিয়ে সেই গ্রুপের আইটেম দেখুন। কোনো ড্রিংকে ট্যাপ করলে কাস্টমাইজ স্ক্রিন খুলবে: Size বেছে নিন (প্রতিটি সাইজের নিজস্ব দাম যোগ হয়), Hot বা Iced বেছে নিন, এবং যেকোনো Dairy/Sweetener, Flavor Swirl বা Add-On মডিফায়ার (প্রতিটিরও নিজস্ব দাম যোগ হয়)। আইটেম সম্পন্ন-এ ট্যাপ করলে সেটি বামপাশের কার্টে যোগ হবে — আইটেম যোগ করার সাথে সাথে Subtotal, Tax ও Total স্বয়ংক্রিয়ভাবে আপডেট হয়। প্রয়োজনে Discount $ পরিমাণ লিখুন, তারপর PAY চেপে Credit Card, CASH (আগে কীপ্যাডে গ্রাহকের দেওয়া টাকার পরিমাণ লিখতে হবে), অথবা GC Redeem বেছে নিয়ে বিক্রয় সম্পন্ন করুন।"},
        {"POS/Checkout: Featured পেজ কীভাবে কাজ করে?",
         "Featured ট্যাবে ট্যাপ করুন, তারপর ডানপাশের সেকশন (New Arrivals, Fan Favorites, Limited Time Offers, Value Bundles) থেকে বেছে নিয়ে সেসব ড্রিংক দেখুন — কাস্টমাইজেশন, কার্ট ও পেমেন্ট ঠিক Beverages-এর মতোই কাজ করে। সাইডবারের নিচে থাকা News & Promos বাটন কোনো ড্রিংকের তালিকা না — এটাতে ট্যাপ করলে এই মাসের প্রোমোশনের একটা তথ্যমূলক বোর্ড খোলে (শুধু দেখার জন্য); অর্ডারে ফিরতে সাইডবারের অন্য যেকোনো সেকশনে ট্যাপ করুন।"},
        {"POS/Checkout: Bakery পেজ কীভাবে কাজ করে?",
         "Bakery ট্যাবে ট্যাপ করুন, তারপর ডানপাশের সেকশন (Sweet Treats, Grab & Go, Bagels & Muffins, Donuts & Donut Holes) থেকে বেছে নিয়ে কুকি, পেস্ট্রি, কেক ইত্যাদি দেখুন — কার্ট, ছাড় ও পেমেন্ট ঠিক Beverages-এর মতোই কাজ করে। খেয়াল রাখুন: প্রতিটি আইটেমের কাস্টমাইজ স্ক্রিনে (বেকারি আইটেমসহ) এখনো Temperature ও পানীয়-স্টাইল মডিফায়ার (Dairy/Sweetener, Flavor Swirl, Add-On) দেখানো হয় — কুকি বা ডোনাটের মতো আইটেমের জন্য এগুলো উপেক্ষা করা নিরাপদ; ইচ্ছাকৃতভাবে না চাপলে এগুলো অর্ডারে প্রভাব ফেলবে না।"},
        {"ছাড় কীভাবে প্রয়োগ করব?",
         "পেমেন্ট চাপার আগে কার্ট কলামের ছাড় $ বক্সে ছাড়ের পরিমাণ লিখুন।"},
        {"ডেলিভারি অর্ডার কীভাবে নেব?",
         "অর্ডার টাইপ পরিবর্তন করে DELIVERY করুন এবং জিজ্ঞাসা করলে ডেলিভারি ঠিকানা লিখুন।"},
        {"গিফট কার্ড কীভাবে রিডিম করব?",
         "পেমেন্ট চাপুন, তারপর গিফট কার্ড রিডিম-এ ট্যাপ করে গিফট কার্ড নম্বর লিখুন।"},
        {"অ্যাপের ভাষা কীভাবে পরিবর্তন করব?",
         "স্ক্রিনের উপরে ঘড়ির পাশে ভাষা বাটনে ট্যাপ করে আপনার ভাষা বেছে নিন।"},
        {"লয়্যালটি কাস্টমার কীভাবে যোগ করব?",
         "কার্ট কলামে + লয়্যালটি কাস্টমার-এ ট্যাপ করে গ্রাহকের নাম ও ফোন নম্বর দিন।"},
        {"অর্ডার কীভাবে বাতিল করব?",
         "কার্ট কলামে অর্ডার বাতিল, অথবা পেমেন্টস স্ক্রিনে সংরক্ষিত অর্ডার বাতিল-এ ট্যাপ করুন।"},
        {"আরও সাহায্যের জন্য কার সাথে যোগাযোগ করব?",
         "ফাংশন ট্যাবের সাপোর্ট বিভাগ ব্যবহার করে সাপোর্টে কল বা চ্যাট করুন।"},
    };

    private static final String[][] TOPICS_HI = {
        {"ऑर्डर में आइटम कैसे जोड़ें?",
         "किसी डिपार्टमेंट टैब (पेय, बेकरी आदि) पर टैप करें, फिर आइटम पर टैप करें। आकार, तापमान और ऐड-ऑन चुनें, फिर आइटम पूर्ण पर टैप करें।"},
        {"POS/Checkout: Beverages पेज कैसे काम करता है?",
         "Beverages टैब पर टैप करें, फिर दाईं ओर के सेक्शन (Coffee, Espresso, Teas, Refreshers, Frozen, Other Beverages) में से कोई एक चुनकर उस समूह की ड्रिंक्स देखें। किसी ड्रिंक पर टैप करने से कस्टमाइज़ स्क्रीन खुलती है: Size चुनें (हर आकार की अपनी कीमत जुड़ती है), Hot या Iced चुनें, और कोई भी Dairy/Sweetener, Flavor Swirl या Add-On मॉडिफायर (इनकी भी अपनी कीमत जुड़ती है)। आइटम पूर्ण पर टैप करने से यह बाईं ओर कार्ट में जुड़ जाता है — आइटम जोड़ते ही Subtotal, Tax और Total अपने आप अपडेट हो जाते हैं। ज़रूरत हो तो Discount $ राशि दर्ज करें, फिर भुगतान दबाकर Credit Card, CASH (पहले कीपैड पर ग्राहक द्वारा दी गई राशि दर्ज करें), या GC Redeem चुनकर बिक्री पूरी करें।"},
        {"POS/Checkout: Featured पेज कैसे काम करता है?",
         "Featured टैब पर टैप करें, फिर दाईं ओर के सेक्शन (New Arrivals, Fan Favorites, Limited Time Offers, Value Bundles) में से चुनकर वे ड्रिंक्स देखें — कस्टमाइज़ेशन, कार्ट और भुगतान ठीक Beverages की तरह ही काम करते हैं। साइडबार के नीचे मौजूद News & Promos बटन कोई ड्रिंक सूची नहीं है — इस पर टैप करने से इस महीने के प्रमोशन का एक सूचनात्मक बोर्ड खुलता है (सिर्फ़ देखने के लिए); ऑर्डर पर वापस जाने के लिए साइडबार के किसी अन्य सेक्शन पर टैप करें।"},
        {"POS/Checkout: Bakery पेज कैसे काम करता है?",
         "Bakery टैब पर टैप करें, फिर दाईं ओर के सेक्शन (Sweet Treats, Grab & Go, Bagels & Muffins, Donuts & Donut Holes) में से चुनकर कुकीज़, पेस्ट्री, केक आदि देखें — कार्ट, छूट और भुगतान ठीक Beverages की तरह ही काम करते हैं। ध्यान दें: हर आइटम (बेकरी आइटम सहित) की कस्टमाइज़ स्क्रीन पर अभी भी Temperature और पेय-शैली के मॉडिफायर (Dairy/Sweetener, Flavor Swirl, Add-On) दिखते हैं — कुकी या डोनट जैसी चीज़ों के लिए इन्हें नज़रअंदाज़ करना सुरक्षित है; जान-बूझकर टैप न करने पर ये ऑर्डर को प्रभावित नहीं करेंगे।"},
        {"छूट कैसे लागू करें?",
         "भुगतान दबाने से पहले कार्ट कॉलम के छूट $ बॉक्स में छूट की राशि दर्ज करें।"},
        {"डिलीवरी ऑर्डर कैसे लें?",
         "ऑर्डर प्रकार को DELIVERY में बदलें और पूछे जाने पर डिलीवरी पता दर्ज करें।"},
        {"गिफ्ट कार्ड कैसे रिडीम करें?",
         "भुगतान दबाएं, फिर गिफ्ट कार्ड रिडीम पर टैप करें और गिफ्ट कार्ड नंबर दर्ज करें।"},
        {"ऐप की भाषा कैसे बदलें?",
         "स्क्रीन के ऊपर घड़ी के बगल में भाषा बटन पर टैप करें और अपनी भाषा चुनें।"},
        {"लॉयल्टी ग्राहक कैसे पंजीकृत करें?",
         "कार्ट कॉलम में + लॉयल्टी ग्राहक पर टैप करें और ग्राहक का नाम व फ़ोन नंबर दर्ज करें।"},
        {"ऑर्डर कैसे रद्द करें?",
         "कार्ट कॉलम में ऑर्डर रद्द करें, या भुगतान स्क्रीन पर सहेजा गया ऑर्डर रद्द करें पर टैप करें।"},
        {"अधिक सहायता के लिए किससे संपर्क करें?",
         "फ़ंक्शन टैब में सहायता केंद्र अनुभाग का उपयोग करके सहायता को कॉल या चैट करें।"},
    };

    private static final String[][] TOPICS_ES = {
        {"¿Cómo agrego un artículo a un pedido?",
         "Toca una pestaña de departamento (Bebidas, Panadería, etc.), luego toca el artículo. Elige tamaño, temperatura y extras, y luego toca Artículo listo."},
        {"¿Cómo funciona la página de pago de Bebidas?",
         "Toca la pestaña Bebidas, luego elige una sección a la derecha (Café, Espresso, Tés, Refrescos, Congelados, Otras Bebidas) para ver las bebidas de ese grupo. Al tocar una bebida se abre la pantalla de personalización: elige un Tamaño (cada tamaño agrega su propio precio), Caliente o Helado, y cualquier modificador de Lácteos/Endulzante, Toque de Sabor o Extra (cada uno también agrega su propio precio). Toca Artículo listo para agregarlo al carrito de la izquierda — el Subtotal, Impuesto y Total se actualizan automáticamente al agregar artículos. Ingresa un monto de Descuento $ si es necesario, luego presiona PAGAR y elige Tarjeta de Crédito, EFECTIVO (ingresa primero en el teclado el monto que entrega el cliente), o Canjear Tarjeta de Regalo para completar la venta."},
        {"¿Cómo funciona la página de pago de Featured (Destacados)?",
         "Toca la pestaña Featured, luego elige una sección a la derecha (New Arrivals, Fan Favorites, Limited Time Offers, Value Bundles) para ver esas bebidas — la personalización, el carrito y el pago funcionan igual que en Bebidas. El botón News & Promos al final de esa barra lateral no es una lista de bebidas — abre un panel de solo lectura con las promociones del mes; toca cualquier otra sección de la barra lateral para volver a pedir."},
        {"¿Cómo funciona la página de pago de Panadería (Bakery)?",
         "Toca la pestaña Bakery, luego elige una sección a la derecha (Sweet Treats, Grab & Go, Bagels & Muffins, Donuts & Donut Holes) para ver galletas, pastelería, pasteles y más — el carrito, el descuento y el pago funcionan igual que en Bebidas. Nota: la pantalla de personalización todavía muestra una fila de Temperatura y modificadores de estilo bebida (Lácteos/Endulzante, Toque de Sabor, Extra) para cada artículo, incluidos los de panadería — es seguro ignorarlos en artículos como galletas o donas; no afectarán el pedido a menos que los toques a propósito."},
        {"¿Cómo aplico un descuento?",
         "Ingresa el monto del descuento en el cuadro Descuento $ de la columna del carrito antes de presionar PAGAR."},
        {"¿Cómo tomo un pedido a domicilio?",
         "Cambia el Tipo de pedido a DELIVERY e ingresa la dirección de entrega cuando se te solicite."},
        {"¿Cómo canjeo una tarjeta de regalo?",
         "Presiona PAGAR, luego toca Canjear tarjeta de regalo e ingresa el número de la tarjeta."},
        {"¿Cómo cambio el idioma de la aplicación?",
         "Toca el botón de idioma junto al reloj en la parte superior de la pantalla y elige tu idioma."},
        {"¿Cómo registro a un cliente de lealtad?",
         "Toca + Cliente de lealtad en la columna del carrito e ingresa el nombre y teléfono del cliente."},
        {"¿Cómo cancelo un pedido?",
         "Toca Cancelar pedido en la columna del carrito, o Cancelar pedido guardado en la pantalla de pagos."},
        {"¿A quién contacto para más ayuda?",
         "Usa la sección SOPORTE en la pestaña Funciones para llamar o chatear con soporte."},
    };

    private static final String[][] TOPICS_FR = {
        {"Comment ajouter un article à une commande ?",
         "Appuyez sur un onglet de département (Boissons, Boulangerie, etc.), puis sur l'article. Choisissez la taille, la température et les suppléments, puis appuyez sur Article terminé."},
        {"Comment fonctionne la page de paiement Boissons ?",
         "Appuyez sur l'onglet Boissons, puis choisissez une section à droite (Café, Espresso, Thés, Rafraîchissements, Surgelés, Autres Boissons) pour voir les boissons de ce groupe. Toucher une boisson ouvre l'écran de personnalisation : choisissez une Taille (chaque taille ajoute son propre prix), Chaud ou Glacé, et tout modificateur Lait/Sucre, Arôme ou Supplément (chacun ajoute aussi son propre prix). Appuyez sur Article terminé pour l'ajouter au panier à gauche — le Sous-total, la Taxe et le Total se mettent à jour automatiquement à chaque ajout. Saisissez un montant de Remise $ si nécessaire, puis appuyez sur PAYER et choisissez Carte de crédit, ESPÈCES (saisissez d'abord au clavier le montant remis par le client), ou Échanger carte-cadeau pour finaliser la vente."},
        {"Comment fonctionne la page de paiement Featured (Vedettes) ?",
         "Appuyez sur l'onglet Featured, puis choisissez une section à droite (New Arrivals, Fan Favorites, Limited Time Offers, Value Bundles) pour voir ces boissons — la personnalisation, le panier et le paiement fonctionnent exactement comme pour Boissons. Le bouton News & Promos en bas de cette barre latérale n'est pas une liste de boissons — il ouvre un panneau de lecture seule des promotions du mois ; touchez n'importe quelle autre section de la barre latérale pour revenir à la commande."},
        {"Comment fonctionne la page de paiement Bakery (Boulangerie) ?",
         "Appuyez sur l'onglet Bakery, puis choisissez une section à droite (Sweet Treats, Grab & Go, Bagels & Muffins, Donuts & Donut Holes) pour voir cookies, pâtisseries, gâteaux, etc. — le panier, la remise et le paiement fonctionnent exactement comme pour Boissons. Remarque : l'écran de personnalisation affiche encore une ligne Température et des modificateurs de style boisson (Lait/Sucre, Arôme, Supplément) pour chaque article, y compris les articles de boulangerie — il est sans risque de les ignorer pour des articles comme les cookies ou les donuts ; ils n'affecteront pas la commande sauf si vous les touchez volontairement."},
        {"Comment appliquer une remise ?",
         "Saisissez le montant de la remise dans la case Remise $ de la colonne du panier avant d'appuyer sur PAYER."},
        {"Comment prendre une commande en livraison ?",
         "Changez le Type de commande en DELIVERY et saisissez l'adresse de livraison lorsque demandé."},
        {"Comment échanger une carte-cadeau ?",
         "Appuyez sur PAYER, puis sur Échanger carte-cadeau et saisissez le numéro de la carte."},
        {"Comment changer la langue de l'application ?",
         "Appuyez sur le bouton de langue à côté de l'horloge en haut de l'écran et choisissez votre langue."},
        {"Comment enregistrer un client fidélité ?",
         "Appuyez sur + Client fidélité dans la colonne du panier et saisissez le nom et le téléphone du client."},
        {"Comment annuler une commande ?",
         "Appuyez sur Annuler la commande dans la colonne du panier, ou Annuler la commande enregistrée sur l'écran des paiements."},
        {"Qui contacter pour plus d'aide ?",
         "Utilisez la section ASSISTANCE de l'onglet Fonctions pour appeler ou discuter avec l'assistance."},
    };

    private final JPanel topicsPanel = new JPanel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel pickerLabel = new JLabel();
    private final JButton closeBtn = new JButton();

    public HelpDialog(Frame owner) {
        super(owner, true);
        setLayout(new BorderLayout());
        setSize(620, 560);
        setLocationRelativeTo(owner);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        header.add(titleLabel, BorderLayout.WEST);

        JPanel pickerRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pickerRow.setOpaque(false);
        pickerLabel.setForeground(Color.LIGHT_GRAY);
        JComboBox<I18n.Lang> langBox = new JComboBox<>(I18n.Lang.values());
        langBox.setSelectedItem(I18n.current());
        langBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof I18n.Lang) setText(((I18n.Lang) value).nativeName);
                return this;
            }
        });
        langBox.addActionListener(e -> {
            I18n.Lang selected = (I18n.Lang) langBox.getSelectedItem();
            I18n.setLanguage(selected);
            refreshText();
        });
        pickerRow.add(pickerLabel);
        pickerRow.add(langBox);
        header.add(pickerRow, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        topicsPanel.setLayout(new BoxLayout(topicsPanel, BoxLayout.Y_AXIS));
        topicsPanel.setBackground(Color.WHITE);
        topicsPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JScrollPane scroll = new JScrollPane(topicsPanel);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);

        refreshText();
    }

    private String[][] topicsForCurrentLanguage() {
        switch (I18n.current()) {
            case BN: return TOPICS_BN;
            case HI: return TOPICS_HI;
            case ES: return TOPICS_ES;
            case FR: return TOPICS_FR;
            default: return TOPICS_EN;
        }
    }

    private void refreshText() {
        setTitle(I18n.t("Help & Support"));
        titleLabel.setText(I18n.t("Help & Support"));
        pickerLabel.setText(I18n.t("Select language:"));
        closeBtn.setText(I18n.t("Close"));

        topicsPanel.removeAll();
        String[][] topics = topicsForCurrentLanguage();
        for (String[] qa : topics) {
            JLabel q = new JLabel("<html><b>" + qa[0] + "</b></html>");
            q.setAlignmentX(Component.LEFT_ALIGNMENT);
            q.setForeground(UITheme.SECTION_HEADER_COLOR);
            q.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
            JLabel a = new JLabel("<html><div style='width:520px'>" + qa[1] + "</div></html>");
            a.setAlignmentX(Component.LEFT_ALIGNMENT);
            topicsPanel.add(q);
            topicsPanel.add(a);
        }
        topicsPanel.revalidate();
        topicsPanel.repaint();
    }
}
