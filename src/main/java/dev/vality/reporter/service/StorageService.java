package dev.vality.reporter.service;

import dev.vality.reporter.domain.tables.pojos.FileMeta;
import dev.vality.reporter.exception.FileNotFoundException;
import dev.vality.reporter.exception.FileStorageException;

import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;

public interface StorageService {

    URL getFileUrl(String fileId, String bucketId, Instant expiresIn)
            throws FileNotFoundException, FileStorageException;

    FileMeta saveFile(Path file) throws FileStorageException;

}
