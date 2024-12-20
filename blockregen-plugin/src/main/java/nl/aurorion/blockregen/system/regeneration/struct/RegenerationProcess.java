package nl.aurorion.blockregen.system.regeneration.struct;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.api.BlockRegenBlockRegenerationEvent;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import nl.aurorion.blockregen.util.BlockUtil;
import nl.aurorion.blockregen.util.LocationUtil;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.UUID;

@Log
@Data
public class RegenerationProcess {

    private final UUID id = UUID.randomUUID();

    private SimpleLocation location;

    private transient Block block;

    @Getter
    private XMaterial originalMaterial;

    @Getter
    private NodeData originalData;

    @Getter
    private String regionName;
    @Getter
    private String worldName;

    private String presetName;

    @Getter
    private transient BlockPreset preset;

    /*
     * Holds the system time when the block should regenerate.
     * -- is set after #start()
     */
    @Getter
    private transient long regenerationTime;

    private transient TargetMaterial replaceMaterial;

    @Getter
    private long timeLeft = -1;

    @Setter
    private transient TargetMaterial regenerateInto;

    private transient BukkitTask task;

    public RegenerationProcess(Block block, NodeData originalData, BlockPreset preset) {
        this.block = block;
        this.location = new SimpleLocation(block);

        this.preset = preset;
        this.presetName = preset.getName();

        this.worldName = block.getWorld().getName();

        this.originalData = originalData;
        this.originalMaterial = XMaterial.matchXMaterial(block.getType());
        this.regenerateInto = preset.getRegenMaterial().get();
        this.replaceMaterial = preset.getReplaceMaterial().get();
    }

    // Return true if the process started, false otherwise.
    public boolean start() {

        // Ensure to stop and null anything that ran before.
        stop();

        BlockRegen plugin = BlockRegen.getInstance();

        // Register that the process is actually running now
        // #start() can be called even on a process already in cache due to #contains() checks (which use #equals()) in RegenerationManager.
        // Two processes with the same location cannot be added.
        plugin.getRegenerationManager().registerProcess(this);

        // If timeLeft is -1, generate a new one from preset regen delay.
        if (timeLeft == -1) {
            int regenDelay = preset.getDelay().getInt();
            this.timeLeft = regenDelay * 1000L;
        }

        this.regenerationTime = System.currentTimeMillis() + timeLeft;

        // No need to start a task when it's time to regenerate already.
        if (timeLeft == 0 || regenerationTime <= System.currentTimeMillis()) {
            Bukkit.getScheduler().runTask(plugin, this::regenerate);
            log.fine(() -> "Regenerated the process upon start.");
            return false;
        }

//        Bukkit.getScheduler().runTask(plugin, this::replaceBlock);

        replaceBlock();

        startTask();
        return true;
    }

