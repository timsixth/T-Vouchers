package pl.timsixth.vouchers.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import pl.timsixth.vouchers.config.ConfigFile;
import pl.timsixth.vouchers.enums.ActionClickType;
import pl.timsixth.vouchers.manager.MenuManager;
import pl.timsixth.vouchers.manager.PrepareToProcessManager;
import pl.timsixth.vouchers.manager.VoucherManager;
import pl.timsixth.vouchers.manager.process.IProcessManager;
import pl.timsixth.vouchers.model.PrepareToProcess;
import pl.timsixth.vouchers.model.Voucher;
import pl.timsixth.vouchers.model.menu.ClickAction;
import pl.timsixth.vouchers.model.menu.Menu;
import pl.timsixth.vouchers.model.menu.MenuItem;
import pl.timsixth.vouchers.model.process.*;
import pl.timsixth.vouchers.util.ChatUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class InventoryClickListener implements Listener {

    private final MenuManager menuManager;
    private final IProcessManager<CreationProcess> createProcessManager;

    private final IProcessManager<DeleteProcess> deleteProcessManager;

    private final IProcessManager<EditProcess> editProcessManager;

    private final VoucherManager voucherManager;

    private final PrepareToProcessManager prepareToProcessManager;

    @EventHandler
    private void onClick(InventoryClickEvent event) throws IOException {
        Player player = (Player) event.getWhoClicked();

        for (Menu menu : menuManager.getMenus()) {
            if (event.getView().getTitle().equalsIgnoreCase(ChatUtil.chatColor(menu.getDisplayName()))) {
                actionsInMenu(event, menu, player);
            }
        }
    }

    private void actionsInMenu(InventoryClickEvent event, Menu mainMenu, Player player) throws IOException {
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        for (MenuItem menuItem : mainMenu.getItems()) {
            if (event.getRawSlot() == menuItem.getSlot()) {
                if (menuItem.getClickAction() == null || menuItem.getClickAction().getActionClickType() == ActionClickType.NONE) {
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.OPEN_MENU) {
                    Menu menu = menuManager.getMenuByName(menuItem.getClickAction().getCalledAction().get(0));
                    if (menu != null) {
                        player.openInventory(menuManager.createMenu(menu));
                    } else {
                        player.sendMessage(ChatUtil.chatColor("&cMenu doesn't exists"));
                        event.setCancelled(true);
                    }
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.CLOSE_MENU) {
                    String actionArgs = menuItem.getClickAction().getCalledAction().get(0);
                    if (actionArgs.equalsIgnoreCase("open_chat")) {
                        CreationProcess creationProcess = new CreationProcess(player.getUniqueId());
                        createProcessManager.startProcess(creationProcess);
                        player.sendMessage(ConfigFile.TYPE_VOUCHER_NAME);
                    }
                    player.closeInventory();
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.NONE_ENCHANTS) {
                    if (createProcessManager.isProcessedByUser(createProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        setNoneEnchants(player, createProcessManager);
                    } else if (editProcessManager.isProcessedByUser(editProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        setNoneEnchants(player, editProcessManager);
                    }
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.CHOOSE_ENCHANT) {
                    if (createProcessManager.isProcessedByUser(createProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        chooseEnchant(player, menuItem, createProcessManager);
                    } else if (editProcessManager.isProcessedByUser(editProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        chooseEnchant(player, menuItem, editProcessManager);
                    }
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.CHOOSE_LEVEL) {
                    if (createProcessManager.isProcessedByUser(createProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        chooseLevel(player, menuItem, createProcessManager);
                    } else if (editProcessManager.isProcessedByUser(editProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        chooseLevel(player, menuItem, editProcessManager);
                    }
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.CREATE_VOUCHER) {
                    if (!createProcessManager.isProcessedByUser(createProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        event.setCancelled(true);
                        return;
                    }
                    CreationProcess creationProcess = createProcessManager.getProcessByUser(player.getUniqueId());
                    createProcessManager.saveProcess(creationProcess);

                    player.sendMessage(ConfigFile.CREATED_VOUCHER);
                    player.closeInventory();
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.OPEN_MENU_AND_GENERATED_ITEMS) {
                    Menu listOfAllVoucherMenu = menuManager.getMenuByName(menuItem.getClickAction().getCalledAction().get(0));
                    List<Voucher> voucherList = voucherManager.getVoucherList();
                    Set<MenuItem> menuItems = listOfAllVoucherMenu.getItems();
                    menuItems.clear();
                    for (int i = 0; i < voucherList.size(); i++) {
                        Voucher voucher = voucherList.get(i);
                        MenuItem voucherMenuItem = new MenuItem(i, Material.PAPER, ChatUtil.chatColor(voucher.getDisplayName()), ChatUtil.chatColor(voucher.getLore()));
                        voucherMenuItem.setClickAction(new ClickAction(ActionClickType.MANAGE_VOUCHER, new ArrayList<>()));
                        voucherMenuItem.setLocalizedName(voucher.getName());
                        if (voucher.getEnchantments() != null) {
                            voucherMenuItem.setEnchantments(voucher.getEnchantments());
                        } else {
                            voucherMenuItem.setEnchantments(new HashMap<>());
                        }
                        menuItems.add(voucherMenuItem);
                    }
                    listOfAllVoucherMenu.setItems(menuItems);
                    player.openInventory(menuManager.createMenu(listOfAllVoucherMenu));
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.MANAGE_VOUCHER) {
                    String voucherName = menuItem.getLocalizedName();
                    PrepareToProcess prepareToProcess = new PrepareToProcess(player.getUniqueId(), voucherName);
                    prepareToProcessManager.addNewLocalizedName(prepareToProcess);
                    Menu menu = menuManager.getMenuByName("manageVouchers");
                    player.openInventory(menuManager.createMenu(menu));
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.EDIT_VOUCHER) {
                    if (editProcessManager.isProcessedByUser(editProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        event.setCancelled(true);
                        return;
                    }
                    String actionArgs = menuItem.getClickAction().getCalledAction().get(0);
                    if (actionArgs.equalsIgnoreCase("open_chat")) {
                        Voucher voucher = voucherManager.getVoucher(prepareToProcessManager.getPrepareToProcess(player.getUniqueId()).getLocalizeName());
                        EditProcess editProcess = new EditProcess(player.getUniqueId());
                        Voucher currentVoucher = new Voucher(voucher.getName(),null,null,null);
                        currentVoucher.setEnchantments(new HashMap<>());
                        editProcess.setCurrentVoucher(currentVoucher);
                        player.sendMessage(ConfigFile.TYPE_VOUCHER_DISPLAY_NAME);
                        editProcessManager.startProcess(editProcess);
                        player.closeInventory();
                    }
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.DELETE_VOUCHER) {
                    if (deleteProcessManager.isProcessedByUser(deleteProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        event.setCancelled(true);
                        return;
                    }
                    DeleteProcess deleteProcess = new DeleteProcess(player.getUniqueId());
                    Voucher currentVoucher = voucherManager.getVoucher(prepareToProcessManager.getPrepareToProcess(player.getUniqueId()).getLocalizeName());
                    deleteProcess.setCurrentVoucher(currentVoucher);
                    deleteProcessManager.startProcess(deleteProcess);

                    deleteProcessManager.saveProcess(deleteProcess);

                    player.sendMessage(ConfigFile.DELETED_VOUCHER);
                    player.closeInventory();
                    event.setCancelled(true);
                } else if (menuItem.getClickAction().getActionClickType() == ActionClickType.REPLACE_VOUCHER) {
                    if (!editProcessManager.isProcessedByUser(editProcessManager.getProcessByUser(player.getUniqueId()), player)) {
                        event.setCancelled(true);
                        return;
                    }
                    EditProcess editProcess = editProcessManager.getProcessByUser(player.getUniqueId());
                    editProcessManager.saveProcess(editProcess);
                    player.sendMessage(ConfigFile.UPDATED_VOUCHER);
                    player.closeInventory();
                    event.setCancelled(true);
                }
            }
            event.setCancelled(true);
        }
        event.setCancelled(true);
    }

    private <T extends IProcess> void chooseLevel(Player player, MenuItem menuItem, IProcessManager<T> iProcessManager) {
        T process = iProcessManager.getProcessByUser(player.getUniqueId());
        Voucher currentVoucher = process.getCurrentVoucher();
        String actionArgs = menuItem.getClickAction().getCalledAction().get(0);
        AtomicReference<Enchantment> enchantment = new AtomicReference<>();
        currentVoucher.getEnchantments().forEach((enchantment1, integer) -> {
            if (integer == 0) {
                enchantment.set(enchantment1);
            }
        });
        currentVoucher.getEnchantments().replace(enchantment.get(), 0, Integer.parseInt(actionArgs));
    }

    private <T extends IProcess> void chooseEnchant(Player player, MenuItem menuItem, IProcessManager<T> iProcessManager) {
        T process = iProcessManager.getProcessByUser(player.getUniqueId());
        Voucher currentVoucher = process.getCurrentVoucher();
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        if (currentVoucher.getEnchantments() == null) {
            enchantments.put(Enchantment.getByName(ChatColor.stripColor(menuItem.getDisplayName())), 0);
            currentVoucher.setEnchantments(enchantments);
        }
        currentVoucher.getEnchantments().put(Enchantment.getByName(ChatColor.stripColor(menuItem.getDisplayName())), 0);
        Menu chooseLevelMenu = menuManager.getMenuByName("chooseEnchantLevel");
        player.openInventory(menuManager.createMenu(chooseLevelMenu));
    }

    private <T extends IProcess> void setNoneEnchants(Player player, IProcessManager<T> iProcessManager) throws IOException {
        T process = iProcessManager.getProcessByUser(player.getUniqueId());
        process.getCurrentVoucher().setEnchantments(null);
        iProcessManager.saveProcess(process);
        if (process instanceof CreationProcess){
            player.sendMessage(ConfigFile.CREATED_VOUCHER);
        }else if (process instanceof EditProcess){
            player.sendMessage(ConfigFile.UPDATED_VOUCHER);
        }
        player.closeInventory();
    }
}
