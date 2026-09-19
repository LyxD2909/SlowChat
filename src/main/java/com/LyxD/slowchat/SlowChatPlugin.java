package com.LyxD.slowchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SlowChatPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final ConcurrentMap<UUID, Long> lastChatTimes = new ConcurrentHashMap<>();
    private volatile long cooldownMillis;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("slowchat") != null) {
            getCommand("slowchat").setExecutor(this);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("slowchat.bypass")) {
            return;
        }

        long cooldown = cooldownMillis;

        if (cooldown <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastChat = lastChatTimes.get(player.getUniqueId());

        if (lastChat != null) {
            long remaining = cooldown - (now - lastChat);

            if (remaining > 0) {
                long seconds = (remaining + 999) / 1000;

                event.setCancelled(true);
                player.sendMessage(
                        Component.text(
                                "Please wait " + seconds + "s before chatting again."
                        )
                );
                return;
            }
        }

        lastChatTimes.put(player.getUniqueId(), now);
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp()) {
            sender.sendMessage(
                    Component.text("You must be an operator to use this command.")
            );
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(
                    Component.text("Usage: /slowchat <seconds>")
            );
            return true;
        }

        long seconds;

        try {
            seconds = Long.parseLong(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(
                    Component.text("Seconds must be a whole number.")
            );
            return true;
        }

        long maximumSeconds = (Long.MAX_VALUE - 999) / 1000;

        if (seconds < 0 || seconds > maximumSeconds) {
            sender.sendMessage(
                    Component.text(
                            "Seconds must be between 0 and "
                                    + maximumSeconds
                                    + "."
                    )
            );
            return true;
        }

        cooldownMillis = seconds * 1000;

        if (seconds == 0) {
            sender.sendMessage(
                    Component.text("Chat cooldown disabled.")
            );
        } else {
            sender.sendMessage(
                    Component.text(
                            "Chat cooldown set to " + seconds + "s."
                    )
            );
        }

        return true;
    }
                               }
