package com.starforge;

import com.mojang.logging.LogUtils;
import com.starforge.client.ClientSetup;
import com.starforge.network.ModNetwork;
import com.starforge.registry.ModArmorMaterials;
import com.starforge.registry.ModBlocks;
import com.starforge.registry.ModCreativeTabs;
import com.starforge.registry.ModItems;
import com.starforge.whisper.WhisperEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Entry point of the Starforge mod.
 *
 * <p>NeoForge instantiates this class once, passing the mod event bus and the mod container.
 * Registration events live on the mod bus, gameplay events on {@link NeoForge#EVENT_BUS}.</p>
 */
@Mod(Starforge.MOD_ID)
public final class Starforge {

    public static final String MOD_ID = "starforge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Starforge(IEventBus modEventBus, ModContainer modContainer) {
        ModArmorMaterials.MATERIALS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(ModNetwork::register);

        NeoForge.EVENT_BUS.addListener(WhisperEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(WhisperEvents::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(WhisperEvents::onLivingChangeTarget);
        NeoForge.EVENT_BUS.addListener(WhisperEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(WhisperEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(WhisperEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(WhisperEvents::onLevelTick);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientSetup::registerKeys);
            ClientSetup.init();
        }

        LOGGER.info("Starforge is loading");
    }
}
