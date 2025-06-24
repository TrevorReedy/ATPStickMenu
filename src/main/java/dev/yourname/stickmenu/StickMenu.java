package dev.yourname.stickmenu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse.*;



import java.util.List;

public class StickMenu extends JavaPlugin implements Listener {

    private FloodgateApi floodgateApi;

    @Override
    public void onEnable() {
        floodgateApi = FloodgateApi.getInstance();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StickMenu enabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.STICK && floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
            event.setCancelled(true);

            SimpleForm form = SimpleForm.builder()
                .title("Select Menu")
                .content("Choose an option:")
                .button("Teleport Requests")
                .button("Homes")
                .button("Add Home")
                .button("Warps")
                .validResultHandler(response -> {
                    int buttonId = response.clickedButtonId();
                        switch (buttonId) {
                            case 0 -> player.performCommand("tpa");
                            case 1 -> player.performCommand("homes");
                            case 2 -> player.performCommand("sethome");
                            case 3 -> warpMenu(player);
                        }
                })
                .build();

            floodgateApi.sendForm(player.getUniqueId(), form);
                        }
                    }




    public void warpMenu(Player player){
        SimpleForm warpForm = SimpleForm.builder()
        .title("Warp Menu")
        .content("Choose an warp:")
        .button("Spawn")
        .button("Shops")
        .button("Games")
        .button("PvP")
        .button("Elytra")
        .button("XP")
        .button("End Portal")
        .validResultHandler(response -> {
            int buttonId = response.clickedButtonId();
                switch (buttonId) {
                    case 0 -> player.performCommand("warp spawn");
                    case 1 -> player.performCommand("warp shops");
                    case 2 -> player.performCommand("warp games");
                    case 3 -> player.performCommand("warp pvp");
                    case 4 -> player.performCommand("warp elytra");
                    case 5 -> player.performCommand("warp xp");
                    case 6 -> player.performCommand("warp portal");
                }
        })
        .build();


        floodgateApi.sendForm(player.getUniqueId(), warpForm);

            }
    };
