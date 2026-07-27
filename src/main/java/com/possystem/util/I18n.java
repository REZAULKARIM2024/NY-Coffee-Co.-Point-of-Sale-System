package com.possystem.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal in-app translation layer for the POS "chrome" — navigation, common actions, and
 * dialogs — across 5 languages (English, Bangla, Hindi, Spanish, French).
 *
 * This intentionally does NOT translate product/menu content (item names, category names,
 * descriptions): those are business data stored in the database, not UI language, and the
 * app already has a separate mechanism for multi-language prep instructions (see
 * RecipeDAO/RecipePanel, which store per-language recipe steps).
 *
 * Usage: call {@link #t(String)} with the English string as the lookup key wherever UI text
 * is built; call {@link #setLanguage(Lang)} to switch; register a {@link #addListener(Runnable)}
 * callback to redraw a screen when the language changes.
 */
public final class I18n {

    public enum Lang {
        EN("English"), BN("বাংলা"), HI("हिन्दी"), ES("Español"), FR("Français");
        public final String nativeName;
        Lang(String nativeName) { this.nativeName = nativeName; }
    }

    private static volatile Lang current = Lang.EN;
    private static final List<Runnable> listeners = new ArrayList<>();

    private I18n() {}

    public static Lang current() { return current; }

    public static void setLanguage(Lang lang) {
        if (lang == null || lang == current) return;
        current = lang;
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }

    /** Registers a callback fired after every language change (e.g. "rebuild my screen text"). */
    public static void addListener(Runnable r) { listeners.add(r); }

    /** Translates an English UI string into the active language. Falls back to the English
     *  key itself if no translation exists yet for that key/language, so nothing ever goes
     *  blank — untranslated strings just stay in English until a translation is added. */
    public static String t(String key) {
        if (current == Lang.EN || key == null) return key;
        Map<String, String> table = DICT.get(current);
        if (table == null) return key;
        return table.getOrDefault(key, key);
    }

    private static final Map<Lang, Map<String, String>> DICT = new LinkedHashMap<>();
    static {
        DICT.put(Lang.BN, new LinkedHashMap<>());
        DICT.put(Lang.HI, new LinkedHashMap<>());
        DICT.put(Lang.ES, new LinkedHashMap<>());
        DICT.put(Lang.FR, new LinkedHashMap<>());
        init();
    }

    private static void put(String key, String bn, String hi, String es, String fr) {
        DICT.get(Lang.BN).put(key, bn);
        DICT.get(Lang.HI).put(key, hi);
        DICT.get(Lang.ES).put(key, es);
        DICT.get(Lang.FR).put(key, fr);
    }

    private static void init() {
        // ---- Header / global chrome ----
        put("Eat In", "খাবেন এখানে", "यहीं खाएं", "Para comer aquí", "Sur place");
        put("Help", "সাহায্য", "सहायता", "Ayuda", "Aide");
        put("Language", "ভাষা", "भाषा", "Idioma", "Langue");
        put("Close", "বন্ধ করুন", "बंद करें", "Cerrar", "Fermer");
        put("Help & Support", "সাহায্য ও সাপোর্ট", "सहायता और समर्थन", "Ayuda y soporte", "Aide et assistance");
        put("Select language:", "ভাষা নির্বাচন করুন:", "भाषा चुनें:", "Seleccione idioma:", "Choisissez la langue :");

        // ---- Department tabs ----
        put("Beverages", "পানীয়", "पेय", "Bebidas", "Boissons");
        put("Featured", "ফিচারড", "विशेष", "Destacados", "En vedette");
        put("Bakery", "বেকারি", "बेकरी", "Panadería", "Boulangerie");
        put("Sandwiches", "স্যান্ডউইচ", "सैंडविच", "Sándwiches", "Sandwichs");
        put("Retail", "রিটেইল", "रिटेल", "Tienda", "Boutique");
        put("Local", "লোকাল", "स्थानीय", "Local", "Local");
        put("Functions", "ফাংশন", "फ़ंक्शन", "Funciones", "Fonctions");

        // ---- News & Promos ----
        put("News & Promos", "খবর ও অফার", "समाचार और ऑफ़र", "Noticias y promociones", "Actualités et promos");
        put("This Month's Happenings", "এই মাসের ঘটনা", "इस महीने की गतिविधियाँ", "Novedades de este mes", "Événements du mois");

        // ---- Cart column ----
        put("Order Type:", "অর্ডার টাইপ:", "ऑर्डर प्रकार:", "Tipo de pedido:", "Type de commande:");
        put("Source:", "সোর্স:", "स्रोत:", "Origen:", "Source:");
        put("Discount $:", "ছাড় $:", "छूट $:", "Descuento $:", "Remise $:");
        put("+ Loyalty Customer", "+ লয়্যালটি কাস্টমার", "+ लॉयल्टी ग्राहक", "+ Cliente de lealtad", "+ Client fidélité");
        put("Subtotal", "সাবটোটাল", "उप-योग", "Subtotal", "Sous-total");
        put("Tax", "ট্যাক্স", "कर", "Impuesto", "Taxe");
        put("Total", "মোট", "कुल", "Total", "Total");
        put("Remove Last", "শেষটি বাদ দিন", "अंतिम हटाएं", "Quitar último", "Retirer le dernier");
        put("Cancel Order", "অর্ডার বাতিল", "ऑर्डर रद्द करें", "Cancelar pedido", "Annuler la commande");
        put("PAY", "পেমেন্ট", "भुगतान", "PAGAR", "PAYER");

        // ---- Customize screen ----
        put("Size", "সাইজ", "आकार", "Tamaño", "Taille");
        put("Temperature", "তাপমাত্রা", "तापमान", "Temperatura", "Température");
        put("Hot", "গরম", "गर्म", "Caliente", "Chaud");
        put("Iced", "ঠান্ডা", "ठंडा", "Con hielo", "Glacé");
        put("Item Done", "আইটেম সম্পন্ন", "आइटम पूर्ण", "Artículo listo", "Article terminé");

        // ---- Functions sidebar ----
        put("Manager", "ম্যানেজার", "प्रबंधक", "Gerente", "Responsable");
        put("Daily Shift Functions", "দৈনিক শিফট ফাংশন", "दैनिक शिफ्ट कार्य", "Funciones del turno diario", "Fonctions du quart quotidien");
        put("Reports", "রিপোর্ট", "रिपोर्ट", "Informes", "Rapports");
        put("All Open Checks", "সব খোলা চেক", "सभी खुले चेक", "Todas las cuentas abiertas", "Toutes les additions ouvertes");
        put("Phone Orders", "ফোন অর্ডার", "फ़ोन ऑर्डर", "Pedidos por teléfono", "Commandes par téléphone");
        put("DT Orders", "ড্রাইভ-থ্রু অর্ডার", "ड्राइव-थ्रू ऑर्डर", "Pedidos autoservicio", "Commandes drive");
        put("OTG Orders", "OTG অর্ডার", "OTG ऑर्डर", "Pedidos OTG", "Commandes OTG");
        put("Order Confirm Board", "অর্ডার কনফার্ম বোর্ড", "ऑर्डर पुष्टि बोर्ड", "Panel de confirmación de pedidos", "Tableau de confirmation des commandes");
        put("SUPPORT", "সাপোর্ট", "सहायता केंद्र", "SOPORTE", "ASSISTANCE");

        // ---- Payments sidebar ----
        put("Payments", "পেমেন্টস", "भुगतान", "Pagos", "Paiements");
        put("Other Payments", "অন্যান্য পেমেন্ট", "अन्य भुगतान", "Otros pagos", "Autres paiements");
        put("Coupons/Discounts", "কুপন/ছাড়", "कूपन/छूट", "Cupones/Descuentos", "Coupons/Remises");
        put("Gift Card Functions", "গিফট কার্ড ফাংশন", "गिफ्ट कार्ड कार्य", "Funciones de tarjeta de regalo", "Fonctions carte-cadeau");
        put("Service Charges", "সার্ভিস চার্জ", "सेवा शुल्क", "Cargos por servicio", "Frais de service");
        put("Delivery Payments", "ডেলিভারি পেমেন্ট", "डिलीवरी भुगतान", "Pagos de entrega", "Paiements de livraison");

        // ---- Functions / Payments group headers ----
        put("Application Functions", "অ্যাপ্লিকেশন ফাংশন", "एप्लिकेशन कार्य", "Funciones de la aplicación", "Fonctions de l'application");
        put("Device Functions", "ডিভাইস ফাংশন", "डिवाइस कार्य", "Funciones del dispositivo", "Fonctions de l'appareil");
        put("Barcode Functions", "বারকোড ফাংশন", "बारकोड कार्य", "Funciones de código de barras", "Fonctions code-barres");
        put("Drawer Functions", "ড্রয়ার ফাংশন", "ड्रॉअर कार्य", "Funciones de la caja", "Fonctions du tiroir-caisse");
        put("Order Types", "অর্ডার টাইপ", "ऑर्डर प्रकार", "Tipos de pedido", "Types de commande");
        put("Employee Functions", "কর্মচারী ফাংশন", "कर्मचारी कार्य", "Funciones de empleado", "Fonctions employé");
        put("Time Clock Function", "টাইম ক্লক ফাংশন", "टाइम क्लॉक कार्य", "Función de reloj de tiempo", "Fonction pointeuse");
        put("Check Functions", "চেক ফাংশন", "चेक कार्य", "Funciones de cuenta", "Fonctions d'addition");
        put("SmartSell Functions", "স্মার্টসেল ফাংশন", "स्मार्टसेल कार्य", "Funciones SmartSell", "Fonctions SmartSell");
        put("Notification Center", "নোটিফিকেশন সেন্টার", "सूचना केंद्र", "Centro de notificaciones", "Centre de notifications");
        put("Menu Functions", "মেনু ফাংশন", "मेनू कार्य", "Funciones del menú", "Fonctions du menu");
        put("Gift Card Batch", "গিফট কার্ড ব্যাচ", "गिफ्ट कार्ड बैच", "Lote de tarjetas de regalo", "Lot de cartes-cadeaux");
        put("Till Management", "টিল ম্যানেজমেন্ট", "टिल प्रबंधन", "Gestión de caja", "Gestion de la caisse");
        put("Unassign User(s) from Till", "টিল থেকে ইউজার(দের) অ্যাসাইনমেন্ট বাতিল", "टिल से उपयोगकर्ता(ओं) को अनसाइन करें", "Desasignar usuario(s) de la caja", "Désaffecter le(s) utilisateur(s) de la caisse");
        put("View Status", "স্ট্যাটাস দেখুন", "स्थिति देखें", "Ver estado", "Voir le statut");
        put("Cash Management Dashboard", "ক্যাশ ম্যানেজমেন্ট ড্যাশবোর্ড", "नकद प्रबंधन डैशबोर्ड", "Panel de gestión de efectivo", "Tableau de bord gestion de caisse");
        put("Safe / Cash Pull", "সেফ / ক্যাশ পুল", "सेफ / कैश पुल", "Caja fuerte / Retiro de efectivo", "Coffre / Retrait de caisse");
        put("All Reports", "সব রিপোর্ট", "सभी रिपोर्ट", "Todos los informes", "Tous les rapports");
        put("Financial Reports", "আর্থিক রিপোর্ট", "वित्तीय रिपोर्ट", "Informes financieros", "Rapports financiers");
        put("Sales Reports", "বিক্রয় রিপোর্ট", "बिक्री रिपोर्ट", "Informes de ventas", "Rapports de ventes");
        put("Labor Reports", "লেবার রিপোর্ট", "श्रम रिपोर्ट", "Informes de mano de obra", "Rapports de main-d'œuvre");
        put("Check Reports", "চেক রিপোর্ট", "चेक रिपोर्ट", "Informes de cuentas", "Rapports d'additions");
        put("Order Confirmation Board Display Functions", "অর্ডার কনফার্মেশন বোর্ড ডিসপ্লে ফাংশন", "ऑर्डर पुष्टि बोर्ड डिस्प्ले कार्य", "Funciones de pantalla del panel de confirmación", "Fonctions d'affichage du tableau de confirmation");
        put("Order Confirmation Board Support Functions", "অর্ডার কনফার্মেশন বোর্ড সাপোর্ট ফাংশন", "ऑर्डर पुष्टि बोर्ड सहायता कार्य", "Funciones de soporte del panel de confirmación", "Fonctions d'assistance du tableau de confirmation");

        // ---- Manager screen leaf buttons ----
        put("Launch PMC", "PMC চালু করুন", "PMC लॉन्च करें", "Iniciar PMC", "Lancer PMC");
        put("Close Application", "অ্যাপ্লিকেশন বন্ধ করুন", "एप्लिकेशन बंद करें", "Cerrar aplicación", "Fermer l'application");
        put("Minimize Application", "অ্যাপ্লিকেশন মিনিমাইজ করুন", "एप्लिकेशन छोटा करें", "Minimizar aplicación", "Réduire l'application");
        put("Activate Backup KDS", "ব্যাকআপ KDS সক্রিয় করুন", "बैकअप KDS सक्रिय करें", "Activar KDS de respaldo", "Activer le KDS de secours");
        put("Restore Primary KDS", "প্রাইমারি KDS পুনরুদ্ধার করুন", "प्राइमरी KDS पुनर्स्थापित करें", "Restaurar KDS principal", "Restaurer le KDS principal");
        put("Upload Logo To Printer", "প্রিন্টারে লোগো আপলোড করুন", "प्रिंटर पर लोगो अपलोड करें", "Subir logo a la impresora", "Téléverser le logo vers l'imprimante");
        put("Barcode Entry", "বারকোড এন্ট্রি", "बारकोड प्रविष्टि", "Entrada de código de barras", "Saisie de code-barres");
        put("Paid In", "প্রদত্ত জমা", "भुगतान प्राप्त", "Entrada de efectivo", "Entrée d'espèces");
        put("Paid Out", "প্রদত্ত উত্তোলন", "भुगतान निकासी", "Salida de efectivo", "Sortie d'espèces");
        put("Cash Pull", "ক্যাশ পুল", "कैश पुल", "Retiro de efectivo", "Retrait de caisse");
        put("No Sale", "নো সেল", "नो सेल", "Sin venta", "Pas de vente");
        put("Change Order Type", "অর্ডার টাইপ পরিবর্তন করুন", "ऑर्डर प्रकार बदलें", "Cambiar tipo de pedido", "Changer le type de commande");
        put("Assign Employee Id", "কর্মচারী আইডি নির্ধারণ করুন", "कर्मचारी आईडी असाइन करें", "Asignar ID de empleado", "Attribuer un identifiant employé");
        put("Employee Training", "কর্মচারী প্রশিক্ষণ", "कर्मचारी प्रशिक्षण", "Capacitación de empleados", "Formation des employés");
        put("Clock In/Out", "ক্লক ইন/আউট", "क्लॉक इन/आउट", "Marcar entrada/salida", "Pointer entrée/sortie");
        put("Adjust Closed Check From List", "তালিকা থেকে বন্ধ চেক সামঞ্জস্য করুন", "सूची से बंद चेक समायोजित करें", "Ajustar cuenta cerrada desde la lista", "Ajuster une addition clôturée depuis la liste");
        put("Adjust Closed Check", "বন্ধ চেক সামঞ্জস্য করুন", "बंद चेक समायोजित करें", "Ajustar cuenta cerrada", "Ajuster une addition clôturée");
        put("Transaction Return", "লেনদেন ফেরত", "लेनदेन वापसी", "Devolución de transacción", "Retour de transaction");
        put("Menu Item Price Override", "মেনু আইটেম মূল্য ওভাররাইড", "मेनू आइटम मूल्य ओवरराइड", "Anular precio del artículo del menú", "Remplacer le prix de l'article du menu");
        put("Manual Credit Entry", "ম্যানুয়াল ক্রেডিট এন্ট্রি", "मैनुअल क्रेडिट प्रविष्टि", "Entrada de crédito manual", "Saisie manuelle de crédit");
        put("SmartSell On/Off", "স্মার্টসেল অন/অফ", "स्मार्टसेल ऑन/ऑफ", "SmartSell activado/desactivado", "SmartSell activé/désactivé");
        put("SmartSell Leaderboard", "স্মার্টসেল লিডারবোর্ড", "स्मार्टसेल लीडरबोर्ड", "Tabla de clasificación SmartSell", "Classement SmartSell");
        put("Consolidation Mode", "কনসোলিডেশন মোড", "समेकन मोड", "Modo de consolidación", "Mode de consolidation");
        put("Menu Item Availability", "মেনু আইটেম উপলব্ধতা", "मेनू आइटम उपलब्धता", "Disponibilidad de artículos del menú", "Disponibilité des articles du menu");
        put("Begin Phone Order", "ফোন অর্ডার শুরু করুন", "फ़ोन ऑर्डर शुरू करें", "Iniciar pedido por teléfono", "Commencer une commande par téléphone");
        put("Navigator Batch Activation", "নেভিগেটর ব্যাচ অ্যাক্টিভেশন", "नेविगेटर बैच सक्रियण", "Activación de lote de Navigator", "Activation de lot Navigator");

        // ---- Daily Shift Functions leaf buttons ----
        put("Assign Till to POS", "টিল POS-এ নির্ধারণ করুন", "टिल को POS में असाइन करें", "Asignar caja al POS", "Affecter la caisse au POS");
        put("Assign User(s) to Till", "টিলে ইউজার(দের) নির্ধারণ করুন", "टिल में उपयोगकर्ता असाइन करें", "Asignar usuario(s) a la caja", "Affecter le(s) utilisateur(s) à la caisse");
        put("Unassign Till from POS", "POS থেকে টিল আনঅ্যাসাইন করুন", "POS से टिल हटाएं", "Desasignar caja del POS", "Désaffecter la caisse du POS");
        put("Count Till", "টিল গণনা করুন", "टिल गिनें", "Contar caja", "Compter la caisse");
        put("Open Safe or Cash Pull", "সেফ বা ক্যাশ পুল খুলুন", "सेफ या कैश पुल खोलें", "Abrir caja fuerte o retiro de efectivo", "Ouvrir le coffre ou le retrait de caisse");
        put("Add Funds", "তহবিল যোগ করুন", "धनराशि जोड़ें", "Agregar fondos", "Ajouter des fonds");
        put("Count Safe or Cash Pull", "সেফ বা ক্যাশ পুল গণনা করুন", "सेफ या कैश पुल गिनें", "Contar caja fuerte o retiro de efectivo", "Compter le coffre ou le retrait de caisse");
        put("Close Safe or Cash Pull", "সেফ বা ক্যাশ পুল বন্ধ করুন", "सेफ या कैश पुल बंद करें", "Cerrar caja fuerte o retiro de efectivo", "Fermer le coffre ou le retrait de caisse");
        put("Deposit Cash from Safe or Cash Pull", "সেফ বা ক্যাশ পুল থেকে নগদ জমা দিন", "सेफ या कैश पुल से नकद जमा करें", "Depositar efectivo de la caja fuerte o retiro", "Déposer les espèces du coffre ou du retrait");
        put("Cash Drawer Report", "ক্যাশ ড্রয়ার রিপোর্ট", "कैश ड्रॉअर रिपोर्ट", "Informe de caja registradora", "Rapport de tiroir-caisse");
        put("Over/Short Report", "ওভার/শর্ট রিপোর্ট", "ओवर/शॉर्ट रिपोर्ट", "Informe de sobrante/faltante", "Rapport d'excédent/déficit");
        put("Paid-in/Paid Out", "প্রদত্ত জমা/উত্তোলন", "भुगतान प्राप्त/निकासी", "Entrada/salida de efectivo", "Entrée/sortie d'espèces");
        put("Safe/Cash Pull Report", "সেফ/ক্যাশ পুল রিপোর্ট", "सेफ/कैश पुल रिपोर्ट", "Informe de caja fuerte/retiro", "Rapport de coffre/retrait");
        put("Bank Deposits Report", "ব্যাংক ডিপোজিট রিপোর্ট", "बैंक जमा रिपोर्ट", "Informe de depósitos bancarios", "Rapport de dépôts bancaires");

        // ---- Reports group leaf buttons ----
        put("Employee Financial", "কর্মচারী আর্থিক", "कर्मचारी वित्तीय", "Financiero del empleado", "Finances de l'employé");
        put("Property Financial", "প্রপার্টি আর্থিক", "प्रॉपर्टी वित्तीय", "Financiero de la propiedad", "Finances de l'établissement");
        put("Menu Item Summary", "মেনু আইটেম সারাংশ", "मेनू आइटम सारांश", "Resumen de artículos del menú", "Résumé des articles du menu");
        put("Menu Item Sales", "মেনু আইটেম বিক্রয়", "मेनू आइटम बिक्री", "Ventas de artículos del menú", "Ventes des articles du menu");
        put("Family Group Sales", "ফ্যামিলি গ্রুপ বিক্রয়", "फैमिली ग्रुप बिक्री", "Ventas por grupo familiar", "Ventes par groupe familial");
        put("Major Group Sales", "মেজর গ্রুপ বিক্রয়", "मेजर ग्रुप बिक्री", "Ventas por grupo principal", "Ventes par groupe principal");
        put("Clock-in Status", "ক্লক-ইন স্ট্যাটাস", "क्लॉक-इन स्थिति", "Estado de entrada", "Statut de pointage");
        put("Employee Labor Summary", "কর্মচারী শ্রম সারাংশ", "कर्मचारी श्रम सारांश", "Resumen de mano de obra", "Résumé de la main-d'œuvre");
        put("Time Period Summary", "সময়কাল সারাংশ", "समय अवधि सारांश", "Resumen del período", "Résumé de la période");
        put("Employee Closed Check", "কর্মচারীর বন্ধ চেক", "कर्मचारी बंद चेक", "Cuenta cerrada del empleado", "Addition clôturée par l'employé");
        put("Employee Open Check", "কর্মচারীর খোলা চেক", "कर्मचारी खुला चेक", "Cuenta abierta del empleado", "Addition ouverte par l'employé");

        // ---- All Open Checks / Phone / DT / OTG leaf buttons ----
        put("View All Open Checks", "সব খোলা চেক দেখুন", "सभी खुले चेक देखें", "Ver todas las cuentas abiertas", "Voir toutes les additions ouvertes");
        put("Filter by Table", "টেবিল অনুযায়ী ফিল্টার করুন", "टेबल के अनुसार फ़िल्टर करें", "Filtrar por mesa", "Filtrer par table");
        put("Filter by Server", "সার্ভার অনুযায়ী ফিল্টার করুন", "सर्वर के अनुसार फ़िल्टर करें", "Filtrar por mesero", "Filtrer par serveur");
        put("Merge Checks", "চেক একত্রিত করুন", "चेक मर्ज करें", "Combinar cuentas", "Fusionner les additions");
        put("View Phone Order Queue", "ফোন অর্ডার সারি দেখুন", "फ़ोन ऑर्डर कतार देखें", "Ver cola de pedidos telefónicos", "Voir la file des commandes téléphoniques");
        put("Cancel Phone Order", "ফোন অর্ডার বাতিল করুন", "फ़ोन ऑर्डर रद्द करें", "Cancelar pedido telefónico", "Annuler la commande téléphonique");
        put("View DT Queue", "ড্রাইভ-থ্রু সারি দেখুন", "ड्राइव-थ्रू कतार देखें", "Ver cola de autoservicio", "Voir la file du drive");
        put("Recall DT Order", "ড্রাইভ-থ্রু অর্ডার পুনরুদ্ধার করুন", "ड्राइव-थ्रू ऑर्डर पुनः प्राप्त करें", "Recuperar pedido de autoservicio", "Rappeler la commande du drive");
        put("Reset DT Timer", "ড্রাইভ-থ্রু টাইমার রিসেট করুন", "ड्राइव-थ्रू टाइमर रीसेट करें", "Reiniciar temporizador de autoservicio", "Réinitialiser le minuteur du drive");
        put("View OTG Queue", "OTG সারি দেখুন", "OTG कतार देखें", "Ver cola OTG", "Voir la file OTG");
        put("Mark OTG Ready", "OTG প্রস্তুত হিসেবে চিহ্নিত করুন", "OTG को तैयार के रूप में चिह्नित करें", "Marcar OTG como listo", "Marquer OTG comme prêt");
        put("Print OTG Ticket", "OTG টিকিট প্রিন্ট করুন", "OTG टिकट प्रिंट करें", "Imprimir ticket OTG", "Imprimer le ticket OTG");

        // ---- Order Confirm Board leaf buttons ----
        put("Activate Display A", "ডিসপ্লে A সক্রিয় করুন", "डिस्प्ले A सक्रिय करें", "Activar pantalla A", "Activer l'écran A");
        put("Deactivate The Display", "ডিসপ্লে নিষ্ক্রিয় করুন", "डिस्प्ले निष्क्रिय करें", "Desactivar la pantalla", "Désactiver l'écran");
        put("Activate Display B", "ডিসপ্লে B সক্রিয় করুন", "डिस्प्ले B सक्रिय करें", "Activar pantalla B", "Activer l'écran B");
        put("View Display Status", "ডিসপ্লে স্ট্যাটাস দেখুন", "डिस्प्ले स्थिति देखें", "Ver estado de la pantalla", "Voir l'état de l'écran");
        put("Test Display", "ডিসপ্লে পরীক্ষা করুন", "डिस्प्ले परीक्षण करें", "Probar pantalla", "Tester l'écran");

        // ---- Payments placeholder screens leaf buttons ----
        put("Check Payment", "চেক পেমেন্ট", "चेक भुगतान", "Pago con cheque", "Paiement par chèque");
        put("House Account", "হাউস অ্যাকাউন্ট", "हाउस खाता", "Cuenta de la casa", "Compte maison");
        put("Employee Meal", "কর্মচারী মিল", "कर्मचारी भोजन", "Comida de empleado", "Repas employé");
        put("Comp", "কম্প (বিনামূল্যে)", "कॉम्प (मुफ़्त)", "Cortesía", "Offert");
        put("Apply Coupon", "কুপন প্রয়োগ করুন", "कूपन लागू करें", "Aplicar cupón", "Appliquer un coupon");
        put("Percent Off Discount", "শতাংশ ছাড়", "प्रतिशत छूट", "Descuento porcentual", "Remise en pourcentage");
        put("Dollar Off Discount", "ডলার ছাড়", "डॉलर छूट", "Descuento en dólares", "Remise en dollars");
        put("Employee Discount", "কর্মচারী ছাড়", "कर्मचारी छूट", "Descuento de empleado", "Remise employé");
        put("Sell Gift Card", "গিফট কার্ড বিক্রি করুন", "गिफ्ट कार्ड बेचें", "Vender tarjeta de regalo", "Vendre une carte-cadeau");
        put("Reload Gift Card", "গিফট কার্ড রিলোড করুন", "गिफ्ट कार्ड रीलोड करें", "Recargar tarjeta de regalo", "Recharger une carte-cadeau");
        put("Check Gift Card Balance", "গিফট কার্ড ব্যালেন্স দেখুন", "गिफ्ट कार्ड बैलेंस देखें", "Consultar saldo de tarjeta de regalo", "Vérifier le solde de la carte-cadeau");
        put("Void Gift Card Sale", "গিফট কার্ড বিক্রয় বাতিল করুন", "गिफ्ट कार्ड बिक्री रद्द करें", "Anular venta de tarjeta de regalo", "Annuler la vente de la carte-cadeau");
        put("Add Service Charge", "সার্ভিস চার্জ যোগ করুন", "सेवा शुल्क जोड़ें", "Agregar cargo por servicio", "Ajouter des frais de service");
        put("Add Delivery Fee", "ডেলিভারি ফি যোগ করুন", "डिलीवरी शुल्क जोड़ें", "Agregar tarifa de entrega", "Ajouter des frais de livraison");
        put("Add Gratuity", "টিপস যোগ করুন", "टिप जोड़ें", "Agregar propina", "Ajouter un pourboire");
        put("Cash on Delivery", "ক্যাশ অন ডেলিভারি", "कैश ऑन डिलीवरी", "Pago contra entrega", "Paiement à la livraison");
        put("Prepaid Delivery", "প্রিপেইড ডেলিভারি", "प्रीपेड डिलीवरी", "Entrega prepagada", "Livraison prépayée");
        put("Delivery Refund", "ডেলিভারি রিফান্ড", "डिलीवरी रिफंड", "Reembolso de entrega", "Remboursement de livraison");

        // ---- Payments screen buttons ----
        put("Credit Card", "ক্রেডিট কার্ড", "क्रेडिट कार्ड", "Tarjeta de crédito", "Carte de crédit");
        put("CASH", "নগদ", "नकद", "EFECTIVO", "ESPÈCES");
        put("GC Redeem", "গিফট কার্ড রিডিম", "गिफ्ट कार्ड रिडीम", "Canjear tarjeta de regalo", "Échanger carte-cadeau");
        put("Tax Exempt", "ট্যাক্স মুক্ত", "कर मुक्त", "Exento de impuestos", "Exonéré de taxe");
        put("Cancel Saved/Stored Order", "সংরক্ষিত অর্ডার বাতিল", "सहेजा गया ऑर्डर रद्द करें", "Cancelar pedido guardado", "Annuler la commande enregistrée");
        put("MAIN MENU", "মূল মেনু", "मुख्य मेनू", "MENÚ PRINCIPAL", "MENU PRINCIPAL");
        put("BACK", "পেছনে", "वापस", "ATRÁS", "RETOUR");
        put("Clear/No", "মুছুন/না", "साफ़ करें/नहीं", "Borrar/No", "Effacer/Non");
        put("Cancel this order?", "এই অর্ডারটি বাতিল করবেন?", "क्या इस ऑर्डर को रद्द करें?", "¿Cancelar este pedido?", "Annuler cette commande ?");
        put("Enter gift card number:", "গিফট কার্ড নম্বর দিন:", "गिफ्ट कार्ड नंबर दर्ज करें:", "Ingrese el número de la tarjeta de regalo:", "Entrez le numéro de la carte-cadeau :");

        // ---- SUPPORT group ----
        put("Call Support", "সাপোর্টে কল করুন", "सहायता को कॉल करें", "Llamar a soporte", "Appeler l'assistance");
        put("Chat with Support", "সাপোর্টের সাথে চ্যাট করুন", "सहायता से चैट करें", "Chatear con soporte", "Discuter avec l'assistance");
        put("Knowledge Base", "নলেজ বেস", "ज्ञान आधार", "Base de conocimientos", "Base de connaissances");
        put("Restart POS Terminal", "POS টার্মিনাল পুনরায় চালু করুন", "POS टर्मिनल पुनः आरंभ करें", "Reiniciar terminal POS", "Redémarrer le terminal POS");

        // ---- misc ----
        put("completed.", "সম্পন্ন হয়েছে।", "पूर्ण हुआ।", "completado.", "terminé.");
    }
}
