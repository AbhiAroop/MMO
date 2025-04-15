package com.server.items;

public enum ItemType {
    // Functional Items (0XXXXX)
    SWORD(10000),
    AXE(11000),
    BOW(12000),
    PICKAXE(13000),
    SHOVEL(14000),
    HOE(15000),
    HELMET(20000),
    CHESTPLATE(21000),
    LEGGINGS(22000),
    BOOTS(23000),
    RELIC(30000),
    
    // Cosmetic Items (1XXXXX)
    COSMETIC_SWORD(110000),
    COSMETIC_AXE(111000),
    COSMETIC_BOW(112000),
    COSMETIC_PICKAXE(113000),
    COSMETIC_SHOVEL(114000),
    COSMETIC_HOE(115000),
    COSMETIC_HELMET(120000),
    COSMETIC_CHESTPLATE(121000),
    COSMETIC_LEGGINGS(122000),
    COSMETIC_BOOTS(123000);

    private final int baseModelData;

    ItemType(int baseModelData) {
        this.baseModelData = baseModelData;
    }

    public int getBaseModelData() {
        return baseModelData;
    }

    public static boolean isCosmetic(int modelData) {
        return String.valueOf(modelData).startsWith("1");
    }

    public static ItemType getTypeFromModelData(int modelData) {
        String modelStr = String.valueOf(modelData);
        int baseModel = Integer.parseInt(modelStr.substring(0, 5) + "0");
        
        for (ItemType type : values()) {
            if (type.baseModelData == baseModel) {
                return type;
            }
        }
        return null;
    }
}