package com.ktk.dukappservice.data.paceuserround;

/**
 * Pure logic for calculating round status.
 * No dependencies, no services, just math.
 */
public record RoundStatusCalculator(
    double userGoal,
    int userTransactions,
    double roundLocalGoalPercentage,
    double roundGoalPercentage,
    double currentStatusDecimal,
    int existingCredits
) {
    // 1. Calculate MyShare Goal
    public int calculateMyShareGoal() {
        double goalFactor = roundLocalGoalPercentage / 100.0;
        int targetAmount = (int) Math.round(userGoal * goalFactor);
        return Math.max(0, targetAmount - userTransactions);
    }

    // 2. Determine if User is On Track
    public boolean isOnTrack() {
        return (currentStatusDecimal * 100) >= roundGoalPercentage;
    }

    public boolean isLocalOnTrack() {
        return (currentStatusDecimal * 100) >= roundLocalGoalPercentage;
    }

    // 3. Round Credits logic (if you have specific logic for it)
    public int resolveCredits() {
        return Math.max(0, existingCredits);
    }
}