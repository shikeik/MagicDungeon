package com.goldsprite.magicdungeon.input;

public enum InputAction {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    
    ATTACK,     // J / X
    INTERACT,   // Space / A
    SKILL,      // H / RT
    BACK,       // Esc / B
    
    MAP,        // M / Y
    BAG,        // E / RB
    PAUSE,      // P / Start
    
    TAB,        // Tab / LT
    QUICK_SLOT, // Q / LB
    
    SAVE,       // F5 / Home
    
    // System Actions
    LOAD_GAME,  // F9
    RESET_MAP,  // R
    
    // UI Navigation Actions
    UI_UP,
    UI_DOWN,
    UI_LEFT,
    UI_RIGHT,
    UI_CONFIRM, // Enter / Space
    UI_CANCEL   // Esc
}
