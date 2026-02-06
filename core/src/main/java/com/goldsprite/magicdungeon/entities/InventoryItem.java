package com.goldsprite.magicdungeon.entities;

import java.util.UUID;

public class InventoryItem {
    public String id;
    public ItemData data;

    public InventoryItem() {
    }

    public InventoryItem(ItemData data) {
        this.id = UUID.randomUUID().toString();
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
