import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.UUID;

public final class SilverDragonEgg extends JavaPlugin implements Listener {

    private NamespacedKey eggKey;

    @Override
    public void onEnable() {
        eggKey = new NamespacedKey(this, "infinity_core");
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::updateHandEffects, 20L, 20L);
    }

    private void updateHandEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isHoldingCore(player.getInventory().getItemInMainHand()) || 
                isHoldingCore(player.getInventory().getItemInOffHand())) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 80, 1, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1, false, false, false));
            }
        }
    }

    private boolean isHoldingCore(ItemStack item) {
        if (item == null || item.getType() != Material.DRAGON_EGG) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(eggKey, PersistentDataType.STRING);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (player.getHealth() - event.getFinalDamage() <= 0) {
            boolean inMain = isHoldingCore(player.getInventory().getItemInMainHand());
            boolean inOff = isHoldingCore(player.getInventory().getItemInOffHand());

            if (inMain || inOff) {
                event.setCancelled(true);
                player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());

                if (inMain) player.getInventory().setItemInMainHand(null);
                else player.getInventory().setItemInOffHand(null);

                player.playEffect(org.bukkit.EntityEffect.TOTEM_RESURRECT);
                player.sendMessage(ChatColor.RED + "Ядро Вечности спасло вашу жизнь и разрушилось!");
                
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.sendMessage(ChatColor.YELLOW + "Вы были возвращены на спавн из пустоты.");
                }
            }
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        if (isHoldingCore(inv.getItem(0)) && inv.getItem(1) != null && inv.getItem(1).getType() != Material.AIR) {
            event.setResult(null);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 1 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден.");
                return true;
            }
            ItemStack core = new ItemStack(Material.DRAGON_EGG);
            ItemMeta meta = core.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Ядро Вечности");
                meta.getPersistentDataContainer().set(eggKey, PersistentDataType.STRING, UUID.randomUUID().toString());
                core.setItemMeta(meta);
            }
            target.getInventory().addItem(core);
            sender.sendMessage(ChatColor.GREEN + "Ядро выдано игроку " + target.getName());
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Использование: /silverdragonegg give <ник>");
        return true;
    }
}
