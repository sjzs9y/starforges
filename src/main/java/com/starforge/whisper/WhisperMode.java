package com.starforge.whisper;

/**
 * 右键模式。潜行右键在三个模式之间循环，物品说明里会显示当前模式。
 */
public enum WhisperMode {

    ATTACK("attack", "mode.starforge.attack", WhisperAbility.ONE_HIT),
    ACCELERATE("accelerate", "mode.starforge.accelerate", WhisperAbility.ACCELERATE),
    DUPLICATE("duplicate", "mode.starforge.duplicate", WhisperAbility.DUPLICATE);

    private static final WhisperMode[] VALUES = values();

    private final String key;
    private final String translationKey;
    private final WhisperAbility requiredToggle;

    WhisperMode(String key, String translationKey, WhisperAbility requiredToggle) {
        this.key = key;
        this.translationKey = translationKey;
        this.requiredToggle = requiredToggle;
    }

    public String getKey() {
        return this.key;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    /** 这个模式对应的总开关；开关关掉时右键不会生效。 */
    public WhisperAbility getRequiredToggle() {
        return this.requiredToggle;
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
