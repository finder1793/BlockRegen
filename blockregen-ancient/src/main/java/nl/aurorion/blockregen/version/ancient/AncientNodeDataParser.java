package nl.aurorion.blockregen.version.ancient;

import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.CropState;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;

public class AncientNodeDataParser implements NodeDataParser {

    private final NodeDataDeserializer<AncientNodeData> nodeDataDeserializer = new NodeDataDeserializer<AncientNodeData>()
            .property("age", (nodeData, value) -> {
                int age = Integer.parseInt(value);
                CropState state = CropState.values()[age];
                nodeData.setCropState(state);
            })
            .property("facing", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                BlockFace facing = BlockFace.values()[id];
                nodeData.setFacing(facing);
            })
            .property("species", (nodeData, value) -> {
                int id = Integer.parseInt(value);
                TreeSpecies species = TreeSpecies.values()[id];
                nodeData.setTreeSpecies(species);
            })
            .property("inverted", (nodeData, value) -> {
                boolean inverted = Boolean.parseBoolean(value);
                nodeData.setInverted(inverted);
            })
            .property("skull", AncientNodeData::setSkull);

    @Override
    public NodeData parse(String input) {
        AncientNodeData nodeData = new AncientNodeData();
        this.nodeDataDeserializer.deserialize(nodeData, input);
        return nodeData;
    }
}
