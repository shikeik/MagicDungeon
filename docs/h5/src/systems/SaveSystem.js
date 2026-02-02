export class SaveSystem {
    static save(key, data) {
        try {
            const json = JSON.stringify(data);
            localStorage.setItem(key, json);
            console.log("Game Saved");
            return true;
        } catch (e) {
            console.error("Save failed", e);
            return false;
        }
    }

    static load(key) {
        try {
            const json = localStorage.getItem(key);
            if (!json) return null;
            return JSON.parse(json);
        } catch (e) {
            console.error("Load failed", e);
            return null;
        }
    }
}
