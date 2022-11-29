package com.farao_community.farao.gridcapa_cse_valid.starter;

import com.farao_community.farao.cse_valid.api.JsonApiConverter;
import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CseValidResponseConversionHelperTest {

    @Mock
    private JsonApiConverter jsonApiConverter;

    public static ResourceParseException getResourceParseException() {
        Error error = new Error();
        error.setDetail("haha");
        Errors errors = new Errors();
        errors.setErrors(List.of(error));
        return new ResourceParseException(errors);
    }

    public static CseValidResponse getCseValidResponse() {
        String id = UUID.randomUUID().toString();
        return new CseValidResponse(id, "", null, null);
    }

    @Test
    void convertCseValidResponseShouldReturnCseValidResponse() {
        Message message = mock(Message.class);
        CseValidResponse cseValidResponse = getCseValidResponse();
        when(jsonApiConverter.fromJsonMessage(any(), any())).thenReturn(cseValidResponse);
        CseValidResponse result = CseValidResponseConversionHelper.convertCseValidResponse(message, jsonApiConverter);
        assertEquals(cseValidResponse, result);
    }

    @Test
    void convertCseValidResponseShouldThrowCseValidInvalidDataExceptionCauseByResourceParseException() {
        Message message = mock(Message.class);
        when(jsonApiConverter.fromJsonMessage(any(), any())).thenThrow(getResourceParseException());
        assertThrows(CseValidInvalidDataException.class, () -> CseValidResponseConversionHelper.convertCseValidResponse(message, jsonApiConverter));
    }

    @Test
    void convertCseValidResponseShouldThrowCseValidInvalidDataExceptionCauseByException() {
        Message message = mock(Message.class);
        when(jsonApiConverter.fromJsonMessage(any(), any())).thenThrow(RuntimeException.class);
        assertThrows(CseValidInvalidDataException.class, () -> CseValidResponseConversionHelper.convertCseValidResponse(message, jsonApiConverter));
    }
}
