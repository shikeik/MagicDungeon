package com.goldsprite.magicdungeon.progress;

import java.util.*;

public class ProgressLayout {
    private static final float NODE_WIDTH = 80;
    private static final float NODE_HEIGHT = 80;
    private static final float SPACING_X = 40;
    private static final float SPACING_Y = 120;

    public static void layout(Collection<ProgressNode> nodes) {
        // 1. Assign Layers (Longest Path)
        Map<String, Integer> layers = new HashMap<>();
        Map<String, ProgressNode> nodeMap = new HashMap<>();
        for(ProgressNode n : nodes) nodeMap.put(n.id, n);
        
        // Init roots
        List<ProgressNode> roots = new ArrayList<>();
        for(ProgressNode n : nodes) {
            if(n.parentIds.isEmpty()) {
                roots.add(n);
                layers.put(n.id, 0);
            }
        }
        
        // Propagate layers
        // Simple iteration might not be enough for complex DAGs, but enough for simple trees
        // Relax edges N times (Bellman-Ford like)
        for(int i=0; i<nodes.size(); i++) {
            boolean changed = false;
            for(ProgressNode n : nodes) {
                if (n.parentIds.isEmpty()) continue;
                
                int maxParentLayer = -1;
                boolean allParentsReady = true;
                
                for(String pid : n.parentIds) {
                    if(layers.containsKey(pid)) {
                        maxParentLayer = Math.max(maxParentLayer, layers.get(pid));
                    } else {
                        allParentsReady = false;
                        break;
                    }
                }
                
                if(allParentsReady && maxParentLayer != -1) {
                    int newLayer = maxParentLayer + 1;
                    if(!layers.containsKey(n.id) || layers.get(n.id) < newLayer) {
                        layers.put(n.id, newLayer);
                        changed = true;
                    }
                }
            }
            if(!changed) break;
        }
        
        // Group by layer
        Map<Integer, List<ProgressNode>> rows = new TreeMap<>();
        for(ProgressNode n : nodes) {
            int layer = layers.getOrDefault(n.id, 0);
            rows.computeIfAbsent(layer, k -> new ArrayList<>()).add(n);
        }
        
        // 2. Assign Positions
        // Let's grow Up (Root at bottom 0,0).
        
        for(Map.Entry<Integer, List<ProgressNode>> entry : rows.entrySet()) {
            int layer = entry.getKey();
            List<ProgressNode> rowNodes = entry.getValue();
            
            // Sort row nodes to minimize crossing
            // Simple heuristic: sort by average parent X
            if (layer > 0) {
                rowNodes.sort((a, b) -> Float.compare(getAvgParentX(a, nodeMap), getAvgParentX(b, nodeMap)));
            }
            
            float rowWidth = rowNodes.size() * NODE_WIDTH + (rowNodes.size() - 1) * SPACING_X;
            float startX = -rowWidth / 2f + NODE_WIDTH / 2f;
            
            for(int i=0; i<rowNodes.size(); i++) {
                ProgressNode n = rowNodes.get(i);
                n.y = layer * SPACING_Y;
                n.x = startX + i * (NODE_WIDTH + SPACING_X);
            }
        }
    }
    
    private static float getAvgParentX(ProgressNode n, Map<String, ProgressNode> nodeMap) {
        if(n.parentIds.isEmpty()) return 0;
        float sum = 0;
        int count = 0;
        for(String pid : n.parentIds) {
            ProgressNode p = nodeMap.get(pid);
            if(p != null) {
                sum += p.x;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }
}