package com.farao_community.farao.cse_valid.app.mapper;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.powsybl.iidm.network.Country;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EicCodesMapper {

    private final EicCodesConfiguration eicCodesConfiguration;

    public EicCodesMapper(EicCodesConfiguration eicCodesConfiguration) {
        this.eicCodesConfiguration = eicCodesConfiguration;
    }

    public String mapToEic(String countryStr) {
        Country country = Country.valueOf(countryStr);
        return Optional.ofNullable(mapToEic(country))
            .orElseThrow(() -> new CseValidInvalidDataException("Invalid country " + countryStr));
    }

    public String mapToEic(Country country) {
        switch (country) {
            case AT:
                return eicCodesConfiguration.getAustria();
            case FR:
                return eicCodesConfiguration.getFrance();
            case IT:
                return eicCodesConfiguration.getItaly();
            case SI:
                return eicCodesConfiguration.getSlovenia();
            case CH:
                return eicCodesConfiguration.getSwitzerland();
            default:
                return null;
        }
    }
}
