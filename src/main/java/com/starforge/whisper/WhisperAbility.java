package com.starforge.whisper;

/**
 * The abilities that can be switched on and off for the "Whisper" item.
 *
 * <p>The ordinal is used on the wire (client -> server), so new entries should always be appended
 * at the end.</p>
 */
public enum WhisperAbility {

    INVINCIBLE("invincible", "whisper.starforge.invincible"),
    FLIGHT("flight", "whisper.starforge.flight"),
    ONE_HIT("one_hit", "whisper.starforge.one_hit"),
    NIGHT_VISION("night_vision", "whisper.starforge.night_vision"),
    SATURATION("saturation", "whisper.starforge.saturation");

    private static final WhisperAbility[] VALUES = values();

    private final String key;
    private final String translationKey;

    WhisperAbility(String key, String translationKey) {
        this.key = key;
        this.translationKey = translationKey;
    }

    public String getKey() {
        return this.key;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static WhisperAbility[] all() {
        return VALUES;
    }

    public static WhisperAbility byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }
}
