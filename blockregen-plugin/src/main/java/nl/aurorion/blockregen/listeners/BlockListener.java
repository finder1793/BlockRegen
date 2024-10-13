package nl.aurorion.blockregen.listeners;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.cryptomorin.xseries.XMaterial;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.api.BlockRegenBlockBreakEvent;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.preset.struct.drop.ExperienceDrop;
import nl.aurorion.blockregen.system.preset.struct.drop.ItemDrop;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import nl.aurorion.blockregen.system.region.struct.RegenerationRegion;
import nl.aurorion.blockregen.util.ItemUtil;
import nl.aurorion.blockregen.util.LocationUtil;
import nl.aurorion.blockregen.util.TextUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Log
public class BlockListener implements Listener {

    private final BlockRegen plugin;

    public BlockListener(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {

        // Respect cancels on higher priorities.
        // This should technically never happen as we have ignoreCancelled = false.
        if (event.isCancelled()) {
            log.fine("Event already cancelled.");
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if the block is regenerating already
        RegenerationProcess existingProcess = plugin.getRegenerationManager().getProcess(block);
        if (existingProcess != null) {

            // Remove the process
            if (hasBypass(player)) {
                plugin.getRegenerationManager().removeProcess(existingProcess);
                log.fine("Removed process in bypass.");
                return;
            }

            if (existingProcess.getRegenerationTime() > System.currentTimeMillis()) {
                log.fine(String.format("Block is regenerating. Process: %s", existingProcess));
                event.setCancelled(true);
                return;
            }
        }

        // Check bypass
        if (hasBypass(player)) {
            log.fine("Player has bypass.");
            return;
        }

        // Block data check
        if (plugin.getRegenerationManager().hasDataCheck(player)) {
            event.setCancelled(true);
            log.fine("Player has block check.");
            return;
        }

        // If the block is protected, do nothing.
        if (checkProtection(event)) {
            return;
        }

        World world = block.getWorld();

        boolean useRegions = plugin.getConfig().getBoolean("Use-Regions", false);
        RegenerationRegion region = plugin.getRegionManager().getRegion(block.getLocation());

        boolean isInWorld = plugin.getConfig().getStringList("Worlds-Enabled").contains(world.getName());
        boolean isInRegion = region != null;

        boolean isInZone = useRegions ? isInRegion : isInWorld;

        if (!isInZone) {
            return;
        }

        log.fine(String.format("Handling %s.", LocationUtil.locationToString(block.getLocation())));

        BlockPreset preset = plugin.getPresetManager().getPreset(block);

        boolean isConfigured = useRegions ? preset != null && region.hasPreset(preset.getName()) : preset != null;

        if (!isConfigured) {
            if (useRegions && preset != null && !region.hasPreset(preset.getName())) {
                log.fine(String.format("Region %s does not have preset %s configured.", region.getName(), preset.getName()));
            }

            if (plugin.getConfig().getBoolean("Disable-Other-Break")) {
                event.setCancelled(true);
                log.fine(String.format("%s is not a configured preset. Denied block break.", block.getType()));
                return;
            }

            log.fine(String.format("%s is not a configured preset.", block.getType()));
            return;
        }

        // Check region permissions
        if (isInRegion && lacksPermission(player, "blockregen.region", region.getName()) && !player.isOp()) {
            event.setCancelled(true);
            Message.PERMISSION_REGION_ERROR.send(event.getPlayer());
            log.fine(String.format("Player doesn't have permissions for region %s", region.getName()));
            return;
        }

        RegenerationProcess process = plugin.getRegenerationManager().createProcess(block, preset, isInRegion ? region.getName() : null);
        process(process, preset, event);
    }

    // Check for supported protection plugins' regions and settings.
    // If any of them are protecting this block, allow them to handle this and do nothing.
    private boolean checkProtection(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Towny
        if (plugin.getConfig().getBoolean("Towny-Support", true)
                && plugin.getServer().getPluginManager().getPlugin("Towny") != null) {

            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(block.getLocation());

            if (townBlock != null && townBlock.hasTown()) {
                log.fine("Let Towny handle this.");
                return true;
            }
        }

        // Grief Prevention
        if (plugin.getConfig().getBoolean("GriefPrevention-Support", true) && plugin.getGriefPrevention() != null) {
            String noBuildReason = plugin.getGriefPrevention().allowBreak(player, block, block.getLocation(), event);

            if (noBuildReason != null) {
                log.fine("Let GriefPrevention handle this.");
                return true;
            }
        }

        // WorldGuard
        if (plugin.getConfig().getBoolean("WorldGuard-Support", true)
                && plugin.getVersionManager().getWorldGuardProvider() != null) {

            if (!plugin.getVersionManager().getWorldGuardProvider().canBreak(player, block.getLocation())) {
                log.fine("Let WorldGuard handle this.");
                return true;
            }
        }

        // Residence
        if (plugin.getConfig().getBoolean("Residence-Support", true) && plugin.getResidence() != null) {
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByLoc(block.getLocation());

            if (residence != null) {
                ResidencePermissions permissions = residence.getPermissions();

                // has neither build nor destroy
                // let residence run its protection
                if (!permissions.playerHas(player, Flags.destroy, true) && !permissions.playerHas(player, Flags.build, true)) {
                    log.fine("Let Residence handle this.");
                    return true;
                }
            }
        }

        return false;
    }

    /*
     We do this our own way, because default permissions don't seem to work well with LuckPerms.
     (having a wildcard permission with default: true doesn't seem to work)

     When neither of the permissions are defined allow everything.
     Specific permission takes precedence over wildcards.
    */
    private boolean lacksPermission(Player player, String permission, String specific) {
        boolean hasAll = player.hasPermission(String.format("%s.*", permission));
        boolean allDefined = player.isPermissionSet(String.format("%s.*", permission));

        boolean hasSpecific = player.hasPermission(String.format("%s.%s", permission, specific));
        boolean specificDefined = player.isPermissionSet(String.format("%s.%s", permission, specific));

        return !((hasAll && !specificDefined) || (!allDefined && !specificDefined) || (hasSpecific && specificDefined));
    }

    private boolean hasBypass(Player player) {
        return plugin.getRegenerationManager().hasBypass(player)
                || (plugin.getConfig().getBoolean("Bypass-In-Creative", false)
                && player.getGameMode() == GameMode.CREATIVE);
    }

    private void process(RegenerationProcess process, BlockPreset preset, BlockBreakEvent event) {
        Player player = event.getPlayer();

        Block block = event.getBlock();
        BlockState state = block.getState();

        // Check block permissions
        // Mostly kept out of backwards compatibility with peoples settings and expectancies over how this works.
        if (lacksPermission(player, "blockregen.block", block.getType().toString()) && !player.isOp()) {
            Message.PERMISSION_BLOCK_ERROR.send(event.getPlayer());
            event.setCancelled(true);
            log.fine(String.format("Player doesn't have permission for block %s.", block.getType()));
            return;
        }

        // Check preset permissions
        if (lacksPermission(player, "blockregen.preset", preset.getName()) && !player.isOp()) {
            Message.PERMISSION_BLOCK_ERROR.send(event.getPlayer());
            event.setCancelled(true);
            log.fine(String.format("Player doesn't have permission for preset %s.", preset.getName()));
            return;
        }

        // Check conditions
        if (!preset.getConditions().check(player)) {
            event.setCancelled(true);
            log.fine("Player doesn't meet conditions.");
            return;
        }

        // Event API
        BlockRegenBlockBreakEvent blockRegenBlockBreakEvent = new BlockRegenBlockBreakEvent(event, preset);
        Bukkit.getServer().getPluginManager().callEvent(blockRegenBlockBreakEvent);

        if (blockRegenBlockBreakEvent.isCancelled()) {
            log.fine("BlockRegenBreakEvent got cancelled.");
            return;
        }

        final AtomicInteger vanillaExperience = new AtomicInteger(event.getExpToDrop());

        // We're dropping the items ourselves.
        if (plugin.getVersionManager().isCurrentAbove("1.8", false)) {
            event.setDropItems(false);
            log.fine("Cancelled BlockDropItemEvent");
        }

        event.setExpToDrop(0);

        List<ItemStack> vanillaDrops = new ArrayList<>(
                block.getDrops(plugin.getVersionManager().getMethods().getItemInMainHand(player)));

        // Cancels item drops below 1.8.
        if (plugin.getVersionManager().isCurrentBelow("1.8", true)) {
            block.setType(Material.AIR);
        }

        // Start regeneration
        process.start();

        Function<String, String> parser = (str) -> TextUtil.parse(str, player, block);

        // Run rewards async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<ItemStack, Boolean> drops = new HashMap<>();
            int experience = 0;

            // Items and exp
            if (preset.isNaturalBreak()) {

                for (ItemStack drop : vanillaDrops) {
                    XMaterial mat = XMaterial.matchXMaterial(drop);
                    ItemStack item = mat.parseItem();

                    if (item == null) {
                        log.severe(String.format("Material %s not supported on this version.", mat));
                        continue;
                    }

                    drops.put(item, preset.isDropNaturally());
                }

                experience += vanillaExperience.get();
            } else {
                for (ItemDrop drop : preset.getRewards().getDrops()) {
                    ItemStack itemStack = drop.toItemStack(player, parser);
                    if (itemStack == null) {
                        continue;
                    }

                    if (preset.isApplyFortune()) {
                        itemStack.setAmount(ItemUtil.applyFortune(block.getType(),
                                plugin.getVersionManager().getMethods().getItemInMainHand(player))
                                + itemStack.getAmount());
                    }

                    drops.put(itemStack, drop.isDropNaturally());

                    ExperienceDrop experienceDrop = drop.getExperienceDrop();
                    if (experienceDrop != null) {
                        experience += experienceDrop.getAmount().getInt();
                    }
                }
            }

            PresetEvent presetEvent = plugin.getEventManager().getEvent(preset.getName());

            // Event
            if (presetEvent != null && presetEvent.isEnabled()) {

                // Double drops and exp
                if (presetEvent.isDoubleDrops()) {
                    drops.keySet().forEach(drop -> drop.setAmount(drop.getAmount() * 2));
                }
                if (presetEvent.isDoubleExperience()) {
                    experience *= 2;
                }

                // Item reward
                if (plugin.getRandom().nextInt(presetEvent.getItemRarity().getInt()) == 0) {
                    ItemDrop eventDrop = presetEvent.getItem();

                    // Event item
                    if (eventDrop != null) {
                        ItemStack eventStack = eventDrop.toItemStack(player, parser);

                        if (eventStack != null) {
                            drops.put(eventStack, eventDrop.isDropNaturally());
                        }
                    }

                    // Add items from presetEvent
                    for (ItemDrop drop : presetEvent.getRewards().getDrops()) {
                        ItemStack item = drop.toItemStack(player, parser);

                        if (item != null) {
                            drops.put(item, drop.isDropNaturally());
                        }
                    }

                    presetEvent.getRewards().give(player, parser);
                }
            }

            // Drop/give all the items & experience at once
            giveItems(drops, state, player);
            giveExp(block.getLocation(), player, experience, preset.isDropNaturally());

            // Trigger Jobs Break if enabled
            if (plugin.getConfig().getBoolean("Jobs-Rewards", false) && plugin.getJobsProvider() != null) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getJobsProvider().triggerBlockBreakAction(player, block));
            }

            // Other rewards - commands, money etc.
            preset.getRewards().give(player, (str) -> TextUtil.parse(str, player, block));

            if (preset.getSound() != null) {
                preset.getSound().play(block.getLocation());
            }

            if (preset.getParticle() != null && plugin.getVersionManager().isCurrentAbove("1.8", false)) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getParticleManager().displayParticle(preset.getParticle(), block));
            }
        });
    }

    private void spawnExp(Location location, int amount) {
        if (location.getWorld() == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin,
                () -> location.getWorld().spawn(location, ExperienceOrb.class).setExperience(amount));
        log.fine(String.format("Spawning xp (%d).", amount));
    }

    private void giveExp(Location location, Player player, int amount, boolean naturally) {
        if (naturally) {
            spawnExp(location, amount);
        } else {
            player.giveExp(amount);
        }
    }

    private void giveItems(Map<ItemStack, Boolean> itemStacks, BlockState blockState, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Item> items = new ArrayList<>();

            for (Map.Entry<ItemStack, Boolean> entry : itemStacks.entrySet()) {
                ItemStack item = entry.getKey();

                if (entry.getValue()) {
                    items.add(blockState.getWorld().dropItemNaturally(blockState.getLocation(), item));
                    log.fine("Dropping item " + item.getType() + "x" + item.getAmount());
                } else {
                    player.getInventory().addItem(item);
                    log.fine("Giving item " + item.getType() + "x" + item.getAmount());
                }
            }

            plugin.getVersionManager().getMethods().handleDropItemEvent(player, blockState, items);
        });
    }
}
