package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.config.CephPostgresqlSpringBootITest;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@CephPostgresqlSpringBootITest
public class StorageServiceTest {

    @Autowired
    private StorageService storageService;

    @MockBean
    private ScheduleReports scheduleReports;

    @Test
    public void saveFileTest() throws IOException {
        Path expectedFile = Files.createTempFile("reporter_", "_expected_file");
        Path actualFile = Files.createTempFile("reporter_", "_actual_file");

        try {
            Files.write(expectedFile, "4815162342".getBytes());
            FileMeta fileMeta = storageService.saveFile(expectedFile);
            URL url = storageService.getFileUrl(fileMeta.getFileId(), fileMeta.getBucketId(),
                    LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));
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
