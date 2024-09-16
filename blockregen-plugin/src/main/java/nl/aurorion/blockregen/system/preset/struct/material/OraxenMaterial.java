package nl.aurorion.blockregen.system.preset.struct.material;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import lombok.Getter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.block.Block;

import java.util.Objects;

// Check using the Oraxen API whether the destroyed block matches.
public class OraxenMaterial implements TargetMaterial {

    private final BlockRegen plugin;

    @Getter
    private final String oraxenId;

    public OraxenMaterial(BlockRegen plugin, String oraxenId) {
        this.plugin = plugin;
        this.oraxenId = oraxenId;
    }

    @Override
    public boolean check(BlockPreset preset, Block block) {
        Mechanic oraxenBlock = OraxenBlocks.getOraxenBlock(block.getLocation());

        if (oraxenBlock == null) {
            return false;
        }

        String blockId = oraxenBlock.getItemID();
        return Objects.equals(blockId, this.oraxenId);
    }

    @Override
    public void place(Block block) {
        OraxenBlocks.place(this.oraxenId, block.getLocation());
    }

    @Override
    public String toString() {
        return "OraxenMaterial{" +
                "oraxenId='" + oraxenId + '\'' +
                '}';
    }
}
