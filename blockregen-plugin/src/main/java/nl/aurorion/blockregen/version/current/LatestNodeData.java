package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.version.api.NodeData;
import org.bukkit.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Stairs;

@Log
@ToString
@NoArgsConstructor
@Setter
public class LatestNodeData implements NodeData {

    private BlockFace facing;

    private Stairs.Shape stairShape;

    private Axis axis;

    private BlockFace rotation;

    private int age = -1;

    private boolean farmland;

    public void copyBlockData(BlockData data) {
        if (data instanceof Directional) {
            this.facing = ((Directional) data).getFacing();
        }

        if (data instanceof Stairs) {
            this.stairShape = ((Stairs) data).getShape();
        }

        if (data instanceof Orientable) {
            this.axis = ((Orientable) data).getAxis();
        }

        if (data instanceof Rotatable) {
            this.rotation = ((Rotatable) data).getRotation();
        }

        if (data instanceof Ageable) {
            this.age = ((Ageable) data).getAge();
        }
    }

    @Override
    public boolean check(Block block) {
        BlockData data = block.getBlockData();

        log.fine(String.format("Checking against data %s", this));

        if (data instanceof Directional directional) {
            if (directional.getFacing() != this.facing)  {
                return false;
            }
        }

        if (data instanceof Stairs stairs) {
            if (stairs.getShape() != this.stairShape) {
                return false;
            }
        }

        if (data instanceof Orientable orientable) {
            if (orientable.getAxis() != this.axis) {
                return false;
            }
        }

        if (data instanceof Rotatable rotatable) {
            if (rotatable.getRotation() != this.rotation) {
                return false;
            }
        }

        if (data instanceof Ageable ageable) {
            if (ageable.getAge() != this.age) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void load(Block block) {
        BlockData data = block.getBlockData();

        this.copyBlockData(data);

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

        if (blockData instanceof Ageable && this.age != -1) {
            ((Ageable) blockData).setAge(this.age);
        }

        block.setBlockData(blockData);
    }
}
