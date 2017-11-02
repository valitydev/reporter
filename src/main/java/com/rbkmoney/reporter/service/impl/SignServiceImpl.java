package com.rbkmoney.reporter.service.impl;

import com.amazonaws.util.IOUtils;
import com.rbkmoney.damsel.signer.SignerSrv;
import com.rbkmoney.reporter.exception.SignException;
import com.rbkmoney.reporter.service.SignService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SignServiceImpl implements SignService {

    private final SignerSrv.Iface signerClient;

    @Autowired
    public SignServiceImpl(SignerSrv.Iface signerClient) {
        this.signerClient = signerClient;
    }

    @Override
    public byte[] sign(InputStream inputStream) {
        try {
            return sign(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            throw new SignException("Failed to read bytes from stream");
        }
    }

    @Override
    public byte[] sign(byte[] byteArray) {
        try {
            return signerClient.sign(ByteBuffer.wrap(byteArray)).array();
        } catch (TException e) {
            throw new SignException("Failed to sign file");
        }
    }

    @Override
    public byte[] sign(Path path) {
        try {
            return sign(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new SignException(String.format("Failed to read bytes from path, path='%s'", path.toString()));
        }
    }
}
