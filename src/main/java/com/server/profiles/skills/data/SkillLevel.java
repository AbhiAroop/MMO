package com.server.profiles.skills.data;

/**
 * Represents a skill's level and experience
 */
public class SkillLevel {
    private int level;
    private double currentXp;
    private double totalXp;
    
    /**
     * Create a new skill level
     * 
     * @param level The current level
     * @param currentXp The current XP towards the next level
     * @param totalXp The total XP earned overall
     */
    public SkillLevel(int level, double currentXp, double totalXp) {
        this.level = Math.max(0, level); // Changed to allow level 0 as minimum
        this.currentXp = Math.max(0, currentXp);
        this.totalXp = Math.max(0, totalXp);
    }
    
    /**
     * Get the current level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Set the current level
     */
    public void setLevel(int level) {
        this.level = Math.max(0, level); // Changed to allow level 0 as minimum
    }
    
    /**
     * Get the current XP towards the next level
     */
    public double getCurrentXp() {
        return currentXp;
    }
    
    /**
     * Set the current XP towards the next level
     */
    public void setCurrentXp(double currentXp) {
        this.currentXp = Math.max(0, currentXp);
    }
    
    /**
     * Get the total XP earned overall
     */
    public double getTotalXp() {
        return totalXp;
    }
    
    /**
     * Set the total XP earned overall
     */
    public void setTotalXp(double totalXp) {
        this.totalXp = Math.max(0, totalXp);
    }
    
    /**
     * Get the progress towards the next level as a percentage (0.0 to 1.0)
     * 
     * @param xpForNextLevel The XP required for the next level
     * @return The progress percentage
     */
    public double getProgressPercentage(double xpForNextLevel) {
        if (xpForNextLevel <= 0) return 1.0;
        return Math.min(1.0, currentXp / xpForNextLevel);
    }
}