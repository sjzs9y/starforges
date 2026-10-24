package com.starforge.registry;

import com.starforge.Starforge;
import com.starforge.whisper.WhisperAccelDisplay;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组的实体类型。
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Starforge.MOD_ID);

    /** 「加速」悬浮文字的载体实体。 */
    public static final DeferredHolder<EntityType<?>, EntityType<WhisperAccelDisplay>> ACCEL_DISPLAY =
            ENTITY_TYPES.register("accel_display",
                    () -> EntityType.Builder.<WhisperAccelDisplay>of(WhisperAccelDisplay::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(Starforge.MOD_ID, "accel_display").toString()));

    private ModEntities() {
    }
}
