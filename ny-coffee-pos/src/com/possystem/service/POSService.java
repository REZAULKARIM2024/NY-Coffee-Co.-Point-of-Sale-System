package com.possystem.service;

import com.possystem.dao.CustomerDAO;
import com.possystem.dao.OrderDAO;
import com.possystem.model.CartItem;
import com.possystem.model.Customer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

public class POSService {

    public static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8% — change here to adjust globally
    public static final int LOYALTY_POINTS_FOR_REWARD = 50; // change here to adjust the loyalty threshold

    private final OrderDAO orderDAO = new OrderDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final Random random = new Random();

    public BigDecimal calculateSubtotal(List<CartItem> cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart) subtotal = subtotal.add(item.getLineTotal());
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTax(BigDecimal subtotalAfterDiscount) {
        return subtotalAfterDiscount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Runs the full checkout: computes totals, simulates/records payment, and persists
     * the order + inventory deduction atomically via OrderDAO.
     */
    public CheckoutResult checkout(List<CartItem> cart, BigDecimal discount, int userId,
                                    Integer customerId, String paymentMethod) {
        return checkout(cart, discount, userId, customerId, paymentMethod, "IN_STORE", "DINE_IN");
    }

    /**
     * Same as checkout(), but also records where the order came from (IN_STORE/PHONE/ONLINE)
     * and how it's being fulfilled (DINE_IN/PICKUP/DELIVERY).
     */
    public CheckoutResult checkout(List<CartItem> cart, BigDecimal discount, int userId,
                                    Integer customerId, String paymentMethod, String orderSource, String orderType) {
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal afterDiscount = subtotal.subtract(discount == null ? BigDecimal.ZERO : discount);
        if (afterDiscount.compareTo(BigDecimal.ZERO) < 0) afterDiscount = BigDecimal.ZERO;
        BigDecimal tax = calculateTax(afterDiscount);
        BigDecimal total = afterDiscount.add(tax);

        String reference = simulatePaymentReference(paymentMethod);

        int orderId = orderDAO.checkout(cart, userId, customerId, subtotal,
                discount == null ? BigDecimal.ZERO : discount, tax, total, paymentMethod, reference,
                orderSource, orderType);

        return new CheckoutResult(orderId, subtotal, discount, tax, total, reference);
    }

    /**
     * Split-tender checkout: part of the total is paid in CASH (cashAmount) and the remainder
     * is charged to CARD, recorded as two separate payment rows against the same order. Used
     * when the cash a customer hands over doesn't cover the full total and the rest is put on
     * a card instead of topping up the cash.
     */
    public CheckoutResult checkoutSplitCashCard(List<CartItem> cart, BigDecimal discount, int userId,
                                    Integer customerId, BigDecimal cashAmount, String orderSource, String orderType) {
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal afterDiscount = subtotal.subtract(discount == null ? BigDecimal.ZERO : discount);
        if (afterDiscount.compareTo(BigDecimal.ZERO) < 0) afterDiscount = BigDecimal.ZERO;
        BigDecimal tax = calculateTax(afterDiscount);
        BigDecimal total = afterDiscount.add(tax);

        BigDecimal cash = cashAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal cardAmount = total.subtract(cash).setScale(2, RoundingMode.HALF_UP);

        String cashRef = simulatePaymentReference("CASH");
        String cardRef = simulatePaymentReference("CARD");

        List<OrderDAO.PaymentPart> parts = List.of(
                new OrderDAO.PaymentPart("CASH", cash, cashRef),
                new OrderDAO.PaymentPart("CARD", cardAmount, cardRef));

        int orderId = orderDAO.checkoutMultiPayment(cart, userId, customerId, subtotal,
                discount == null ? BigDecimal.ZERO : discount, tax, total, parts, orderSource, orderType);

        CheckoutResult result = new CheckoutResult(orderId, subtotal, discount, tax, total, cashRef + " + " + cardRef);
        result.cashPortion = cash;
        result.cardPortion = cardAmount;
        return result;
    }

    /** Same as checkoutSplitCashCard(), but also runs the loyalty program (see checkoutWithLoyalty). */
    public CheckoutResult checkoutSplitCashCardWithLoyalty(List<CartItem> cart, BigDecimal discount, int userId,
                                    Customer customer, BigDecimal cashAmount, String orderSource, String orderType) {
        int newPoints = customer.getLoyaltyPoints() + 1;
        boolean rewardApplied = false;

        if (newPoints >= LOYALTY_POINTS_FOR_REWARD && !cart.isEmpty()) {
            CartItem cheapest = cart.get(0);
            for (CartItem ci : cart) {
                if (ci.getUnitPrice().compareTo(cheapest.getUnitPrice()) < 0) cheapest = ci;
            }
            cheapest.setLoyaltyFree(true);
            newPoints = 0;
            rewardApplied = true;
        }

        CheckoutResult result = checkoutSplitCashCard(cart, discount, userId, customer.getId(), cashAmount, orderSource, orderType);
        customerDAO.updatePoints(customer.getId(), newPoints);
        result.loyaltyPointsAfter = newPoints;
        result.loyaltyRewardApplied = rewardApplied;
        return result;
    }

    /**
     * Same as checkout(), but also runs the loyalty program: +1 point for the
     * customer, and if that reaches LOYALTY_POINTS_FOR_REWARD, the cheapest item
     * in the cart becomes free and points reset to 0.
     */
    public CheckoutResult checkoutWithLoyalty(List<CartItem> cart, BigDecimal discount, int userId,
                                               Customer customer, String paymentMethod) {
        return checkoutWithLoyalty(cart, discount, userId, customer, paymentMethod, "IN_STORE", "DINE_IN");
    }

    /** Same as checkoutWithLoyalty() above, but also records order source/type. */
    public CheckoutResult checkoutWithLoyalty(List<CartItem> cart, BigDecimal discount, int userId,
                                               Customer customer, String paymentMethod,
                                               String orderSource, String orderType) {
        int newPoints = customer.getLoyaltyPoints() + 1;
        boolean rewardApplied = false;

        if (newPoints >= LOYALTY_POINTS_FOR_REWARD && !cart.isEmpty()) {
            CartItem cheapest = cart.get(0);
            for (CartItem ci : cart) {
                if (ci.getUnitPrice().compareTo(cheapest.getUnitPrice()) < 0) cheapest = ci;
            }
            cheapest.setLoyaltyFree(true);
            newPoints = 0;
            rewardApplied = true;
        }

        CheckoutResult result = checkout(cart, discount, userId, customer.getId(), paymentMethod, orderSource, orderType);
        customerDAO.updatePoints(customer.getId(), newPoints);
        result.loyaltyPointsAfter = newPoints;
        result.loyaltyRewardApplied = rewardApplied;
        return result;
    }

    private String simulatePaymentReference(String method) {
        // TODO: replace with a real gateway call — see README "Going live with a real payment gateway"
        return method + "-SIM-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));
    }

    public static class CheckoutResult {
        public final int orderId;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal tax;
        public final BigDecimal total;
        public final String paymentReference;
        public int loyaltyPointsAfter = -1;      // -1 = no loyalty customer on this order
        public boolean loyaltyRewardApplied = false;
        public BigDecimal cashPortion;           // non-null only for split cash+card checkouts
        public BigDecimal cardPortion;           // non-null only for split cash+card checkouts

        public CheckoutResult(int orderId, BigDecimal subtotal, BigDecimal discount,
                               BigDecimal tax, BigDecimal total, String paymentReference) {
            this.orderId = orderId;
            this.subtotal = subtotal;
            this.discount = discount;
            this.tax = tax;
            this.total = total;
            this.paymentReference = paymentReference;
        }
    }
}
