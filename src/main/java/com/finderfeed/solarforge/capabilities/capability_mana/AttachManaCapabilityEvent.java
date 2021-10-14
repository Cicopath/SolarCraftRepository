package com.finderfeed.solarforge.capabilities.capability_mana;

import com.finderfeed.solarforge.packet_handler.SolarForgePacketHandler;
import com.finderfeed.solarforge.packet_handler.packets.ToggleAlchemistPacket;
import com.finderfeed.solarforge.registries.items.ItemsRegister;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fmllegacy.network.NetworkDirection;


public class AttachManaCapabilityEvent {
    public static void attachCapabilities(final AttachCapabilitiesEvent<Entity> event){
        if (event.getObject() instanceof Player) {
            PlayerManaProvider provide = new PlayerManaProvider();
            event.addCapability(new ResourceLocation("solarforge", "solar_mana_level"), provide);
            //event.addListener(provide::invalidate);
        }
    }






    public static void tickEvent(final TickEvent.PlayerTickEvent event){
        Player player = event.player;
        if (!player.level.isClientSide && player.getCapability(CapabilitySolarMana.SOLAR_MANA_PLAYER).isPresent() && (event.phase == TickEvent.Phase.END)) {
            double mana = player.getCapability(CapabilitySolarMana.SOLAR_MANA_PLAYER).orElseThrow(RuntimeException::new).getMana();

                mana += 0.20d;
                if (player.getInventory().contains(ItemsRegister.SOLAR_MANA_AMULET.get().getDefaultInstance())){
                    mana+=0.20d;
                }
                if (!player.isDeadOrDying()) {
                    if (mana <= 3000) {
                        player.getCapability(CapabilitySolarMana.SOLAR_MANA_PLAYER).orElseThrow(RuntimeException::new).setMana(mana);
                    }else {
                        player.getCapability(CapabilitySolarMana.SOLAR_MANA_PLAYER).orElseThrow(RuntimeException::new).setMana(3000);
                    }
                        ServerPlayer playerServer = (ServerPlayer) player;
                    if ((event.player.level.getGameTime() % 10 == 0) ) {
                        SolarForgePacketHandler.INSTANCE.sendTo(new UpdateManaPacket(mana), playerServer.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                    if (player.getPersistentData().getBoolean("is_alchemist_toggled") && !player.isCreative() &&
                            player.getCapability(CapabilitySolarMana.SOLAR_MANA_PLAYER).orElseThrow(RuntimeException::new).getMana() >= 1
                    && player.getPersistentData().getBoolean("solar_forge_can_player_use_alchemist")){
                        player.getCapability(CapabilitySolarMana.SOLAR_MANA_PLAYER).orElseThrow(RuntimeException::new).setMana(
                        player.getCapability(CapabilitySolarMana.SOLAR_MANA_PLAYER).orElseThrow(RuntimeException::new).getMana()-1
                        );
                    }
                }





            if (player.getPersistentData().getBoolean("is_alchemist_toggled") && player.getPersistentData().getBoolean("solar_forge_can_player_use_alchemist")) {
                SolarForgePacketHandler.INSTANCE.sendTo(new ToggleAlchemistPacket(true), ((ServerPlayer)event.player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            } else {
                SolarForgePacketHandler.INSTANCE.sendTo(new ToggleAlchemistPacket(false), ((ServerPlayer)event.player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            }

        }
    }
}
