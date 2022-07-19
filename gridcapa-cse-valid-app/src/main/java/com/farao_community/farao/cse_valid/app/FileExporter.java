/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final String JSON_CRAC_FILE_NAME = "crac.json";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private static final String MINIO_SEPARATOR = "/";
    private static final String ZONE_ID = "Europe/Paris";

    private final MinioAdapter minioAdapter;

    public FileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public String saveCracInJsonFormat(Crac crac, OffsetDateTime processTargetDateTime, ProcessType processType) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CseValidInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = makeDestinationMinioPath(processTargetDateTime, processType, FileKind.ARTIFACTS) + JSON_CRAC_FILE_NAME;
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            minioAdapter.uploadArtifactForTimestamp(cracPath, is, processType.toString(), "", processTargetDateTime);
        } catch (IOException e) {
            throw new CseValidInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    public String saveNetworkInArtifact(Network network, String networkFilePath, String fileType, OffsetDateTime processTargetDateTime, ProcessType processType) {
        exportAndUploadNetwork(network, "XIIDM", GridcapaFileGroup.ARTIFACT, networkFilePath, fileType, processTargetDateTime, processType);
        return minioAdapter.generatePreSignedUrl(networkFilePath);
    }

    String exportAndUploadNetwork(Network network, String format, GridcapaFileGroup fileGroup, String filePath, String fileType, OffsetDateTime offsetDateTime, ProcessType processType) {
        try (InputStream is = getNetworkInputStream(network, format)) {
            switch (fileGroup) {
                case OUTPUT:
                    minioAdapter.uploadOutputForTimestamp(filePath, is, processType.toString(), fileType, offsetDateTime);
                    break;
                case ARTIFACT:
                    minioAdapter.uploadArtifactForTimestamp(filePath, is, processType.toString(), fileType, offsetDateTime);
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("File group %s not supported", fileGroup));
            }
        } catch (IOException e) {
            throw new CseValidInternalException("Error while trying to save network", e);
        }
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private InputStream getNetworkInputStream(Network network, String format) throws IOException {
        MemDataSource memDataSource = new MemDataSource();
        switch (format) {
            case "UCTE":
                Exporters.export("UCTE", network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", "uct");
            case "XIIDM":
                Exporters.export("XIIDM", network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", "xiidm");
            default:
                throw new UnsupportedOperationException(String.format("Network format %s not supported", format));
        }
    }

    public String saveRaoParameters(OffsetDateTime offsetDateTime, ProcessType processType) {
        RaoParameters raoParameters = RaoParameters.load();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = makeDestinationMinioPath(offsetDateTime, processType, FileKind.ARTIFACTS) + RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifactForTimestamp(raoParametersDestinationPath, bais, processType.toString(), "", offsetDateTime);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    public String saveTtcValidation(TcDocumentTypeWriter tcDocumentTypeWriter, OffsetDateTime offsetDateTime, ProcessType processType) {
        String ttcValidationDestinationPath = makeDestinationMinioPath(offsetDateTime, processType, FileKind.OUTPUTS);
        String ttcValidationFileName = getTTCValidationFilename(processType, offsetDateTime, ttcValidationDestinationPath);
        InputStream ttcValidationIs = tcDocumentTypeWriter.buildTcDocumentType();
        minioAdapter.uploadOutputForTimestamp(ttcValidationFileName, ttcValidationIs, processType.toString(), "TTC-VALIDATION", offsetDateTime);
        return minioAdapter.generatePreSignedUrl(ttcValidationDestinationPath);
    }

    public String makeDestinationMinioPath(OffsetDateTime offsetDateTime, ProcessType processType, FileKind filekind) {
        ZonedDateTime targetDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(ZONE_ID));
        return processType + MINIO_SEPARATOR
                + targetDateTime.getYear() + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getMonthValue()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getDayOfMonth()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getHour()) + "_30" + MINIO_SEPARATOR
                + filekind + MINIO_SEPARATOR;
    }

    private String getTTCValidationFilename(ProcessType processType, OffsetDateTime offsetDateTime, String ttcValidationDestinationPath) {
        String dateTime = formatDate(offsetDateTime);
        String processCode = getProcessCode(processType, offsetDateTime);
        String ttcValidationFileName = ttcValidationDestinationPath + String.format("TTC_RTEValidation_%s_%s_[v].xml", dateTime, processCode);
        return getLatestFileVersion(ttcValidationFileName);
    }

    private String formatDate(OffsetDateTime offsetDateTime) {
        return DateTimeFormatter.BASIC_ISO_DATE.format(offsetDateTime.toLocalDate());
    }

    private String getProcessCode(ProcessType processType, OffsetDateTime offsetDateTime) {
        return processType.getCode() + offsetDateTime.getDayOfWeek().getValue();
    }

    private String getLatestFileVersion(String ttcValidationFileName) {
        String fileVersionned = ttcValidationFileName.replace("[v]", "1");

        for (int versionNumber = 1; minioAdapter.fileExists(fileVersionned) && versionNumber <= 99; versionNumber++) {
            fileVersionned = ttcValidationFileName.replace("[v]", String.valueOf(versionNumber));
        }

        return fileVersionned;
    }

    public enum FileKind {
        ARTIFACTS,
        OUTPUTS
    }
}
