package nl.aurorion.blockregen;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair<F, S> {
    private final F first;
    private final S second;
}
