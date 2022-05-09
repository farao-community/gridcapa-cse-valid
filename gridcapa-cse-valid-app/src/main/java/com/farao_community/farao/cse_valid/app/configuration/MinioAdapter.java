/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.configuration;

import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class MinioAdapter {
    private static final int DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioAdapter.class);
    public static final String FORMAT_URL = "%s/%s";

    private final MinioClient client;
    private final String bucket;
    private final String basePath;

    public MinioAdapter(MinioConfiguration minioConfiguration, MinioClient minioClient) {
        this.client = minioClient;
        this.bucket = minioConfiguration.getBucket();
        this.basePath = minioConfiguration.getBasePath();
    }

    public String getBasePath() {
        return basePath;
    }

    public void uploadFile(String filePath, InputStream sourceInputStream) {
        String fullPath = String.format(FORMAT_URL, basePath, filePath);
        try {
            createBucketIfDoesNotExist(bucket);
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(fullPath).stream(sourceInputStream, -1, 50000000).build());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new CseValidInternalException(String.format("Exception occurred while uploading file: %s, to minio server", filePath));
        }
    }

    public Optional<InputStream> getMinioObject(String filename) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        return Optional.of(client.getObject(GetObjectArgs
                .builder()
                .bucket(bucket)
                .object(filename)
                .build()));
    }

    private void createBucketIfDoesNotExist(String bucket) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                LOGGER.info("Create Minio bucket '{}' that did not exist already", bucket);
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new CseValidInternalException(String.format("Cannot create bucket '%s'", bucket), e);
        }
    }
}
