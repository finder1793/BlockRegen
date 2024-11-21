package nl.aurorion.blockregen.version.legacy;

import lombok.extern.java.Log;
import nl.aurorion.blockregen.version.NodeDataDeserializer;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.CropState;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;

@Log
public class LegacyNodeDataParser implements NodeDataParser {

    private final NodeDataDeserializer<LegacyNodeData> nodeDataDeserializer = new NodeDataDeserializer<LegacyNodeData>()
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
            .property("skull", (LegacyNodeData::setSkull));

    @Override
    public NodeData parse(String input) {
        LegacyNodeData nodeData = new LegacyNodeData();
        this.nodeDataDeserializer.deserialize(nodeData, input);
        return nodeData;
    }
}