    private void startTask() {
        // Start the task
        this.task = Bukkit.getScheduler().runTaskLater(BlockRegen.getInstance(), this::regenerate, timeLeft / 50);
        log.fine(() -> String.format("Regenerate %s in %ds", this, timeLeft / 1000));
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            this.task = null;
        }
    }

    /**
     * Regenerate the process and block.
     * <p>
     * Calls BlockRegenBlockRegenerationEvent.
     */
    public void regenerate() {
        log.fine(() -> "Regenerating " + this + "...");

        // Cancel the task if running.
        if (task != null) {
            task.cancel();
        }

        BlockRegen plugin = BlockRegen.getInstance();

        // If this block requires a block under it, wait for it to be there,
        // only if there's a running process at the block directly under.
        //
        // Otherwise, throw this process away.

        TargetMaterial regenerateInto = getRegenerateInto();

        if (regenerateInto.requiresSolidGround() && preset.isCheckSolidGround()) {
            Block below = this.block.getRelative(BlockFace.DOWN);
            XMaterial belowType = plugin.getVersionManager().getMethods().getType(below);
            RegenerationProcess processBelow = plugin.getRegenerationManager().getProcess(below);

            // Sugarcane on sugarcane (aka not solid, still can be placed)
            // + kelp on kelp
            if (!below.getType().isSolid() && belowType != XMaterial.SUGAR_CANE && !BlockUtil.isKelp(belowType) && !BlockUtil.isSeagrass(belowType)) {
                if (processBelow != null) {
                    long delay = processBelow.getRegenerationTime() >= this.getRegenerationTime() ? processBelow.getRegenerationTime() - this.getRegenerationTime() + 100 : 1000;

                    // Regenerate with the block below.
                    this.timeLeft = delay;
                    this.regenerationTime = System.currentTimeMillis() + timeLeft;

                    log.fine(() -> "Delaying " + this + " to wait for " + processBelow + " delay: " + delay);

                    startTask();
                } else {
                    // no block under, no regeneration,... no hope
                    log.fine(() -> "No block under " + this + ", no point regenerating.");
                    plugin.getRegenerationManager().removeProcess(this);
                }
                return;
            }
        }

        // Call the event
        BlockRegenBlockRegenerationEvent blockRegenBlockRegenEvent = new BlockRegenBlockRegenerationEvent(this);
        Bukkit.getPluginManager().callEvent(blockRegenBlockRegenEvent);

        plugin.getRegenerationManager().removeProcess(this);

        if (blockRegenBlockRegenEvent.isCancelled()) {
            return;
        }

        regenerateBlock();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (preset.getRegenerationParticle() != null) {
                plugin.getParticleManager().displayParticle(preset.getRegenerationParticle(), block);
            }
        });

        // Null the task
        this.task = null;
    }

    /**
     * Simply regenerate the block.
     */
    public void regenerateBlock() {
        // Set type
        TargetMaterial regenerateInto = getRegenerateInto();
        if (regenerateInto == null) {
            log.fine(() -> "Found no regeneration material for " + this);
            return;
        }

        // -- Regenerate farmland under crops
        if (regenerateInto.requiresFarmland()) {
            Block under = block.getRelative(BlockFace.DOWN);
            XMaterial underType = BlockRegen.getInstance().getVersionManager().getMethods().getType(under);

            if (underType != XMaterial.FARMLAND) {
                under.setType(Objects.requireNonNull(XMaterial.FARMLAND.parseMaterial()));
            }
        }

        regenerateInto.place(block);
        originalData.place(block); // Apply original data
        regenerateInto.applyData(block); // Override with configured data if any
        log.fine(() -> "Regenerated " + this);
    }

    // Revert process to original material.
    public void revert() {
        stop();

        BlockRegen plugin = BlockRegen.getInstance();
        plugin.getRegenerationManager().removeProcess(this);

        revertBlock();
    }

    // Revert block to original state
    public void revertBlock() {
        Material material = originalMaterial.parseMaterial();

        if (material != null) {
            // -- Place farmland under crops
            if (XBlock.isCrop(originalMaterial)) {
                Block under = block.getRelative(BlockFace.DOWN);
                XMaterial underType = BlockRegen.getInstance().getVersionManager().getMethods().getType(under);
                if (underType != XMaterial.FARMLAND) {
                    under.setType(Objects.requireNonNull(XMaterial.FARMLAND.parseMaterial()));
                }
            }

            block.setType(material);
            originalData.place(this.block);
            log.fine(() -> String.format("Reverted block for %s", this));
        }
    }

    // Has to be synchronized to run on the next tick. Otherwise, the block does not get replaced.
    public void replaceBlock() {
        TargetMaterial replaceMaterial = getReplaceMaterial();

        if (replaceMaterial == null) {
            return;
        }

        // -- Place farmland under crops
        if (replaceMaterial.requiresFarmland()) {
            Block under = block.getRelative(BlockFace.DOWN);
            XMaterial underType = BlockRegen.getInstance().getVersionManager().getMethods().getType(under);
            if (underType != XMaterial.FARMLAND) {
                under.setType(Objects.requireNonNull(XMaterial.FARMLAND.parseMaterial()));
            }
        }

        this.replaceMaterial.place(block);
        // this.originalData.place(block); // Apply original data
        // replaceMaterial.applyData(block); // Apply configured data if any

        // Otherwise skull textures wouldn't update.
        // Bukkit.getScheduler().runTaskLater(BlockRegen.getInstance(), () -> block.getState().update(true), 1L);
        log.fine(() -> "Replaced block for " + this);
    }

    public TargetMaterial getRegenerateInto() {
        // Make sure we always get something.
        if (regenerateInto == null) {
            this.regenerateInto = preset.getRegenMaterial().get();
        }
        return regenerateInto;
    }

    public TargetMaterial getReplaceMaterial() {
        // Make sure we always get something.
        if (replaceMaterial == null) {
            this.replaceMaterial = preset.getReplaceMaterial().get();
        }
        return replaceMaterial;
    }

    // Convert stored Location pointer to the Block at the location.
    public boolean convertLocation() {

        if (location == null) {
            log.severe("Could not load location for process " + this);
            return false;
        }

        Block block = this.location.toBlock();

        if (block == null) {
            log.severe("Could not load location for process " + this + ", world is invalid or not loaded.");
            return false;
        }

        // Prevent async chunk load.
        Bukkit.getScheduler().runTask(BlockRegen.getInstance(), () -> this.block = block);
        return true;
    }

    public boolean convertPreset() {
        BlockRegen plugin = BlockRegen.getInstance();

        BlockPreset preset = plugin.getPresetManager().getPreset(presetName);

        if (preset == null) {
            log.severe("Could not load process " + this + ", it's preset '" + presetName + "' is invalid.");
            return false;
        }

        this.preset = preset;
        return true;
    }

    public void updateTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
        if (timeLeft > 0) {
            start();
        } else {
            regenerate();
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    public Block getBlock() {
        if (this.block == null) {
            convertLocation();
        }
        return block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegenerationProcess process = (RegenerationProcess) o;
        return process.getId().equals(this.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("{id=%s; task=%s; presetName=%s; worldName=%s; regionName=%s; block=%s; originalData=%s; originalMaterial=%s; regenerateInto=%s; replaceMaterial=%s; timeLeft=%d; regenerationTime=%d}",
                id,
                task == null ? "null" : task.getTaskId(),
                presetName,
                worldName,
                regionName,
                block == null ? "null" : LocationUtil.locationToString(block.getLocation()),
                originalData,
                originalMaterial,
                getRegenerateInto(),
                getReplaceMaterial(),
                timeLeft,
                regenerationTime);
    }
}