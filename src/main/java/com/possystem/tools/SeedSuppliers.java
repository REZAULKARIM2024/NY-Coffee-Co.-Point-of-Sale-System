package com.possystem.tools;

import com.possystem.dao.SupplierDAO;
import com.possystem.model.Supplier;

/**
 * One-off seeding utility: populates the new Suppliers module with realistic NYC-area
 * vendor contacts covering every ingredient/retail family used elsewhere in the app
 * (coffee, dairy, bakery, produce, deli, packaging, equipment, retail merch, etc.).
 * Uses SupplierDAO directly, same as the "Add Supplier" button would.
 *
 * Usage: java -cp target/classes;lib/mysql-connector-j-*.jar com.possystem.tools.SeedSuppliers
 */
public class SeedSuppliers {

    private static final String[][] SUPPLIERS = {
        // name, contact person, phone, email, address
        {"Empire Roasting Co.", "Marcus Reilly", "(718) 555-3010", "orders@empireroasting.com", "412 Franklin Ave, Brooklyn, NY 11238"},
        {"Hudson Valley Dairy Cooperative", "Diane Koch", "(845) 555-2244", "accounts@hvdairycoop.com", "88 Creamery Rd, New Paltz, NY 12561"},
        {"Gotham Bakers Supply", "Anthony Ruiz", "(718) 555-4471", "sales@gothambakerssupply.com", "215 Northern Blvd, Queens, NY 11101"},
        {"Five Boroughs Produce", "Sarah Kim", "(718) 555-6602", "orders@5boroughsproduce.com", "901 Hunts Point Ave, Bronx, NY 10474"},
        {"Liberty Packaging Solutions", "James Whitfield", "(212) 555-7788", "sales@libertypackaging.com", "44-10 Vernon Blvd, Long Island City, NY 11101"},
        {"Empire State Flavor Co.", "Priya Nair", "(212) 555-9021", "info@empirestateflavor.com", "233 Spring St, New York, NY 10013"},
        {"Continental Meat & Deli Supply", "Frank DeLuca", "(718) 555-3345", "orders@continentaldeli.com", "1180 Atlantic Ave, Brooklyn, NY 11238"},
        {"Harbor Tea Traders", "Wei Chen", "(212) 555-6690", "wholesale@harborteatraders.com", "77 Mott St, New York, NY 10013"},
        {"Metro Restaurant Equipment", "Oscar Ramirez", "(718) 555-1123", "service@metrorestequip.com", "30-15 Steinway St, Queens, NY 11103"},
        {"Brooklyn Print & Promo", "Nina Alvarez", "(718) 555-8834", "hello@brooklynprintpromo.com", "199 Bedford Ave, Brooklyn, NY 11249"},
        {"NYC Gift Card Solutions", "Derek Shaw", "(212) 555-4456", "support@nycgiftcardsolutions.com", "350 5th Ave, New York, NY 10118"},
        {"Union Square Linen & Janitorial", "Carol Bennett", "(212) 555-2287", "service@unionsquarelinen.com", "853 Broadway, New York, NY 10003"},
        {"Catskill Farms Collective", "Ben Foster", "(845) 555-7761", "orders@catskillfarmscollective.com", "12 Orchard Rd, Margaretville, NY 12455"},
        {"Empire Ice & Beverage Supply", "Luis Ortega", "(718) 555-5529", "orders@empireicebev.com", "2200 Bruckner Blvd, Bronx, NY 10473"},
        {"Continental Coffee Equipment", "Grace Sullivan", "(212) 555-3367", "service@continentalcoffeeequip.com", "150 W 28th St, New York, NY 10001"},
        {"Five Points Bakery Ingredients", "Tom Baxter", "(718) 555-9915", "sales@fivepointsbakery.com", "58-20 Grand Ave, Queens, NY 11378"},
        {"Harlem Honey & Sweeteners", "Michelle Grant", "(212) 555-6674", "orders@harlemhoney.com", "215 W 125th St, New York, NY 10027"}
    };

    public static void main(String[] args) {
        SupplierDAO dao = new SupplierDAO();
        int count = 0;
        for (String[] row : SUPPLIERS) {
            Supplier s = new Supplier();
            s.setName(row[0]);
            s.setContactPerson(row[1]);
            s.setPhone(row[2]);
            s.setEmail(row[3]);
            s.setAddress(row[4]);
            dao.saveSupplier(s);
            count++;
        }
        System.out.println("Suppliers inserted: " + count);
        System.out.println("DONE.");
    }
}
