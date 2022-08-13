package pl.timsixth.vouchers.manager.process;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import pl.timsixth.vouchers.VouchersPlugin;
import pl.timsixth.vouchers.config.ConfigFile;
import pl.timsixth.vouchers.enums.ProcessType;
import pl.timsixth.vouchers.manager.LogsManager;
import pl.timsixth.vouchers.manager.PrepareToProcessManager;
import pl.timsixth.vouchers.manager.VoucherManager;
import pl.timsixth.vouchers.model.Log;
import pl.timsixth.vouchers.model.Voucher;
import pl.timsixth.vouchers.model.process.DeleteProcess;

import java.io.IOException;
import java.util.Date;

public class DeleteVoucherProcessManager extends AbstractProcessManager<DeleteProcess> {

    private final PrepareToProcessManager prepareToProcessManager;

    private final VouchersPlugin vouchersPlugin;

    public DeleteVoucherProcessManager(ConfigFile configFile, VoucherManager voucherManager, PrepareToProcessManager prepareToProcessManager, VouchersPlugin vouchersPlugin, LogsManager logsManager) {
        super(configFile, voucherManager, logsManager);
        this.prepareToProcessManager = prepareToProcessManager;
        this.vouchersPlugin = vouchersPlugin;
    }

    @Override
    public void saveProcess(DeleteProcess process) {
        if (process.getCurrentVoucher() == null) return;
        ConfigurationSection vouchersSection = getConfigFile().getYmlVouchers().getConfigurationSection("vouchers");
        Voucher currentVoucher = process.getCurrentVoucher();
        prepareToProcessManager.removeLocalizedName(prepareToProcessManager.getPrepareToProcess(process.getUserUuid()));
        getVoucherManager().getVoucherList().remove(currentVoucher);
        new BukkitRunnable(){
            @Override
            public void run() {
                vouchersSection.set(currentVoucher.getName(),null);
                try {
                    getConfigFile().getYmlVouchers().save(getConfigFile().getVouchersFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskLater(vouchersPlugin,2*20L);
        process.setContinue(false);
        cancelProcess(process);
        getLogsManager().addLog(new Log(process.getUserUuid(),"Voucher of name "+ process.getCurrentVoucher().getName()+" was deleted",new Date(), ProcessType.DELETE));
    }
}
