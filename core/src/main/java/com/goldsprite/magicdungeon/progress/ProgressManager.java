package com.goldsprite.magicdungeon.progress;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.magicdungeon.AppConstants;
import java.util.*;

public class ProgressManager {
    private static ProgressManager instance;
    private static final String SAVE_FILE = "progress.json";

    private Map<String, ProgressNode> nodes = new HashMap<>();
    private Set<String> unlockedIds = new HashSet<>();

    public static ProgressManager getInstance() {
        if (instance == null) {
            instance = new ProgressManager();
        }
        return instance;
    }

    private ProgressManager() {
        initDefinitions();
        load();
    }

    private void initDefinitions() {
        // Test Data
        addNode(new ProgressNode("root", "始源", "一切的开始", "icon_star"));
        
        addNode(new ProgressNode("combat_1", "初级战斗", "学会挥舞武器", "icon_sword").addParent("root"));
        addNode(new ProgressNode("combat_2", "坚韧", "提升防御力", "icon_shield").addParent("combat_1"));
        addNode(new ProgressNode("combat_3", "狂暴", "提升攻击力", "icon_axe").addParent("combat_1"));
        
        addNode(new ProgressNode("magic_1", "魔力感应", "感知周围的魔法", "icon_wand").addParent("root"));
        addNode(new ProgressNode("magic_2", "冥想", "加快法力回复", "icon_potion").addParent("magic_1"));
        
        addNode(new ProgressNode("mastery", "大师", "精通战斗与魔法", "icon_crown")
            .addParent("combat_3").addParent("magic_2")); // Dual parent

        buildGraph();
    }

    private void addNode(ProgressNode node) {
        nodes.put(node.id, node);
    }

    private void buildGraph() {
        // Clear children
        for (ProgressNode node : nodes.values()) {
            node.childIds.clear();
        }
        // Populate children from parents
        for (ProgressNode node : nodes.values()) {
            for (String pid : node.parentIds) {
                ProgressNode parent = nodes.get(pid);
                if (parent != null) {
                    parent.childIds.add(node.id);
                }
            }
        }
        
        // Auto Layout
        ProgressLayout.layout(nodes.values());
    }

    public Collection<ProgressNode> getAllNodes() {
        return nodes.values();
    }
    
    public ProgressNode getNode(String id) {
        return nodes.get(id);
    }

    public boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }
    
    public boolean isAvailable(String id) {
        if (isUnlocked(id)) return false; // Already unlocked (so not available to unlock, but unlocked)
        // Wait, "Available" usually means "Can be unlocked now".
        // If it's already unlocked, we might return false or handle it in UI.
        
        ProgressNode node = nodes.get(id);
        if (node == null) return false;
        if (node.parentIds.isEmpty()) return true; // Root is available if not unlocked
        
        // Check if all parents are unlocked
        for (String pid : node.parentIds) {
            if (!isUnlocked(pid)) return false;
        }
        return true;
    }

    public void unlock(String id) {
        if (!unlockedIds.contains(id)) {
            unlockedIds.add(id);
            save();
        }
    }

    public void save() {
        try {
            Json json = new Json();
            FileHandle file = AppConstants.getLocalFile(SAVE_FILE);
            file.parent().mkdirs();
            file.writeString(json.prettyPrint(unlockedIds), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void load() {
        try {
            FileHandle file = AppConstants.getLocalFile(SAVE_FILE);
            if (file.exists()) {
                Json json = new Json();
                unlockedIds = json.fromJson(HashSet.class, file.readString());
            } else {
                // Default unlocks
                unlock("root");
            }
        } catch (Exception e) {
            e.printStackTrace();
            unlock("root"); // Fallback
        }
    }
}