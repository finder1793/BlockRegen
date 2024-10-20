package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;

@NoArgsConstructor
public class LatestNodeDataParser implements NodeDataParser {

    private final NodeDataDeserializer<LatestNodeData> nodeDataDeserializer = new NodeDataDeserializer<LatestNodeData>()
            .property("age", (nodeData, value) -> {
                int age = Integer.parseInt(value);
                nodeData.setAge(age);
            })
            .property("facing", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                BlockFace facing = BlockFace.values()[id];
                nodeData.setFacing(facing);
            })
            .property("rotation", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                BlockFace facing = BlockFace.values()[id];
                nodeData.setRotation(facing);
            })
            .property("axis", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                Axis axis = Axis.values()[id];
                nodeData.setAxis(axis);
            })
            .property("stairShape", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                Stairs.Shape species = Stairs.Shape.values()[id];
                nodeData.setStairShape(species);
            })
            .property("skull", (LatestNodeData::setSkull));

    @Override
    public NodeData parse(String input) throws IllegalArgumentException {
        LatestNodeData nodeData = new LatestNodeData();
        nodeDataDeserializer.deserialize(nodeData, input);
        return nodeData;
    }
}
