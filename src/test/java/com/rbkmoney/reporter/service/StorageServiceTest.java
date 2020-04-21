package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.config.AbstractStorageServiceConfig;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StorageServiceTest extends AbstractStorageServiceConfig {

    @Autowired
    private StorageService storageService;

    @Test
    @Ignore("disable test with ceph usage, if needed check it locally")
    public void saveFileTest() throws IOException {
        Path expectedFile = Files.createTempFile("reporter_", "_expected_file");
        Path actualFile = Files.createTempFile("reporter_", "_actual_file");

        try {
            Files.write(expectedFile, "4815162342".getBytes());
            FileMeta fileMeta = storageService.saveFile(expectedFile);
            URL url = storageService.getFileUrl(fileMeta.getFileId(), fileMeta.getBucketId(), LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));
            assertNotNull(url);

            try (InputStream in = url.openStream()) {
                Files.copy(in, actualFile, StandardCopyOption.REPLACE_EXISTING);
            }
            assertEquals(Files.readAllLines(expectedFile), Files.readAllLines(actualFile));
            assertEquals(fileMeta.getMd5(), DigestUtils.md5Hex(Files.newInputStream(actualFile)));
            assertEquals(fileMeta.getSha256(), DigestUtils.sha256Hex(Files.newInputStream(actualFile)));
        } finally {
            Files.deleteIfExists(expectedFile);
            Files.deleteIfExists(actualFile);
        }
    }
}
