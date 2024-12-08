package nl.aurorion.blockregen.system.preset.struct.material;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.Mechanic;
import lombok.Getter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.block.Block;

import java.util.Objects;

// Check using the Nexo API whether the destroyed block matches.
public class NexoMaterial implements TargetMaterial {

    private final BlockRegen plugin;

    @Getter
    private final String nexoId;

    public NexoMaterial(BlockRegen plugin, String nexoId) {
        this.plugin = plugin;
        this.nexoId = nexoId;
    }

    @Override
    public boolean check(BlockPreset preset, Block block) {
        Mechanic nexoBlock = NexoBlocks.getNexoBlock(block.getLocation());

        if (nexoBlock == null) {
            return false;
        }

        String blockId = nexoBlock.getItemID();
        return Objects.equals(blockId, this.nexoId);
    }

    @Override
    public void place(Block block) {
        NexoBlocks.place(this.nexoId, block.getLocation());
    }

    @Override
    public String toString() {
        return "NexoMaterial{" +
                "nexoId='" + nexoId + '\'' +
                '}';
    }
}
