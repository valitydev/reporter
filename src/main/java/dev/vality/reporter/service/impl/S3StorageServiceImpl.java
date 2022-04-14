package dev.vality.reporter.service.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import dev.vality.reporter.domain.tables.pojos.FileMeta;
import dev.vality.reporter.exception.FileNotFoundException;
import dev.vality.reporter.exception.FileStorageException;
import dev.vality.reporter.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageServiceImpl implements StorageService {

    private final TransferManager transferManager;
    private final AmazonS3 storageClient;
    private final String bucketName;

    @Autowired
    public S3StorageServiceImpl(TransferManager transferManager,
                                @Value("${storage.bucketName}") String bucketName) {
        this.transferManager = transferManager;
        this.storageClient = transferManager.getAmazonS3Client();
        this.bucketName = bucketName;
    }

    @PostConstruct
    public void init() {
        if (!storageClient.doesBucketExistV2(bucketName)) {
            log.info("Create bucket in file storage, bucketId='{}'", bucketName);
            storageClient.createBucket(bucketName);
        }
    }

    @Override
    public URL getFileUrl(String fileId, String bucketId, Instant expiresIn)
            throws FileStorageException, FileNotFoundException {
        try {
            log.info("Trying to generate presigned url, fileId='{}', bucketId='{}', expiresIn='{}'",
                    fileId, bucketId, expiresIn);
            URL url = storageClient.generatePresignedUrl(bucketId, fileId, Date.from(expiresIn));
            if (Objects.isNull(url)) {
                throw new FileNotFoundException(
                        String.format("Presigned url is null, fileId='%s', bucketId='%s'", fileId, bucketId));
            }
            log.info("Presigned url have been successfully generated, url='{}', " +
                            "fileId='{}', bucketId='{}', expiresIn='{}'",
                    url, fileId, bucketId, expiresIn);
            return url;
        } catch (AmazonClientException ex) {
            throw new FileStorageException(
                    String.format("Failed to generate presigned url, fileId='%s', bucketId='%s', expiresIn='%s'",
                            fileId, bucketId, expiresIn), ex);
        }
    }

    @Override
    public FileMeta saveFile(Path file) throws FileStorageException {
        String filename = file.getFileName().toString();
        log.info("Trying to upload file to storage, filename='{}', bucketId='{}'", filename, bucketName);

        try {
            String fileId;
            do {
                fileId = UUID.randomUUID().toString();
            } while (storageClient.doesObjectExist(bucketName, fileId));

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileId, file.toFile());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentDisposition("attachment;filename=" + filename);
            putObjectRequest.setMetadata(objectMetadata);
            Upload upload = transferManager.upload(putObjectRequest);
            try {
                upload.waitForUploadResult();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            FileMeta fileMeta = createFileMeta(
                    fileId,
                    bucketName,
                    filename,
                    DigestUtils.md5Hex(Files.newInputStream(file)),
                    DigestUtils.sha256Hex(Files.newInputStream(file))
            );
            log.info("File have been successfully uploaded, fileId='{}', " +
                            "bucketId='{}', filename='{}', md5='{}', sha256='{}'",
                    fileMeta.getFileId(), fileMeta.getBucketId(), fileMeta.getFilename(), fileMeta.getMd5(),
                    fileMeta.getSha256());

            return fileMeta;

        } catch (IOException | AmazonClientException ex) {
            throw new FileStorageException(
                    String.format("Failed to upload file to storage, filename='%s', bucketId='%s'", filename,
                            bucketName), ex);
        }
    }

    @PreDestroy
    public void terminate() {
        transferManager.shutdownNow(true);
    }

    private FileMeta createFileMeta(String fileId, String bucketId, String filename, String md5, String sha256) {
        FileMeta fileMeta = new FileMeta();
        fileMeta.setFileId(fileId);
        fileMeta.setBucketId(bucketId);
        fileMeta.setFilename(filename);
        fileMeta.setMd5(md5);
        fileMeta.setSha256(sha256);
        return fileMeta;
    }
}
