package com.catalis.core.plugin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Verifies the signatures of plugin JAR files.
 * This class ensures that plugins are signed with trusted certificates.
 */
public class PluginSignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(PluginSignatureVerifier.class);

    private final Set<X509Certificate> trustedCertificates = new HashSet<>();
    private final boolean requireSignature;

    /**
     * Creates a new PluginSignatureVerifier.
     *
     * @param requireSignature whether to require plugins to be signed
     */
    public PluginSignatureVerifier(boolean requireSignature) {
        this.requireSignature = requireSignature;
        logger.info("Plugin signature verifier initialized with requireSignature={}", requireSignature);
    }

    /**
     * Adds a trusted certificate.
     *
     * @param certificate the certificate to add
     */
    public void addTrustedCertificate(X509Certificate certificate) {
        trustedCertificates.add(certificate);
        logger.debug("Added trusted certificate: {}", certificate.getSubjectX500Principal().getName());
    }

    /**
     * Loads a trusted certificate from a file.
     *
     * @param certificatePath the path to the certificate file
     * @throws CertificateException if the certificate cannot be loaded
     * @throws IOException if an I/O error occurs
     */
    public void loadTrustedCertificate(Path certificatePath) throws CertificateException, IOException {
        try (InputStream is = java.nio.file.Files.newInputStream(certificatePath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
            addTrustedCertificate(cert);
            logger.info("Loaded trusted certificate from {}: {}", certificatePath, cert.getSubjectX500Principal().getName());
        }
    }

    /**
     * Verifies the signature of a plugin JAR file.
     *
     * @param pluginPath the path to the plugin JAR file
     * @return true if the signature is valid, false otherwise
     * @throws SecurityException if the plugin is not signed and signatures are required
     */
    public boolean verifyPluginSignature(Path pluginPath) throws SecurityException {
        logger.debug("Verifying signature of plugin: {}", pluginPath);
        
        try (JarFile jarFile = new JarFile(pluginPath.toFile(), true)) {
            // Get the manifest
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                if (requireSignature) {
                    throw new SecurityException("Plugin JAR does not contain a manifest");
                }
                logger.warn("Plugin JAR does not contain a manifest: {}", pluginPath);
                return false;
            }
            
            // Check all entries in the JAR file
            Enumeration<JarEntry> entries = jarFile.entries();
            List<X509Certificate> signerCertificates = new ArrayList<>();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                // Skip directories and the manifest itself
                if (entry.isDirectory() || entry.getName().equals("META-INF/MANIFEST.MF")) {
                    continue;
                }
                
                // Read the entry to verify its signature
                try (InputStream is = jarFile.getInputStream(entry)) {
                    byte[] buffer = new byte[8192];
                    while (is.read(buffer) != -1) {
                        // Just read, don't need to do anything with the data
                    }
                }
                
                // Check if the entry is signed
                CodeSigner[] signers = entry.getCodeSigners();
                if (signers == null || signers.length == 0) {
                    if (requireSignature) {
                        throw new SecurityException("Unsigned entry in plugin JAR: " + entry.getName());
                    }
                    logger.warn("Unsigned entry in plugin JAR: {}", entry.getName());
                    return false;
                }
                
                // Get the certificates from the first signer
                for (Certificate cert : signers[0].getSignerCertPath().getCertificates()) {
                    if (cert instanceof X509Certificate) {
                        signerCertificates.add((X509Certificate) cert);
                    }
                }
            }
            
            // If we got here, all entries are signed
            if (signerCertificates.isEmpty()) {
                if (requireSignature) {
                    throw new SecurityException("No signer certificates found in plugin JAR");
                }
                logger.warn("No signer certificates found in plugin JAR: {}", pluginPath);
                return false;
            }
            
            // Verify the certificates against the trusted certificates
            if (!trustedCertificates.isEmpty()) {
                boolean trusted = false;
                for (X509Certificate signerCert : signerCertificates) {
                    for (X509Certificate trustedCert : trustedCertificates) {
                        if (signerCert.equals(trustedCert)) {
                            trusted = true;
                            break;
                        }
                    }
                    if (trusted) {
                        break;
                    }
                }
                
                if (!trusted) {
                    if (requireSignature) {
                        throw new SecurityException("Plugin JAR is not signed with a trusted certificate");
                    }
                    logger.warn("Plugin JAR is not signed with a trusted certificate: {}", pluginPath);
                    return false;
                }
            }
            
            logger.debug("Plugin signature verification successful: {}", pluginPath);
            return true;
            
        } catch (IOException e) {
            if (requireSignature) {
                throw new SecurityException("Failed to verify plugin signature: " + e.getMessage(), e);
            }
            logger.warn("Failed to verify plugin signature: {}", e.getMessage());
            return false;
        }
    }
}
