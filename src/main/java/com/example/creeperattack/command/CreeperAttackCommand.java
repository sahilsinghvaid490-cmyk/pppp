package com.example.creeperattack.command;

import com.example.creeperattack.CreeperAttackPlugin;
import com.example.creeperattack.config.ConfigManager;
import com.example.creeperattack.manager.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles {@code /creeperattack <start|stop|reload>}, including permission
 * checks, argument validation, and tab completion.
 */
public final class CreeperAttackCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("start", "stop", "reload");
    private static final String PERMISSION = "creeperattack.admin";

    private final CreeperAttackPlugin plugin;
    private final ConfigManager configManager;
    private final EventManager eventManager;

    public CreeperAttackCommand(CreeperAttackPlugin plugin, ConfigManager configManager, EventManager eventManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(configManager.getMessage("invalid-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(configManager.getMessage("unknown-command"));
        }

        return true;
    }

    private void handleStart(CommandSender sender) {
        boolean started = eventManager.startManualEvent();
        if (started) {
            sender.sendMessage(configManager.getMessage("command-start-success"));
        } else {
            sender.sendMessage(configManager.getMessage("command-disabled"));
        }
    }

    private void handleStop(CommandSender sender) {
        boolean stopped = eventManager.stopManualEvent();
        if (stopped) {
            sender.sendMessage(configManager.getMessage("command-stop-success"));
        } else {
            sender.sendMessage(configManager.getMessage("event-not-active"));
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            configManager.reload();
            eventManager.start();
            sender.sendMessage(configManager.getMessage("reload-success"));
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to reload configuration: " + ex.getMessage());
            sender.sendMessage(configManager.getMessage("reload-failure"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission(PERMISSION)) {
            return new ArrayList<>();
        }

        String partial = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(partial)) {
                completions.add(sub);
            }
        }
        return completions;
    }
}
