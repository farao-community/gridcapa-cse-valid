package com.farao_community.farao.cse_valid.app.net_position;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import groovy.transform.ToString;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Collections;
import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Getter
@ToString
@SuperBuilder
@Jacksonized
public class AreaReport {
    @NonNull
    private final String id;
    private final double netPosition;
    @NonNull
    private final Map<String, Double> bordersExchanges;

    @JsonCreator
    public AreaReport(@JsonProperty(required = true) String id,
                      @JsonProperty(required = true) double netPosition,
                      @JsonProperty(required = true) Map<String, Double> bordersExchanges) {
        this.id = id;
        this.netPosition = netPosition;
        this.bordersExchanges = Collections.unmodifiableMap(bordersExchanges);
    }
}
