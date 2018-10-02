package com.rbkmoney.reporter.service;

import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.exception.FileNotFoundException;
import com.rbkmoney.reporter.exception.FileStorageException;

import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;

public interface StorageService {

    URL getFileUrl(String fileId, String bucketId, Instant expiresIn) throws FileNotFoundException, FileStorageException;

    FileMeta saveFile(Path file) throws FileStorageException;

}
