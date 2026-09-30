package com.ktk.dukappservice.data.userstatus;

/**
 * Goal and transactions that a status is measured against:
 * the couple's combined values for spouses, otherwise the user's own.
 */
public record StatusTotals(int goal, int transactions) {

    public double status() {
        return goal > 0 ? (double) transactions / goal : 0;
    }
}
