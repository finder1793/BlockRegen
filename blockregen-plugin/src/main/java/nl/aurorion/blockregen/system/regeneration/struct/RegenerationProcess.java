package nl.aurorion.blockregen.system.regeneration.struct;

import com.cryptomorin.xseries.XMaterial;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.ConsoleOutput;
import nl.aurorion.blockregen.Utils;
import nl.aurorion.blockregen.api.BlockRegenBlockRegenerationEvent;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

@Data
public class RegenerationProcess implements Runnable {

    private SimpleLocation location;

    private transient Block block;

    @Getter
    private XMaterial originalMaterial;

    @Getter
    private String regionName;
    @Getter
    private String worldName;

    private String presetName;

    @Getter
    private transient BlockPreset preset;

    /**
     * Holds the system time when the block should regenerate.
     * -- is set after #start()
     */
    @Getter
    private transient long regenerationTime;

    private transient Material replaceMaterial;

    @Getter
    private long timeLeft = -1;

    @Setter
    private transient Material regenerateInto;

    private transient BukkitTask task;

    public RegenerationProcess(Block block, BlockPreset preset) {
        this.block = block;
        this.preset = preset;

        this.presetName = preset.getName();
        this.originalMaterial = XMaterial.matchXMaterial(block.getType());
        this.location = new SimpleLocation(block.getLocation());

        getRegenerateInto();
        getReplaceMaterial();
    }

    public Material getRegenerateInto() {
        if (this.regenerateInto == null)
            this.regenerateInto = preset.getRegenMaterial().get().parseMaterial();
        return this.regenerateInto;
    }

    public Material getReplaceMaterial() {
        if (this.replaceMaterial == null)
            this.replaceMaterial = preset.getReplaceMaterial().get().parseMaterial();
        return this.replaceMaterial;
    }

    public boolean start() {

        BlockRegen plugin = BlockRegen.getInstance();

        // Register that the process is actually running now
        plugin.getRegenerationManager().registerProcess(this);

        // If timeLeft is -1, generate a new one from preset regen delay.

        ConsoleOutput.getInstance().debug("Time left: " + this.timeLeft / 1000 + "s");

        if (this.timeLeft == -1) {
            int regenDelay = Math.max(1, preset.getDelay().getInt());
            this.timeLeft = regenDelay * 1000;
        }

        this.regenerationTime = System.currentTimeMillis() + timeLeft;

        if (this.regenerationTime <= System.currentTimeMillis()) {
            regenerate();
            ConsoleOutput.getInstance().debug("Regenerated the process already.");
            return false;
        }

        // Replace the block

        if (getReplaceMaterial() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                block.setType(getReplaceMaterial());
                ConsoleOutput.getInstance().debug("Replaced block with " + replaceMaterial.toString());
            });
        }

        // Start the regeneration task

        if (task != null)
            task.cancel();

        task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this, timeLeft / 50);
        ConsoleOutput.getInstance().debug("Started regeneration...");
        ConsoleOutput.getInstance().debug("Regenerate in " + this.timeLeft / 1000 + "s");
        return true;
    }

    @Override
    public void run() {
        regenerate();
    }

    /**
     * Regenerate the block.
     */
    public void regenerate() {

        stop();

        BlockRegen plugin = BlockRegen.getInstance();

        Bukkit.getScheduler().runTask(plugin, this::regenerateBlock);

        // Particle
        if (preset.getRegenerationParticle() != null)
            plugin.getParticleManager().displayParticle(preset.getRegenerationParticle(), block);
    }

    /**
     * Simply regenerate the block. This method is unsafe to execute from async context.
     */
    public void regenerateBlock() {
        // Call the event
        BlockRegenBlockRegenerationEvent blockRegenBlockRegenEvent = new BlockRegenBlockRegenerationEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(blockRegenBlockRegenEvent);

        BlockRegen.getInstance().getRegenerationManager().removeProcess(this);

        if (blockRegenBlockRegenEvent.isCancelled())
            return;

        // Set type
        if (getRegenerateInto() != null) {
            block.setType(getRegenerateInto());
            ConsoleOutput.getInstance().debug("Regenerated block " + originalMaterial + " into " + getRegenerateInto());
        }
    }

    /**
     * Revert process to original material.
     */
    public void revert() {

        stop();

        BlockRegen plugin = BlockRegen.getInstance();

        plugin.getRegenerationManager().removeProcess(this);

        Bukkit.getScheduler().runTask(plugin, this::revertBlock);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void revertBlock() {
        // Set the block
        Material material = originalMaterial.parseMaterial();
        if (material != null) {
            block.setType(material);
            ConsoleOutput.getInstance().debug("Placed back block " + originalMaterial);
        }
    }

    public void updateTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
        if (timeLeft > 0)
            start();
        else run();
    }

    /**
     * Convert stored Location pointer to the Block at the location.
     */
    public boolean convertLocation() {

        if (location == null) {
            ConsoleOutput.getInstance().err("Could not load location for process " + toString());
            return false;
        }

        Location location = this.location.toLocation();

        if (location == null) {
            ConsoleOutput.getInstance().err("Could not load location for process " + toString() + ", world is invalid or not loaded.");
            return false;
        }

        this.block = location.getBlock();
        return true;
    }

    public boolean convertPreset() {
        BlockRegen plugin = BlockRegen.getInstance();

        BlockPreset preset = plugin.getPresetManager().getPreset(presetName).orElse(null);

        if (preset == null) {
            plugin.getConsoleOutput().err("Could not load process " + toString() + ", it's preset '" + presetName + "' is invalid.");
            revert();
            return false;
        }

        this.preset = preset;
        return true;
    }

    public boolean isRunning() {
        return task != null;
    }

    public Block getBlock() {
        if (this.block == null)
            convertLocation();
        return block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegenerationProcess process = (RegenerationProcess) o;
        return location.equals(process.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "id: " + (task != null ? task.getTaskId() : "NaN") + "=" + presetName + " : " + (block != null ? Utils.locationToString(block.getLocation()) : location == null ? "" : location.toString()) + " - oM:" + originalMaterial.toString() + ", tL: " + timeLeft + " rT: " + regenerationTime;
    }
}