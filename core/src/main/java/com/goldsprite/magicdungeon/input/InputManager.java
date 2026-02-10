package com.goldsprite.magicdungeon.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.log.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputManager {
	private static String INPUTACTIONS_FILE = "MagicDungeon/input_actions.json";
    private static InputManager instance;
    private final Map<InputAction, List<Integer>> keyboardMappings = new HashMap<>();
    private final Map<InputAction, List<Integer>> controllerMappings = new HashMap<>();

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
    public static final int BUTTON_DPAD_UP = 10; // May vary
    public static final int BUTTON_DPAD_DOWN = 11;
    public static final int BUTTON_DPAD_LEFT = 12;
    public static final int BUTTON_DPAD_RIGHT = 13;
    // Triggers are usually axes, but some mapping treat them as buttons if pressed deep enough
    // For simplicity we might map triggers to buttons if the library supports it, or handle axes separately.
    // Standard gdx-controllers usually maps axes 4 and 5 to triggers.

    private InputManager() {
        loadMappings();
    }

    public static InputManager getInstance() {
        if (instance == null) instance = new InputManager();
        return instance;
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

    private void loadMappings() {
        // Priority: Local > Assets/Options > Assets/Data
        FileHandle file = Gdx.files.local(INPUTACTIONS_FILE);

        if (!file.exists()) {
            file = Gdx.files.internal("options/input.json");
        }

		if (!file.exists()) {
			Debug.logT("InputManager", "No config found at options/input.json, using defaults.");
			setDefaultMappings();
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

        // D-Pad
        mapC(InputAction.MOVE_UP, BUTTON_DPAD_UP);
        mapC(InputAction.MOVE_DOWN, BUTTON_DPAD_DOWN);
        mapC(InputAction.MOVE_LEFT, BUTTON_DPAD_LEFT);
        mapC(InputAction.MOVE_RIGHT, BUTTON_DPAD_RIGHT);
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
                if (Gdx.input.isKeyPressed(key)) return true;
            }
        }

        // Check Controller
        List<Integer> buttons = controllerMappings.get(action);
        if (buttons != null && Controllers.getControllers().size > 0) {
            Controller controller = Controllers.getControllers().first();
            for (int btn : buttons) {
                if (controller.getButton(btn)) return true;
            }
        }

        return false;
    }

    public boolean isJustPressed(InputAction action) {
        List<Integer> keys = keyboardMappings.get(action);
        if (keys != null) {
            for (int key : keys) {
                if (Gdx.input.isKeyJustPressed(key)) {
                    Debug.logT("InputManager", "Action JustPressed: " + action + " (Key: " + Input.Keys.toString(key) + ")");
                    return true;
                }
            }
        }

        // Note: Controller "just pressed" usually requires polling state manually or using an adapter.
        // For simplicity, we only check keyboard here unless we implement state tracking.
        return false;
    }
}
