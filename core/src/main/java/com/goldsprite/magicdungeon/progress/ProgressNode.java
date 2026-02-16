package com.goldsprite.magicdungeon.progress;

import java.util.ArrayList;
import java.util.List;

public class ProgressNode {
    public String id;
    public String title;
    public String description;
    public String iconName;
    
    public List<String> parentIds = new ArrayList<>();
    public List<String> childIds = new ArrayList<>();
    
    // Layout coordinates (Relative, or Grid based)
    public float x, y;
    
    // Runtime State (Not saved here, but useful for UI binding)
    // Actually, UI should query Manager.
    
    public ProgressNode(String id, String title, String description, String iconName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconName = iconName;
    }
    
    public ProgressNode addParent(String parentId) {
        if (!parentIds.contains(parentId)) {
            parentIds.add(parentId);
        }
        return this;
    }
}