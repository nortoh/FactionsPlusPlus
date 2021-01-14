package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckClaimCommand {

    public boolean showClaim(CommandSender sender) {
        if (sender.hasPermission("mf.checkclaim")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String result = ChunkManager.getInstance().checkOwnershipAtPlayerLocation(player);
                if (result.equalsIgnoreCase("unclaimed")) {
                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LandIsUnclaimed"));
                    return true;
                }
                else {
                    player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("LandClaimedBy"), result));
                    return false;
                }
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.checkclaim"));
            return false;
        }
        return false;
    }

}
