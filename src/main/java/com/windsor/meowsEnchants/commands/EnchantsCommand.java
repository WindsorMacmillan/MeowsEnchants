package com.windsor.meowsEnchants.commands;

import com.windsor.meowsEnchants.gui.EnchantBrowser;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /enchants 命令，为玩家打开附魔查询界面。
 */
public final class EnchantsCommand implements BasicCommand {

    private final EnchantBrowser browser;

    public EnchantsCommand(@NotNull EnchantBrowser browser) {
        this.browser = browser;
    }

    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        CommandSender sender = commandSourceStack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
            return;
        }
        browser.open(player, 0);
    }
}
