package com.goldsprite.magicdungeon.input;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.log.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InputManager {
	private static String INPUTACTIONS_FILE = "MagicDungeon/input_actions.json";
    private static InputManager instance;
    private final Map<InputAction, List<Integer>> keyboardMappings = new HashMap<>();
    private final Map<InputAction, List<Integer>> controllerMappings = new HashMap<>();
    private boolean isVirtualGamepad = false;
    
    // Global Input Mode State
    public enum InputMode {
        MOUSE,
        KEYBOARD // Includes Controller
    }
    private InputMode currentInputMode = InputMode.MOUSE;
    
    // State Tracking
    private final Set<Integer> currentButtons = new HashSet<>();
    private final Set<Integer> previousButtons = new HashSet<>();
    
    // Startup delay to prevent initial controller drift/noise from locking cursor
    private float startupTimer = 0f;
    private static final float STARTUP_DELAY = 0.5f;
    
    // Debug Listener
    private final ControllerListener debugListener = new ControllerAdapter() {
        @Override
        public void connected(Controller controller) {
            Debug.logT("InputManager", "Controller connected: " + controller.getName());
        }

        @Override
        public void disconnected(Controller controller) {
            Debug.logT("InputManager", "Controller disconnected: " + controller.getName());
        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            if (startupTimer < STARTUP_DELAY) return false;
            setInputMode(InputMode.KEYBOARD);
            String btnName = getButtonName(buttonCode);
            Debug.logT("InputManager", "Controller Button Down: " + buttonCode + " [" + btnName + "] (" + controller.getName() + ")");
            return false;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            String btnName = getButtonName(buttonCode);
            Debug.logT("InputManager", "Controller Button Up: " + buttonCode + " [" + btnName + "] (" + controller.getName() + ")");
            return false;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            if (startupTimer < STARTUP_DELAY) return false;
            if (Math.abs(value) > 0.5f) { // Increased threshold for mode switching (was 0.3f)
                setInputMode(InputMode.KEYBOARD);
                Debug.logT("InputManager", "Controller Axis Moved: " + axisCode + " = " + value + " (" + controller.getName() + ")");
            }
            return false;
        }
    };

    // LibGDX Controller Mappings (Standard / Xbox)
    public static final int BUTTON_A = 0;
    public static final int BUTTON_B = 1;
    public static final int BUTTON_X = 2;
    public static final int BUTTON_Y = 3;
    public static final int BUTTON_LB = 4;
    public static final int BUTTON_RB = 5;
    public static final int BUTTON_BACK = 6;
    public static final int BUTTON_START = 7;
    public static final int BUTTON_L3 = 8;
    public static final int BUTTON_R3 = 9;
    
    // D-Pad Mappings
    public static int BUTTON_DPAD_UP = 10;
    public static int BUTTON_DPAD_DOWN = 11;
    public static int BUTTON_DPAD_LEFT = 12;
    public static int BUTTON_DPAD_RIGHT = 13;

    // Switch Pro Controller / Generic D-Pad (User reported)
    public static final int SWITCH_DPAD_UP = 11;
    public static final int SWITCH_DPAD_DOWN = 12;
    public static final int SWITCH_DPAD_LEFT = 13;
    public static final int SWITCH_DPAD_RIGHT = 14;
    
    // Android Mappings (Standard HID)
    // A=96, B=97, X=99, Y=100, L1=102, R1=103
    // DPAD: Up=19, Down=20, Left=21, Right=22
    // But GDX Controller button codes might be different from KeyCodes.
    // Usually on Android GDX Controller maps to raw KeyCodes or HID codes.
    // Let's assume standard Android codes if user reports 20/21.
    public static final int ANDROID_DPAD_UP = 19;
    public static final int ANDROID_DPAD_DOWN = 20;
    public static final int ANDROID_DPAD_LEFT = 21;
    public static final int ANDROID_DPAD_RIGHT = 22;
    
    // Virtual Button IDs for Axis Simulation
    public static final int VIRTUAL_AXIS_START = 200;
    public static final int AXIS_LEFT_UP = 200;
    public static final int AXIS_LEFT_DOWN = 201;
    public static final int AXIS_LEFT_LEFT = 202;
    public static final int AXIS_LEFT_RIGHT = 203;
    
    // Standard Axis Indices
    public static final int AXIS_X = 0; // Left Stick X
    public static final int AXIS_Y = 1; // Left Stick Y
    
    private static final float DEADZONE = 0.3f;

    private InputManager() {
        // Platform specific defaults
        applyPlatformDefaults();
        
        loadMappings();
        Controllers.addListener(debugListener);
        Debug.logT("InputManager", "Initialized. Current controllers: " + Controllers.getControllers().size);
        for(Controller c : Controllers.getControllers()) {
             Debug.logT("InputManager", " - " + c.getName() + " [" + c.getUniqueId() + "]");
        }
    }
    
    private void applyPlatformDefaults() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            BUTTON_DPAD_UP = ANDROID_DPAD_UP;
            BUTTON_DPAD_DOWN = ANDROID_DPAD_DOWN;
            BUTTON_DPAD_LEFT = ANDROID_DPAD_LEFT;
            BUTTON_DPAD_RIGHT = ANDROID_DPAD_RIGHT;
            Debug.logT("InputManager", "Applied Android specific D-Pad mappings.");
        } else {
            // Desktop Defaults (Xbox style or based on user feedback)
            // User confirmed: Up=11, Down=12, Left=13, Right=14 for their Switch Pro
            // Standard Xbox: 10, 11, 12, 13
            // We stick to the ones that worked for user last time
            BUTTON_DPAD_UP = 11;
            BUTTON_DPAD_DOWN = 12;
            BUTTON_DPAD_LEFT = 13;
            BUTTON_DPAD_RIGHT = 14;
        }
    }

    private String getFriendlyName(Controller c) {
        String name = c.getName().toLowerCase();
        if (name.contains("xbox")) return "Xbox Controller";
        if (name.contains("sony") || name.contains("wireless controller")) return "PlayStation Controller"; // DS4 usually "Wireless Controller"
        if (name.contains("nintendo") || name.contains("switch")) return "Switch Pro Controller";
        return c.getName();
    }

    public void setInputMode(InputMode mode) {
        if (currentInputMode == mode) return;
        currentInputMode = mode;
        
        if (mode == InputMode.KEYBOARD) {
            // Lock cursor
            Gdx.input.setCursorCatched(true);
            Debug.logT("InputManager", "InputMode -> KEYBOARD (Cursor Locked)");
        } else {
            // Unlock cursor
            Gdx.input.setCursorCatched(false);
            Debug.logT("InputManager", "InputMode -> MOUSE (Cursor Unlocked)");
        }
    }
    
    public InputMode getInputMode() {
        return currentInputMode;
    }

    public static InputManager getInstance() {
        if (instance == null) instance = new InputManager();
        return instance;
    }
    
    /**
     * Must be called once per frame, preferably at start of render()
     */
    public void update() {
        // Update startup timer
        if (startupTimer < STARTUP_DELAY) {
            startupTimer += Gdx.graphics.getDeltaTime();
        }

        // Cycle states
        previousButtons.clear();
        previousButtons.addAll(currentButtons);
        currentButtons.clear();

        // Check Mouse Movement to switch back to MOUSE mode
        // Only if significant movement to avoid jitter
        if (Math.abs(Gdx.input.getDeltaX()) > 2 || Math.abs(Gdx.input.getDeltaY()) > 2 || Gdx.input.isTouched()) {
             setInputMode(InputMode.MOUSE);
        }
        
        if (Controllers.getControllers().size == 0) return;
        if (startupTimer < STARTUP_DELAY) return; // Ignore controller input during startup
        
        Controller controller = Controllers.getControllers().first();
        
        // 1. Poll configured buttons
        // We iterate through all known mappings to check status
        for (List<Integer> mappedButtons : controllerMappings.values()) {
            for (int btnCode : mappedButtons) {
                if (btnCode < VIRTUAL_AXIS_START) {
                    if (controller.getButton(btnCode)) {
                        currentButtons.add(btnCode);
                    }
                }
            }
        }
        
        // 2. Poll Axes and simulate buttons
        // Note: Axis Y is often inverted (-1 is Up on many controllers)
        float xAxis = controller.getAxis(AXIS_X);
        float yAxis = controller.getAxis(AXIS_Y);
        
        if (Math.abs(xAxis) > DEADZONE) {
            if (xAxis < 0) currentButtons.add(AXIS_LEFT_LEFT);
            else currentButtons.add(AXIS_LEFT_RIGHT);
        }
        
        if (Math.abs(yAxis) > DEADZONE) {
            // Usually -1 is Up, 1 is Down
            if (yAxis < 0) currentButtons.add(AXIS_LEFT_UP);
            else currentButtons.add(AXIS_LEFT_DOWN);
        }
        
        // D-Pad is often handled as buttons by LibGDX (10-13), but verify if needed.
        // If D-Pad is Axis 6/7, we might need more logic here. 
        // For now, assume standard mapping covers D-Pad as buttons.
    }

    public void reload() {
        keyboardMappings.clear();
        controllerMappings.clear();
        loadMappings();
    }

    public void rebindKeyboard(InputAction action, int key) {
        // Clear existing binding for this action to avoid duplicates or keep single key per action?
        // User request: "mapping to same InputAction" implies 1 action -> N keys.
        // But for rebind UI, usually we replace the primary key.
        // Let's replace the first key or clear all and add new one for simplicity in UI.

        List<Integer> keys = keyboardMappings.get(action);
        if (keys == null) {
            keys = new ArrayList<>();
            keyboardMappings.put(action, keys);
        }
        keys.clear(); // Simple mode: 1 key per action for custom binding
        keys.add(key);
    }

    public void rebindController(InputAction action, int buttonCode) {
        List<Integer> buttons = controllerMappings.get(action);
        if (buttons == null) {
            buttons = new ArrayList<>();
            controllerMappings.put(action, buttons);
        }
        buttons.clear();
        buttons.add(buttonCode);
    }

    public void saveMappings() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        JsonValue root = new JsonValue(JsonValue.ValueType.object);

        for (InputAction action : InputAction.values()) {
            JsonValue entry = new JsonValue(JsonValue.ValueType.object);

            // Keyboard
            List<Integer> keys = keyboardMappings.get(action);
            if (keys != null && !keys.isEmpty()) {
                JsonValue kArray = new JsonValue(JsonValue.ValueType.array);
                for (int k : keys) kArray.addChild(new JsonValue(k));
                entry.addChild("keyboard", kArray);
            }

            // Controller
            List<Integer> buttons = controllerMappings.get(action);
            if (buttons != null && !buttons.isEmpty()) {
                JsonValue cArray = new JsonValue(JsonValue.ValueType.array);
                for (int b : buttons) cArray.addChild(new JsonValue(b));
                entry.addChild("controller", cArray);
            }

            root.addChild(action.name(), entry);
        }

        // Save to local storage (user preferences/config)
        // Note: 'options/input.json' in local storage
        FileHandle file = Gdx.files.local(INPUTACTIONS_FILE);
        file.parent().mkdirs();
        file.writeString(root.prettyPrint(JsonWriter.OutputType.json, 80), false);
        Debug.logT("InputManager", "Mappings saved to " + file.path());
    }

    public int getBoundKey(InputAction action) {
        List<Integer> keys = keyboardMappings.get(action);
        return (keys != null && !keys.isEmpty()) ? keys.get(0) : -1;
    }

    public int getBoundButton(InputAction action) {
        List<Integer> buttons = controllerMappings.get(action);
        return (buttons != null && !buttons.isEmpty()) ? buttons.get(0) : -1;
    }

    public boolean hasConnectedController() {
        return Controllers.getControllers().size > 0;
    }

    public void setVirtualGamepad(boolean active) {
        this.isVirtualGamepad = active;
    }

    public boolean isUsingController() {
        return hasConnectedController() || isVirtualGamepad;
    }

    private void loadMappings() {
        // Set defaults first to ensure all actions have bindings (especially new ones)
        setDefaultMappings();

        // Priority: Local > Assets/Options > Assets/Data
        FileHandle file = Gdx.files.local(INPUTACTIONS_FILE);

        if (!file.exists()) {
            file = Gdx.files.internal("options/input.json");
        }

		if (!file.exists()) {
			Debug.logT("InputManager", "No config found at options/input.json, using defaults.");
			return;
		}

        try {
            JsonValue root = new Json().fromJson(null, file);
            for (JsonValue entry : root) {
                try {
                    InputAction action = InputAction.valueOf(entry.name());

                    // Keyboard
                    if (entry.has("keyboard")) {
                        List<Integer> keys = new ArrayList<>();
                        for (int key : entry.get("keyboard").asIntArray()) {
                            keys.add(key);
                        }
                        keyboardMappings.put(action, keys);
                    }

                    // Controller
                    if (entry.has("controller")) {
                        List<Integer> buttons = new ArrayList<>();
                        for (int btn : entry.get("controller").asIntArray()) {
                            buttons.add(btn);
                        }
                        controllerMappings.put(action, buttons);
                    }
                } catch (IllegalArgumentException e) {
                    Debug.logT("InputManager", "Unknown action in config: " + entry.name());
                }
            }
            Debug.logT("InputManager", "Mappings loaded.");
        } catch (Exception e) {
            Debug.logT("InputManager", "Failed to load mappings: " + e.getMessage());
            e.printStackTrace();
            setDefaultMappings();
        }
    }

    private void setDefaultMappings() {
        // WASD / Arrow Keys
        mapK(InputAction.MOVE_UP, Input.Keys.W, Input.Keys.UP);
        mapK(InputAction.MOVE_DOWN, Input.Keys.S, Input.Keys.DOWN);
        mapK(InputAction.MOVE_LEFT, Input.Keys.A, Input.Keys.LEFT);
        mapK(InputAction.MOVE_RIGHT, Input.Keys.D, Input.Keys.RIGHT);

        mapK(InputAction.ATTACK, Input.Keys.J, Input.Keys.BUTTON_X); // X
        mapK(InputAction.INTERACT, Input.Keys.SPACE, Input.Keys.BUTTON_A); // A
        mapK(InputAction.SKILL, Input.Keys.H, Input.Keys.BUTTON_R2); // RT
        mapK(InputAction.BACK, Input.Keys.ESCAPE, Input.Keys.DEL, Input.Keys.BUTTON_B); // B

        mapK(InputAction.MAP, Input.Keys.M, Input.Keys.BUTTON_Y); // Y
        mapK(InputAction.BAG, Input.Keys.E, Input.Keys.BUTTON_R1); // RB
        mapK(InputAction.PAUSE, Input.Keys.P, Input.Keys.BUTTON_START); // Start

        mapK(InputAction.TAB, Input.Keys.TAB, Input.Keys.BUTTON_L2); // LT
        mapK(InputAction.QUICK_SLOT, Input.Keys.Q, Input.Keys.BUTTON_L1); // LB
        mapK(InputAction.SAVE, Input.Keys.F5); // Home
        
        mapK(InputAction.LOAD_GAME, Input.Keys.F9);
        mapK(InputAction.RESET_MAP, Input.Keys.R);
        
        // UI Navigation Defaults
        // Re-use Move keys + Arrows for UI
        mapK(InputAction.UI_UP, Input.Keys.W, Input.Keys.UP);
        mapK(InputAction.UI_DOWN, Input.Keys.S, Input.Keys.DOWN);
        mapK(InputAction.UI_LEFT, Input.Keys.A, Input.Keys.LEFT);
        mapK(InputAction.UI_RIGHT, Input.Keys.D, Input.Keys.RIGHT);
        mapK(InputAction.UI_CONFIRM, Input.Keys.ENTER, Input.Keys.SPACE, Input.Keys.J, Input.Keys.BUTTON_A);
        mapK(InputAction.UI_CANCEL, Input.Keys.ESCAPE, Input.Keys.BACKSPACE, Input.Keys.BUTTON_B);

        // Controller Defaults (Xbox)
        mapC(InputAction.ATTACK, BUTTON_X);
        mapC(InputAction.INTERACT, BUTTON_A);
        mapC(InputAction.SKILL, BUTTON_RB); // Mapping Skill to RB for now (User said RT, but triggers are complex)
        mapC(InputAction.BACK, BUTTON_B);
        mapC(InputAction.MAP, BUTTON_Y);
        mapC(InputAction.BAG, BUTTON_LB); // Mapping Bag to LB
        mapC(InputAction.PAUSE, BUTTON_START);
        mapC(InputAction.TAB, BUTTON_L3); // ?
        mapC(InputAction.QUICK_SLOT, BUTTON_R3); // ?
        
        // D-Pad (Standard Xbox)
        mapC(InputAction.MOVE_UP, BUTTON_DPAD_UP, AXIS_LEFT_UP);
        mapC(InputAction.MOVE_DOWN, BUTTON_DPAD_DOWN, AXIS_LEFT_DOWN);
        mapC(InputAction.MOVE_LEFT, BUTTON_DPAD_LEFT, AXIS_LEFT_LEFT);
        mapC(InputAction.MOVE_RIGHT, BUTTON_DPAD_RIGHT, AXIS_LEFT_RIGHT);
        
        // D-Pad (Switch Pro / Generic)
        // Adding 11-14 as fallback based on user report
        mapC(InputAction.MOVE_UP, SWITCH_DPAD_UP);
        mapC(InputAction.MOVE_DOWN, SWITCH_DPAD_DOWN);
        mapC(InputAction.MOVE_LEFT, SWITCH_DPAD_LEFT);
        mapC(InputAction.MOVE_RIGHT, SWITCH_DPAD_RIGHT);
        
        // UI Navigation (Controller) - Add Sticks too
        mapC(InputAction.UI_UP, BUTTON_DPAD_UP, AXIS_LEFT_UP, SWITCH_DPAD_UP);
        mapC(InputAction.UI_DOWN, BUTTON_DPAD_DOWN, AXIS_LEFT_DOWN, SWITCH_DPAD_DOWN);
        mapC(InputAction.UI_LEFT, BUTTON_DPAD_LEFT, AXIS_LEFT_LEFT, SWITCH_DPAD_LEFT);
        mapC(InputAction.UI_RIGHT, BUTTON_DPAD_RIGHT, AXIS_LEFT_RIGHT, SWITCH_DPAD_RIGHT);
    }
    
    public String getButtonName(int code) {
        if(code == BUTTON_A) return "A";
        if(code == BUTTON_B) return "B";
        if(code == BUTTON_X) return "X";
        if(code == BUTTON_Y) return "Y";
        if(code == BUTTON_LB) return "LB";
        if(code == BUTTON_RB) return "RB";
        if(code == BUTTON_BACK) return "BACK";
        if(code == BUTTON_START) return "START";
        if(code == BUTTON_L3) return "L3";
        if(code == BUTTON_R3) return "R3";
        
        // Use dynamic values
        if(code == BUTTON_DPAD_UP) return "DPAD_UP (" + code + ")";
        if(code == BUTTON_DPAD_DOWN) return "DPAD_DOWN (" + code + ")";
        if(code == BUTTON_DPAD_LEFT) return "DPAD_LEFT (" + code + ")";
        if(code == BUTTON_DPAD_RIGHT) return "DPAD_RIGHT (" + code + ")";
        
        return "Unknown";
    }

    private void mapK(InputAction action, int... keys) {
        List<Integer> list = keyboardMappings.computeIfAbsent(action, k -> new ArrayList<>());
        for (int k : keys) list.add(k);
    }

    private void mapC(InputAction action, int... buttons) {
        List<Integer> list = controllerMappings.computeIfAbsent(action, k -> new ArrayList<>());
        for (int b : buttons) list.add(b);
    }

    public boolean isPressed(InputAction action) {
        // Check Keyboard
        List<Integer> keys = keyboardMappings.get(action);
        if (keys != null) {
            for (int key : keys) {
                if (Gdx.input.isKeyPressed(key)) {
                    setInputMode(InputMode.KEYBOARD);
                    return true;
                }
            }
        }

        // Check Controller
        List<Integer> buttons = controllerMappings.get(action);
        if (buttons != null) {
            for (int btn : buttons) {
                if (currentButtons.contains(btn)) {
                    setInputMode(InputMode.KEYBOARD);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isJustPressed(InputAction action) {
        // Check Keyboard
        List<Integer> keys = keyboardMappings.get(action);
        if (keys != null) {
            for (int key : keys) {
                if (Gdx.input.isKeyJustPressed(key)) {
                    setInputMode(InputMode.KEYBOARD);
                    Debug.logT("InputManager", "Action JustPressed: " + action + " (Key: " + Input.Keys.toString(key) + ")");
                    return true;
                }
            }
        }
        
        // Check Controller
        List<Integer> buttons = controllerMappings.get(action);
        if (buttons != null) {
            for (int btn : buttons) {
                if (currentButtons.contains(btn) && !previousButtons.contains(btn)) {
                    setInputMode(InputMode.KEYBOARD);
                    Debug.logT("InputManager", "Action JustPressed: " + action + " (Btn: " + btn + ")");
                    return true;
                }
            }
        }

        return false;
    }
}
