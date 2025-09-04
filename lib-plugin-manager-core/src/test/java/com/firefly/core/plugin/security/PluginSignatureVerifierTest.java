/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.core.plugin.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

public class PluginSignatureVerifierTest {

    private PluginSignatureVerifier verifier;
    private Path unsignedJarPath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        // Create an unsigned JAR file
        File unsignedJar = tempDir.resolve("unsigned.jar").toFile();
        createUnsignedJar(unsignedJar);
        unsignedJarPath = unsignedJar.toPath();

        // Create the verifier
        verifier = new PluginSignatureVerifier(false);
    }

    @Test
    void testVerifyUnsignedJarWithoutRequirement() {
        // Create a verifier that doesn't require signatures
        PluginSignatureVerifier verifier = new PluginSignatureVerifier(false);

        // Verify the unsigned JAR
        boolean result = verifier.verifyPluginSignature(unsignedJarPath);
        assertFalse(result);
    }

    @Test
    void testVerifyUnsignedJarWithRequirement() {
        // Create a verifier that requires signatures
        PluginSignatureVerifier verifier = new PluginSignatureVerifier(true);

        // Verify the unsigned JAR
        assertThrows(SecurityException.class, () -> verifier.verifyPluginSignature(unsignedJarPath));
    }

    @Test
    void testAddTrustedCertificate() throws CertificateException {
        // Create a self-signed certificate
        String cert = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDazCCAlOgAwIBAgIUJFdUxtG6Kl/h1rE8lV/DYBCIzYUwDQYJKoZIhvcNAQEL\n" +
                "BQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM\n" +
                "GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMDA3MDIxMzM5NDNaFw0zMDA2\n" +
                "MzAxMzM5NDNaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw\n" +
                "HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB\n" +
                "AQUAA4IBDwAwggEKAoIBAQCwSAMGN6N7UUG/B/H5myV8AxEwBbRE3RFMKbQQVj+k\n" +
                "7P9cYoVuGNdJaYRjOeHUMZQXkCXTrLqZRpB9ZzYCxE1BNbHzM0Nk0YBzjvvVdS8x\n" +
                "ZhQlk5JXbiCJKOu5dYbZuCK2QG8BjL0YZiUiQVd8LVJvUiwMIpXRdCYCJ7G3aQhY\n" +
                "MYHbCUGzZUgD9v4yxgaJtJ5+32h/Z8gOjnGAW1JEFsSSQjKAZU0kUCvYZQBnEqHH\n" +
                "xpHMnAHlBGQxBtPD8qLSqHzHjbJcUxLlZ8ysE3yxU3n7KNj8qI2JJUUvLjbGmNLQ\n" +
                "cjqL2QMHt7JnQ3ujk+5jHxP8GUDTWzW8dHGrOcTpU8/JAgMBAAGjUzBRMB0GA1Ud\n" +
                "DgQWBBQdAYCkPnzYCFzQCQhx5uHgrXYYZDAfBgNVHSMEGDAWgBQdAYCkPnzYCFzQ\n" +
                "CQhx5uHgrXYYZDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBQ\n" +
                "UiVyU3JT7Q9N9rGqIpP3ZOTE8LL2yELR6WtJ2wgGqTFmYzUBH+1bJNFqEYqgZvAO\n" +
                "svUIhODMeHa7ZYvQKQQQKEEG7F8GJcMfEVV12ZBDJ3nHKDtpELQZVvgmYHoEiIl3\n" +
                "QSPP8rKiV9cQWTYwM0jQzEhkbFciQAZDFB6qjUzWDfwZIXA+tS8D0yYfc/YRS1iu\n" +
                "ZJM8GPE1U6UwJ7jeSlPJL+SlO5oacKkZFe4XU0AOraKY/1dht+Cjm5mQP5HY9YgC\n" +
                "5eiU+kPYOZV0tBPF9T+ssJFwQvTilILYztYRvhFGbHlkLjmf8aXSVdMwS2wnGQcB\n" +
                "NjLvzFYfKEA0KAu7ERkS\n" +
                "-----END CERTIFICATE-----";

        // Convert the certificate string to a certificate object
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                new java.io.ByteArrayInputStream(cert.getBytes()));

        // Add the certificate to the verifier
        verifier.addTrustedCertificate(certificate);
    }

    /**
     * Creates an unsigned JAR file with a minimal structure.
     */
    private void createUnsignedJar(File jarFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Plugin-Id", "test-plugin");

        try (FileOutputStream fos = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {

            // Add a dummy class file
            JarEntry entry = new JarEntry("com/example/TestClass.class");
            jos.putNextEntry(entry);
            jos.write(new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE });
            jos.closeEntry();
        }
    }
}
