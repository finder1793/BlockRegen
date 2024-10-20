package nl.aurorion.blockregen.version.current;

import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileContainerException;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Stairs;

import java.util.HashMap;
import java.util.Map;

@Log
@ToString
@NoArgsConstructor
@Setter
public class LatestNodeData implements NodeData {

    private BlockFace facing;

    private Stairs.Shape stairShape;

    private Axis axis;

    private BlockFace rotation;

    private Integer age;

    private String skull;

    @Override
    public boolean check(Block block) {
        BlockData data = block.getBlockData();

        log.fine(String.format("Checking against data %s", this));

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

        if (data instanceof Stairs stairs && this.stairShape != null) {
            if (stairs.getShape() != this.stairShape) {
                return false;
            }
        }

        if (data instanceof Orientable orientable && this.axis != null) {
            if (orientable.getAxis() != this.axis) {
                return false;
            }
        }

        if (data instanceof Rotatable rotatable && this.rotation != null) {
            if (rotatable.getRotation() != this.rotation) {
                return false;
            }
        }

        if (data instanceof Ageable ageable && this.age != null) {
            if (ageable.getAge() != this.age) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void load(Block block) {
        BlockData data = block.getBlockData();

        try {
            this.skull = XSkull.of(block).getProfileString();
        } catch (InvalidProfileContainerException e) {
            // not a skull
        }

        if (data instanceof Directional directional) {
            this.facing = directional.getFacing();
        }

        if (data instanceof Stairs stairs) {
            this.stairShape = stairs.getShape();
        }

        if (data instanceof Orientable orientable) {
            this.axis = orientable.getAxis();
        }

        if (data instanceof Rotatable rotatable) {
            this.rotation = rotatable.getRotation();
        }

        if (data instanceof Ageable ageable) {
            this.age = ageable.getAge();
        }

        log.fine(String.format("Loaded block data %s (%s)", block.getType(), this));
    }

    @Override
    public void place(Block block) {
        BlockData blockData = block.getBlockData();

        if (blockData instanceof Directional && this.facing != null) {
            ((Directional) blockData).setFacing(this.facing);
        }

        if (blockData instanceof Stairs && this.stairShape != null) {
            ((Stairs) blockData).setShape(this.stairShape);
        }

        if (blockData instanceof Orientable && this.axis != null) {
            ((Orientable) blockData).setAxis(this.axis);
        }

        if (blockData instanceof Rotatable && this.rotation != null) {
            ((Rotatable) blockData).setRotation(this.rotation);
        }

        if (blockData instanceof Ageable && this.age != null) {
            ((Ageable) blockData).setAge(this.age);
        }

        block.setBlockData(blockData);

        if (this.skull != null) {
            XSkull.of(block)
                    .profile(Profileable.detect(this.skull))
                    .apply();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.facing == null && this.stairShape == null && this.axis == null && this.rotation == null && this.age == null;
    }

    @Override
    public String getPrettyString() {
        Map<String, Object> entries = new HashMap<>();
        entries.put("facing", this.facing);
        entries.put("shape", this.stairShape);
        entries.put("axis", this.axis);
        entries.put("rotation", this.rotation);
        entries.put("age", this.age);
        entries.put("skull", this.skull);
        return StringUtil.serializeNodeDataEntries(entries);
    }
}
