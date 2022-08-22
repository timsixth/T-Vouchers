package pl.timsixth.vouchers.command;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.timsixth.vouchers.config.ConfigFile;
import pl.timsixth.vouchers.config.Messages;
import pl.timsixth.vouchers.manager.MenuManager;
import pl.timsixth.vouchers.manager.VoucherManager;
import pl.timsixth.vouchers.model.Voucher;
import pl.timsixth.vouchers.util.ChatUtil;

@RequiredArgsConstructor
public class VoucherCommand implements CommandExecutor {

    private final VoucherManager voucherManager;
    private final MenuManager menuManager;

    private final ConfigFile configFile;

    private final Messages messages;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(configFile.getPermission())) {
            sender.sendMessage(messages.getNoPermission());
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(messages.getCorrectUse());
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatUtil.chatColor("&eVouchers:&7"));
                voucherManager.getVoucherList().forEach(voucher -> sender.sendMessage(ChatUtil.chatColor("&7 " + voucher.getName())));
            }else if (args[0].equalsIgnoreCase("gui")){
                if (sender instanceof Player){
                    Player player = (Player) sender;
                    player.openInventory(menuManager.createMenu(menuManager.getMenuByName("main")));
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender instanceof Player) {
                    if (voucherManager.voucherExists(voucherManager.getVoucher(args[1]))) {
                        Voucher voucher = voucherManager.getVoucher(args[1]);
                        ((Player) sender).getInventory().addItem(voucherManager.getItemVoucher(voucher));
                        sender.sendMessage(messages.getAddedVoucher());
                    } else {
                        sender.sendMessage(messages.getVoucherDoesntExists());
                    }
                } else {
                    sender.sendMessage(messages.getCorrectUse());
                    return true;
                }
            }else{
                Bukkit.getLogger().info("Player only use this command");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                if (voucherManager.voucherExists(voucherManager.getVoucher(args[1]))) {
                    Player other = Bukkit.getPlayer(args[2]);
                    if (other != null) {
                        Voucher voucher = voucherManager.getVoucher(args[1]);
                        other.getInventory().addItem(voucherManager.getItemVoucher(voucher));
                        other.sendMessage(messages.getAddedVoucher());
                        String message = messages.getAddedVoucherToOtherPlayer()
                                .replace("{PLAYER_NAME}", sender.getName());
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(messages.getOfflinePlayer());
                    }
                } else {
                    sender.sendMessage(messages.getVoucherDoesntExists());
                }
            } else {
                sender.sendMessage(messages.getCorrectUse());
                return true;
            }
        } else {
            sender.sendMessage(messages.getCorrectUse());
            return true;
        }
        return false;
    }
}
