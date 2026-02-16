package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/**
 * 统一管理所有音频资源的路径与加载
 */
public class AudioAssets {
    // Music
    public static final String MUSIC_DEFEAT = "packs/Quilly-Crawler/audio/music/Defeat.mp3";
    public static final String MUSIC_LASER_QUEST = "packs/Quilly-Crawler/audio/music/Laser Quest Loop.ogg";
    public static final String MUSIC_QUANTUM = "packs/Quilly-Crawler/audio/music/Quantum Loop.ogg";
    public static final String MUSIC_TAKE_COVER = "packs/Quilly-Crawler/audio/music/Take Cover.ogg";
    public static final String MUSIC_MOUNTAINS = "packs/Quilly-Crawler/audio/music/The Mountains Loop.ogg";
    public static final String MUSIC_SOLVE_THIS = "packs/Quilly-Crawler/audio/music/Try and Solve This.ogg";
    public static final String MUSIC_VICTORY = "packs/Quilly-Crawler/audio/music/Victory.mp3";

    // Sounds
    public static final String SFX_DRAGON_GROWL_00 = "packs/Quilly-Crawler/audio/sounds/Dragon_Growl_00.mp3";
    public static final String SFX_DRAGON_GROWL_01 = "packs/Quilly-Crawler/audio/sounds/Dragon_Growl_01.mp3";
    public static final String SFX_GOBLIN_03 = "packs/Quilly-Crawler/audio/sounds/Goblin_03.mp3";
    public static final String SFX_AMBIENCE_CAVE = "packs/Quilly-Crawler/audio/sounds/ambience_cave.mp3";
    public static final String SFX_CHEST_OPEN = "packs/Quilly-Crawler/audio/sounds/chest_open.mp3";
    public static final String SFX_DROP = "packs/Quilly-Crawler/audio/sounds/drop.mp3";
    public static final String SFX_MENU_BACK = "packs/Quilly-Crawler/audio/sounds/menu_back.mp3";
    public static final String SFX_MENU_SELECT = "packs/Quilly-Crawler/audio/sounds/menu_select.mp3";
    public static final String SFX_MENU_SELECT_2 = "packs/Quilly-Crawler/audio/sounds/menu_select_2.mp3";
    public static final String SFX_POWER_UP = "packs/Quilly-Crawler/audio/sounds/power_up_12.wav";
    public static final String SFX_DEATH = "packs/Quilly-Crawler/audio/sounds/sfx_death.wav";
    public static final String SFX_DEFEND = "packs/Quilly-Crawler/audio/sounds/sfx_defend_01.wav";
    public static final String SFX_EXPLOSION = "packs/Quilly-Crawler/audio/sounds/sfx_explosion.wav";
    public static final String SFX_FLAME = "packs/Quilly-Crawler/audio/sounds/sfx_flame.wav";
    public static final String SFX_HEAL = "packs/Quilly-Crawler/audio/sounds/sfx_heal.wav";
    public static final String SFX_PROTECT = "packs/Quilly-Crawler/audio/sounds/sfx_protect.wav";
    public static final String SFX_PROTECT_CAST = "packs/Quilly-Crawler/audio/sounds/sfx_protect_cast.wav";
    public static final String SFX_PUNCH = "packs/Quilly-Crawler/audio/sounds/sfx_punch_01.wav";
    public static final String SFX_TRANSFORM = "packs/Quilly-Crawler/audio/sounds/sfx_transform_demon.mp3";

    public static void loadAll(AssetManager manager) {
        // Load Music
        manager.load(MUSIC_DEFEAT, Music.class);
        manager.load(MUSIC_LASER_QUEST, Music.class);
        manager.load(MUSIC_QUANTUM, Music.class);
        manager.load(MUSIC_TAKE_COVER, Music.class);
        manager.load(MUSIC_MOUNTAINS, Music.class);
        manager.load(MUSIC_SOLVE_THIS, Music.class);
        manager.load(MUSIC_VICTORY, Music.class);

        // Load Sounds
        manager.load(SFX_DRAGON_GROWL_00, Sound.class);
        manager.load(SFX_DRAGON_GROWL_01, Sound.class);
        manager.load(SFX_GOBLIN_03, Sound.class);
        manager.load(SFX_AMBIENCE_CAVE, Sound.class);
        manager.load(SFX_CHEST_OPEN, Sound.class);
        manager.load(SFX_DROP, Sound.class);
        manager.load(SFX_MENU_BACK, Sound.class);
        manager.load(SFX_MENU_SELECT, Sound.class);
        manager.load(SFX_MENU_SELECT_2, Sound.class);
        manager.load(SFX_POWER_UP, Sound.class);
        manager.load(SFX_DEATH, Sound.class);
        manager.load(SFX_DEFEND, Sound.class);
        manager.load(SFX_EXPLOSION, Sound.class);
        manager.load(SFX_FLAME, Sound.class);
        manager.load(SFX_HEAL, Sound.class);
        manager.load(SFX_PROTECT, Sound.class);
        manager.load(SFX_PROTECT_CAST, Sound.class);
        manager.load(SFX_PUNCH, Sound.class);
        manager.load(SFX_TRANSFORM, Sound.class);
    }
}
