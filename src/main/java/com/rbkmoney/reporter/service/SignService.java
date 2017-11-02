package com.rbkmoney.reporter.service;

import java.io.InputStream;
import java.nio.file.Path;

public interface SignService {

    byte[] sign(InputStream inputStream);

    byte[] sign(byte[] byteArray);

    byte[] sign(Path path);

}
