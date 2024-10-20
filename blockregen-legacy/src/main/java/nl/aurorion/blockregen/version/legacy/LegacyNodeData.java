package nl.aurorion.blockregen.version.legacy;

import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileContainerException;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.CropState;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.*;

import java.util.HashMap;
import java.util.Map;

@Log
@ToString
@Setter
@NoArgsConstructor
public class LegacyNodeData implements NodeData {

    private BlockFace facing;

    // Trees
    private TreeSpecies treeSpecies;

    // Stairs
    private Boolean inverted;

    private CropState cropState;

    private String skull;

    @Override
    public boolean check(Block block) {
        MaterialData data = block.getState().getData();

        if (this.skull != null) {
            try {
                String profileString = XSkull.of(block).getProfileString();

                if (profileString != null && !profileString.equals(this.skull)) {
                    return false;
                }
            } catch (InvalidProfileContainerException e) {
                // not a skull
                return false;
            }
        }

        if (data instanceof Directional directional && this.facing != null) {
            if (directional.getFacing() != this.facing) {
                return false;
            }
        }

        if (data instanceof Tree tree && this.facing != null) {
            if (tree.getDirection() != this.facing) {
                return false;
            }
        }

        if (data instanceof Wood wood && this.treeSpecies != null) {
            if (wood.getSpecies() != this.treeSpecies) {
                return false;
            }
        }

        if (data instanceof Stairs stairs && this.inverted != null) {
            if (stairs.isInverted() != this.inverted) {
                return false;
            }
        }

        if (data instanceof Crops crops && this.cropState != null) {
            if (crops.getState() != this.cropState) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void load(Block block) {
        MaterialData data = block.getState().getData();

        try {
            this.skull = XSkull.of(block).getProfileString();
        } catch (InvalidProfileContainerException e) {
            // not a skull
        }

        if (data instanceof Directional directional) {
            this.facing = directional.getFacing();
        }

        if (data instanceof Tree tree) {
            this.facing = tree.getDirection();
        }

        if (data instanceof Stairs stairs) {
            this.inverted = stairs.isInverted();
        }

        if (data instanceof Crops crops) {
            this.cropState = crops.getState();
        }

        if (data instanceof Wood wood) {
            this.treeSpecies = wood.getSpecies();
        }

        log.fine(String.format("Loaded block data %s (%s)", block.getType(), this));
    }

    @Override
    public void place(Block block) {
        BlockState state = block.getState();
        MaterialData data = state.getData();

        if (data instanceof Directional directional && this.facing != null) {
            directional.setFacingDirection(this.facing);
        }

        if (data instanceof Tree tree && this.facing != null) {
            tree.setDirection(this.facing);
        }

        if (data instanceof Wood wood && this.treeSpecies != null) {
            wood.setSpecies(this.treeSpecies);
        }

        if (data instanceof Stairs && this.inverted != null && this.inverted) {
            ((Stairs) data).setInverted(true);
        }

        if (data instanceof Crops crops && this.cropState != null) {
            crops.setState(cropState);
        }

        if (this.skull != null) {
            XSkull.of(block)
                    .profile(Profileable.detect(this.skull))
                    .apply();
        }

        state.setData(data);
    }

    @Override
    public boolean isEmpty() {
        return this.facing == null && this.treeSpecies == null && this.inverted == null && this.cropState == null;
    }

    @Override
    public String getPrettyString() {
        Map<String, Object> entries = new HashMap<>();
        entries.put("facing", this.facing);
        entries.put("species", this.treeSpecies);
        entries.put("inverted", this.inverted);
        entries.put("age", this.cropState == null ? null : this.cropState.ordinal());
        return StringUtil.serializeNodeDataEntries(entries);
    }
}
