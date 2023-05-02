package pl.timsixth.vouchers.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import pl.timsixth.vouchers.model.menu.MenuItem;
import pl.timsixth.vouchers.model.menu.action.custom.impl.ManageVoucherAction;
import pl.timsixth.vouchers.util.ChatUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Voucher implements IGenerable {

    private String name;
    private String command;
    private List<String> lore;
    private String displayName;
    private Material material;
    private Map<Enchantment, Integer> enchantments;

    public Voucher(String name, String command, List<String> lore, String displayName, Material material) {
        this.name = name;
        this.command = command;
        this.lore = lore;
        this.displayName = displayName;
        this.material = material;
    }

    @Override
    public MenuItem getGeneratedItem(int slot) {
        MenuItem menuItem = new MenuItem(slot, material, ChatUtil.chatColor(displayName), ChatUtil.chatColor(lore));
        menuItem.setAction(new ManageVoucherAction());
        menuItem.setLocalizedName(name);
        if (enchantments != null) {
            menuItem.setEnchantments(enchantments);
        } else {
            menuItem.setEnchantments(new HashMap<>());
        }
        return menuItem;
    }
}
