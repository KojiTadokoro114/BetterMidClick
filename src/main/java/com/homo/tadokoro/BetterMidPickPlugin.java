package com.homo.tadokoro;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterMidPickPlugin extends JavaPlugin implements Listener {

    public static BetterMidPickPlugin plugin;

    @Override
    public void onEnable() {
        plugin = this;
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ServerPlayer sp = ((CraftPlayer) event.getPlayer()).getHandle();
        sp.connection.connection.channel.pipeline().addBefore("packet_handler", "bettermidpick_handler", new PickUpHandler(sp));
    }
}
