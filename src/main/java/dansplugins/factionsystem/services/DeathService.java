package dansplugins.factionsystem.services;

import java.util.ArrayList;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.google.inject.Singleton;

@Singleton
public class DeathService {

    public ItemStack getHead(OfflinePlayer loser) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skull = (SkullMeta) item.getItemMeta();
        if (skull == null) {
            return null;
        }
        skull.setDisplayName(loser.getName() + "'s head.");
        skull.setOwningPlayer(loser);
        ArrayList<String> lore = new ArrayList<>();
        lore.add("Won in a duel against " + Objects.requireNonNull(loser).getName() + ".");
        skull.setLore(lore);
        item.setItemMeta(skull);
        return item;
    }
}
