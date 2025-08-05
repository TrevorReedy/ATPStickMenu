package dev.yourname.stickmenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse.*;
import org.bukkit.inventory.Inventory;


import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class StickMenu extends JavaPlugin implements Listener {

    private FloodgateApi floodgateApi;
    private final Set<UUID> waitingForInput = new HashSet<>();
    private final Map<UUID, String> pendingTrades = new HashMap<>();
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    );

    @Override
    public void onEnable() {
        floodgateApi = FloodgateApi.getInstance();
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("tradehead")).setExecutor(this);
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
                .button("Trade Heads")
                .validResultHandler(response -> {
                    int buttonId = response.clickedButtonId();
                    switch (buttonId) {
                        case 0 -> player.performCommand("tpa");
                        case 1 -> player.performCommand("homes");
                        case 2 -> player.performCommand("sethome");
                        case 3 -> warpMenu(player);
                        case 4 -> initiateHeadTrade(player);
                    }
                })
                .build();

            floodgateApi.sendForm(player.getUniqueId(), form);
        }
    }

    private void initiateHeadTrade(Player player) {
        if (floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
            // Bedrock players: Show UUID input form (using CustomForm for input)
            CustomForm form = CustomForm.builder()
                .title("Head Trade")
                .label("Enter Head UUID below (or type 'cancel'):\nYou need 5 random heads.")
                .input("Head UUID", "123e4567-e89b-12d3...")
                .validResultHandler(response -> {
                    CustomFormResponse res = (CustomFormResponse) response;
                    String input = res.getInput(0).trim(); //The method getInput(int) from the type CustomFormResponse is deprecatedJava(67108967)
                    if (input.equalsIgnoreCase("cancel")) {
                        player.sendMessage("Trade cancelled.");
                    } else if (isValidHeadUUID(input)) {
                        openHeadTradeGUI(player, input);
                    } else {
                        player.sendMessage("Invalid UUID! Try: 123e4567-e89b-12d3-a456-426614174000");
                    }
                })
                .build();
            floodgateApi.sendForm(player.getUniqueId(), form);
        } else {
            // Java players: Use chat input
            player.sendMessage(ChatColor.YELLOW + "Enter Head UUID (or 'cancel'):");
            player.sendMessage(ChatColor.GRAY + "You need 5 random heads to trade.");
            waitingForInput.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!waitingForInput.contains(player.getUniqueId())) return;
        
        event.setCancelled(true);
        String input = event.getMessage().trim();
        
        Bukkit.getScheduler().runTask(this, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                waitingForInput.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Trade cancelled.");
                return;
            }
            
            if (isValidHeadUUID(input)) {
                waitingForInput.remove(player.getUniqueId());
                openHeadTradeGUI(player, input);
            } else {
                player.sendMessage(ChatColor.RED + "Invalid UUID! Format: 123e4567-e89b-12d3-a456-426614174000");
            }
        });
    }

    private boolean isValidHeadUUID(String uuid) {
        return UUID_PATTERN.matcher(uuid).matches();
    }

private void openHeadTradeGUI(Player player, String targetHeadUUID) {
    pendingTrades.put(player.getUniqueId(), targetHeadUUID);

    String guiTitle = ChatColor.BLUE + "Insert 5 Heads";
    Inventory gui = Bukkit.createInventory(null, 9, guiTitle);

    // Register a new listener specific to this player
    Listener listener = new Listener() {
        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player p)) return;

            // Compare by title (more reliable than object reference)
            String title = ChatColor.stripColor(event.getView().getTitle());
            if (!title.equalsIgnoreCase("Insert 5 Heads")) return;

            if (!pendingTrades.containsKey(p.getUniqueId())) return;

            String uuid = pendingTrades.remove(p.getUniqueId());

            int headCount = countPlayerHeads(event.getInventory());
            if (headCount >= 5) {
                executeHeadTrade(p, uuid, event.getInventory());
            } else {
                returnItems(p, event.getInventory());
                p.sendMessage(ChatColor.RED + "Trade cancelled. Needed 5 heads.");
            }

            InventoryCloseEvent.getHandlerList().unregister(this); // Just this listener
        }
    };

    Bukkit.getPluginManager().registerEvents(listener, this);
    player.openInventory(gui);
}


private int countPlayerHeads(Inventory gui) {
    if (gui == null) return 0; // Safety check
    int count = 0;
    for (ItemStack item : gui.getContents()) {
        if (item == null) continue;
        try {
            if (item.getType() == Material.PLAYER_HEAD) {
                count++;
            }
        } catch (Exception ignored) {
            // Prevent crash from corrupted items
        }
    }
    return count;
}


private void returnItems(Player player, Inventory gui) {
    if (gui == null) return;

    for (ItemStack item : gui.getContents()) {
        if (item == null) continue;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        leftover.values().forEach(remaining -> player.getWorld().dropItem(player.getLocation(), remaining));
    }
}

    private void executeHeadTrade(Player player, String headUUID, Inventory gui) {
        gui.clear();
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "hdb give " + player.getName() + " " + headUUID
                );
                player.sendMessage(ChatColor.GREEN + "Trade successful! You received your head.");
            }
        }.runTaskLater(this, 1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("tradehead")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /tradehead <HeadUUID>");
                player.sendMessage(ChatColor.GRAY + "Example: /tradehead 123e4567-e89b-12d3-a456-426614174000");
                return true;
            }
            
            if (isValidHeadUUID(args[0])) {
                openHeadTradeGUI(player, args[0]);
            } else {
                player.sendMessage(ChatColor.RED + "Invalid Head UUID format!");
            }
            return true;
        }
        return false;
    }

    public void warpMenu(Player player) {
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
}