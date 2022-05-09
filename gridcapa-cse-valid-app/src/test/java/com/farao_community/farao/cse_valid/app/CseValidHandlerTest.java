package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.MinioAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class CseValidHandlerTest {

    @Autowired
    CseValidHandler cseValidHandler;

    @MockBean
    MinioAdapter minioAdapter;

    private static CseValidRequest cseValidRequest;

    @BeforeAll
    static void setUp() {
        cseValidRequest = new CseValidRequest("id",
                ProcessType.D2CC,
                OffsetDateTime.now(),
                new CseValidFileResource("ttcAdjustment.xml", "file://ttcAdjustment.xml"),
                null,
                null,
                null);
    }

    @Test
    void simpleTestWithExistingTtcAdjustmentFile() throws Exception {
        Mockito.when(minioAdapter.getMinioObject(any())).thenReturn(Optional.ofNullable(getClass().getResourceAsStream("/TTC_Adjustment_20200813_2D4_CSE1_Simple_Import.xml")));
        cseValidHandler.handleCseValidRequest(cseValidRequest);
    }

    @Test
    void simpleTestWithNonExistingTtcAdjustmentFile() throws Exception {
        Mockito.when(minioAdapter.getMinioObject(any())).thenReturn(Optional.empty());
        cseValidHandler.handleCseValidRequest(cseValidRequest);
    }
}
