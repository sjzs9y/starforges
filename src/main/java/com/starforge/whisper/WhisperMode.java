package com.starforge.whisper;

/**
 * 右键模式。潜行右键在三个模式之间循环，物品说明里会显示当前模式。
 */
public enum WhisperMode {

    ATTACK("attack", "mode.starforge.attack"),
    ACCELERATE("accelerate", "mode.starforge.accelerate"),
    DUPLICATE("duplicate", "mode.starforge.duplicate");

    private static final WhisperMode[] VALUES = values();

    private final String key;
    private final String translationKey;

    WhisperMode(String key, String translationKey) {
        this.key = key;
        this.translationKey = translationKey;
    }

    public String getKey() {
        return this.key;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public WhisperMode next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public static WhisperMode byKey(String key) {
        for (WhisperMode mode : VALUES) {
            if (mode.key.equals(key)) {
                return mode;
            }
        }
        return ATTACK;
    }
}
