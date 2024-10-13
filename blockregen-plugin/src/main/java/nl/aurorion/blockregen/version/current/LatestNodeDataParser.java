package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

@NoArgsConstructor
public class LatestNodeDataParser implements NodeDataParser {

    @Override
    public NodeData parse(String input) {
        BlockData data = Bukkit.createBlockData(input);
        LatestNodeData nodeData = new LatestNodeData();
        nodeData.copyBlockData(data);
        return nodeData;
    }
}
