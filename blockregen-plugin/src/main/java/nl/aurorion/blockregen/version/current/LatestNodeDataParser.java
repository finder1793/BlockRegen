package nl.aurorion.blockregen.version.current;

import lombok.NoArgsConstructor;
import nl.aurorion.blockregen.version.api.NodeData;
import nl.aurorion.blockregen.version.api.NodeDataParser;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

@NoArgsConstructor
public class LatestNodeDataParser implements NodeDataParser {

    @Override
    public NodeData parse(String input) throws IllegalArgumentException {
        BlockData data;
        try {
            data = Bukkit.createBlockData(input);
        } catch (IllegalArgumentException e) {
            // Rethrow with more information
            throw new IllegalArgumentException(String.format("""
                    Could not parse block data from %s. \
                    Common causes include wrong format or invalid data for the material. \
                    (ex.: age is too high for the crop, the material does not support this data). \
                    Use /br check if you're not sure what's wrong.""", input));
        }

        LatestNodeData nodeData = new LatestNodeData();
        nodeData.copyBlockData(data);
        return nodeData;
    }
}
