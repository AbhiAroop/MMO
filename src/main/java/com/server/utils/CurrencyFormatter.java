package com.server.utils;

import java.text.DecimalFormat;

public class CurrencyFormatter {
    
    private static final DecimalFormat UNITS_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat PREMIUM_FORMAT = new DecimalFormat("#,###.##");
    
    /**
     * Format Units currency for display
     */
    public static String formatUnits(int units) {
        if (units >= 1000000) {
            double millionUnits = units / 1000000.0;
            return String.format("%.1fM", millionUnits);
        } else if (units >= 1000) {
            double thousandUnits = units / 1000.0;
            return String.format("%.1fK", thousandUnits);
        } else {
            return UNITS_FORMAT.format(units);
        }
    }
    
    /**
     * Format Premium Units currency for display
     */
    public static String formatPremiumUnits(int premiumUnits) {
        if (premiumUnits >= 1000000) {
            double millionUnits = premiumUnits / 1000000.0;
            return String.format("%.2fM", millionUnits);
        } else if (premiumUnits >= 1000) {
            double thousandUnits = premiumUnits / 1000.0;
            return String.format("%.1fK", thousandUnits);
        } else {
            return PREMIUM_FORMAT.format(premiumUnits);
        }
    }
    
    /**
     * Format Essence currency for display
     */
    public static String formatEssence(int essence) {
        if (essence >= 1000000) {
            double millionEssence = essence / 1000000.0;
            return String.format("%.1fM", millionEssence);
        } else if (essence >= 1000) {
            double thousandEssence = essence / 1000.0;
            return String.format("%.1fK", thousandEssence);
        } else {
            return UNITS_FORMAT.format(essence);
        }
    }
    
    /**
     * Format Bits currency for display
     */
    public static String formatBits(int bits) {
        return UNITS_FORMAT.format(bits);
    }
}