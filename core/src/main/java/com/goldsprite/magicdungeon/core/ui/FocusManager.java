package com.goldsprite.magicdungeon.core.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.kotcrab.vis.ui.widget.VisTextField;
import java.util.ArrayList;
import java.util.List;

/**
 * 焦点管理器
 * 负责管理当前的焦点容器 (FocusScope) 和焦点区域 (FocusZone)
 */
public class FocusManager {
    private static FocusManager instance;
    private FocusScope currentScope;
    
    // 全局输入监听器 (用于处理导航)
    private final InputListener inputListener = new InputListener() {
        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            if (currentScope != null) {
                return currentScope.handleKeyDown(keycode);
            }
            return false;
        }
    };

    public static FocusManager getInstance() {
        if (instance == null) instance = new FocusManager();
        return instance;
    }

    public void setScope(FocusScope scope) {
        if (this.currentScope == scope) return;
        
        if (this.currentScope != null) {
            this.currentScope.onLoseFocus();
        }
        
        this.currentScope = scope;
        
        if (this.currentScope != null) {
            this.currentScope.onGainFocus();
        }
    }
    
    public FocusScope getCurrentScope() {
        return currentScope;
    }
    
    public void registerStage(Stage stage) {
        stage.addListener(inputListener);
    }
    
    public void unregisterStage(Stage stage) {
        stage.removeListener(inputListener);
    }

    // --- Interfaces ---

    public interface FocusScope {
        void onGainFocus();
        void onLoseFocus();
        boolean handleKeyDown(int keycode);
    }

    public interface FocusZone {
        void onGainFocus();
        void onLoseFocus();
        boolean handleNavigation(int keycode);
    }
    
    // --- Helpers ---
    
    /**
     * 简单的 Scope 实现，管理一组 Zone
     */
    public static class BaseFocusScope implements FocusScope {
        protected List<FocusZone> zones = new ArrayList<>();
        protected int currentZoneIndex = 0;
        
        public void addZone(FocusZone zone) {
            zones.add(zone);
        }
        
        @Override
        public void onGainFocus() {
            if (!zones.isEmpty()) {
                zones.get(currentZoneIndex).onGainFocus();
            }
        }

        @Override
        public void onLoseFocus() {
            if (!zones.isEmpty()) {
                zones.get(currentZoneIndex).onLoseFocus();
            }
        }

        @Override
        public boolean handleKeyDown(int keycode) {
            if (zones.isEmpty()) return false;
            
            // Tab 切换 Zone
            if (keycode == Input.Keys.TAB) {
                nextZone();
                return true;
            }
            
            // 当前 Zone 处理
            return zones.get(currentZoneIndex).handleNavigation(keycode);
        }
        
        protected void nextZone() {
            zones.get(currentZoneIndex).onLoseFocus();
            currentZoneIndex = (currentZoneIndex + 1) % zones.size();
            zones.get(currentZoneIndex).onGainFocus();
        }
    }
}
