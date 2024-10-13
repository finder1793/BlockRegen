package nl.aurorion.blockregen.version.api;

// Parse NodeData from String input.
public interface NodeDataParser {

    // Parse input into NodeData.
    NodeData parse(String input);
}
