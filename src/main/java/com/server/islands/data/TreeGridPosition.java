package com.server.islands.data;

/**
 * Represents a position in the challenge tree grid
 */
public class TreeGridPosition {
    private final int x;
    private final int y;
    
    public TreeGridPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Get the X coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Get the Y coordinate
     */
    public int getY() {
        return y;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TreeGridPosition other = (TreeGridPosition) obj;
        return x == other.x && y == other.y;
    }
    
    @Override
    public int hashCode() {
        return 31 * x + y;
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
