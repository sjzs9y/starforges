package com.starforge.whisper;

/**
 * The abilities that can be switched on and off for the "Whisper" item.
 *
 * <p>The ordinal is used on the wire (client -> server), so new entries should always be appended
 * at the end.</p>
 */
public enum WhisperAbility {

    // 主动能力：必须拿在主手才生效
    ONE_HIT("one_hit", "whisper.starforge.one_hit", false),

    // 被动能力：带在背包 / 副手 / 主手都生效
    INVINCIBLE("invincible", "whisper.starforge.invincible", true),
    FLIGHT("flight", "whisper.starforge.flight", true),
    NIGHT_VISION("night_vision", "whisper.starforge.night_vision", true),
    SATURATION("saturation", "whisper.starforge.saturation", true),
    WATER_BREATHING("water_breathing", "whisper.starforge.water_breathing", true),
    TRUE_INVISIBILITY("true_invisibility", "whisper.starforge.true_invisibility", true),

    // 挖掘模式：拿在主手时生效，两者互斥
    PRECISE("precise", "whisper.starforge.precise", false),
    LUCKY("lucky", "whisper.starforge.lucky", false),

    // 右键模式对应的总开关
    ACCELERATE("accelerate", "whisper.starforge.accelerate", false),
    DUPLICATE("duplicate", "whisper.starforge.duplicate", false);

    private static final WhisperAbility[] VALUES = values();

    private final String key;
    private final String translationKey;
    private final boolean passive;

    WhisperAbility(String key, String translationKey, boolean passive) {
        this.key = key;
        this.translationKey = translationKey;
        this.passive = passive;
    }

    public String getKey() {
        return this.key;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    /** {@code true} 表示物品只要带在身上（背包 / 副手 / 主手）就生效。 */
    public boolean isPassive() {
        return this.passive;
    }

    /** 精准模式和幸运模式不能同时开启。 */
    public WhisperAbility getExclusive() {
        return switch (this) {
            case PRECISE -> LUCKY;
            case LUCKY -> PRECISE;
            default -> null;
        };
    }

    public static WhisperAbility[] all() {
        return VALUES;
    }

    public static WhisperAbility byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }
}
