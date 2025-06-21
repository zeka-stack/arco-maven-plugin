package dev.dong4j.arco.maven.plugin.boot.loader.jar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Extended variant of {@link java.util.jar.JarEntry} returned by {@link CustomJarFile}s.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
class CustomJarEntry extends java.util.jar.JarEntry implements FileHeader {

    /** Name */
    private final AsciiBytes jarName;

    /** Header name */
    private final AsciiBytes headerName;

    /** Certificates */
    private Certificate[] certificates;

    /** Code signers */
    private CodeSigner[] codeSigners;

    /** Jar file */
    private final CustomJarFile jarFile;

    /** Local header offset */
    private final long localHeaderOffset;

    /**
     * Jar entry
     *
     * @param jarFile   jar file
     * @param header    header
     * @param nameAlias name alias
     * @since 1.0.0
     */
    CustomJarEntry(CustomJarFile jarFile, CentralDirectoryFileHeader header, AsciiBytes nameAlias) {
        super((nameAlias != null) ? nameAlias.toString() : header.getName().toString());
        this.jarName = (nameAlias != null) ? nameAlias : header.getName();
        this.headerName = header.getName();
        this.jarFile = jarFile;
        this.localHeaderOffset = header.getLocalHeaderOffset();
        this.setCompressedSize(header.getCompressedSize());
        this.setMethod(header.getMethod());
        this.setCrc(header.getCrc());
        this.setComment(header.getComment().toString());
        this.setSize(header.getSize());
        this.setTime(header.getTime());
        if (header.hasExtra()) {
            this.setExtra(header.getExtra());
        }
    }

    /**
     * Gets ascii bytes name *
     *
     * @return the ascii bytes name
     * @since 1.0.0
     */
    AsciiBytes getAsciiBytesName() {
        return this.jarName;
    }

    /**
     * Has name boolean
     *
     * @param name   name
     * @param suffix suffix
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    public boolean hasName(CharSequence name, char suffix) {
        return this.headerName.matches(name, suffix);
    }

    /**
     * Return a {@link URL} for this {@link CustomJarEntry}.
     *
     * @return the URL for the entry
     * @throws MalformedURLException if the URL is not valid
     * @since 1.0.0
     */
    URL getUrl() throws MalformedURLException {
        return new URL(this.jarFile.getUrl(), this.getName());
    }

    /**
     * Gets attributes *
     *
     * @return the attributes
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Attributes getAttributes() throws IOException {
        Manifest manifest = this.jarFile.getManifest();
        return (manifest != null) ? manifest.getAttributes(this.getName()) : null;
    }

    /**
     * Get certificates certificate [ ]
     *
     * @return the certificate [ ]
     * @since 1.0.0
     */
    @Override
    public Certificate[] getCertificates() {
        if (this.jarFile.isSigned() && this.certificates == null) {
            this.jarFile.setupEntryCertificates(this);
        }
        return this.certificates;
    }

    /**
     * Get code signers code signer [ ]
     *
     * @return the code signer [ ]
     * @since 1.0.0
     */
    @Override
    public CodeSigner[] getCodeSigners() {
        if (this.jarFile.isSigned() && this.codeSigners == null) {
            this.jarFile.setupEntryCertificates(this);
        }
        return this.codeSigners;
    }

    /**
     * Sets certificates *
     *
     * @param entry entry
     * @since 1.0.0
     */
    void setCertificates(java.util.jar.JarEntry entry) {
        this.certificates = entry.getCertificates();
        this.codeSigners = entry.getCodeSigners();
    }

    /**
     * Gets local header offset *
     *
     * @return the local header offset
     * @since 1.0.0
     */
    @Override
    public long getLocalHeaderOffset() {
        return this.localHeaderOffset;
    }

}
