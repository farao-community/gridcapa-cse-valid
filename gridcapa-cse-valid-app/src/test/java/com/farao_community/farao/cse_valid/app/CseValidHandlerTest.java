package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
    void simpleTestWithExistingTtcAdjustmentFile() {
        when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "basepath", "url", "accesskey", "secretkey"));
        when(minioAdapter.getFile(any())).thenReturn(getClass().getResourceAsStream("/TTC_Adjustment_20200813_2D4_CSE1_no_calcul.xml"));
        cseValidHandler.handleCseValidRequest(cseValidRequest);
        assertNotNull(cseValidHandler.getTcDocumentType());
        assertEquals(1, cseValidHandler.getTcDocumentType().getAdjustmentResults().get(0).getTimestamp().size());
    }

    @Test
    void simpleTestWithNonExistingTtcAdjustmentFile() {
        when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "basepath", "url", "accesskey", "secretkey"));
        when(minioAdapter.getFile(any())).thenReturn(getClass().getResourceAsStream("/doesNotExist.xml"));
        Assertions.assertThrows(CseValidInvalidDataException.class, () -> cseValidHandler.handleCseValidRequest(cseValidRequest));
    }
}
