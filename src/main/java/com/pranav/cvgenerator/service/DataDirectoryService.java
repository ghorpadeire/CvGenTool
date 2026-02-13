package com.pranav.cvgenerator.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Manages a runtime-writable data directory for editable resource files.
 *
 * On first run, copies default files from classpath (src/main/resources)
 * into the writable location. All subsequent reads and writes go through
 * this directory, so edits persist across restarts without modifying
 * packaged resources.
 *
 * @author Pranav Ghorpade
 */
@Service
@Slf4j
public class DataDirectoryService {

    public static final String CANDIDATE_DATA_FILE = "candidate-data.json";
    public static final String SYSTEM_PROMPT_FILE = "cv-gen-system-prompt.txt";
    public static final String CANDIDATE_DATA_FRESHER_FILE = "candidate-data-fresher.json";
    public static final String SYSTEM_PROMPT_FRESHER_FILE = "cv-gen-system-prompt-fresher.txt";

    @Value("${app.data.dir:./data}")
    private String dataDir;

    private Path dataPath;

    @PostConstruct
    public void init() throws IOException {
        dataPath = Path.of(dataDir).toAbsolutePath().normalize();
        Files.createDirectories(dataPath);
        log.info("Writable data directory: {}", dataPath);

        seedIfMissing(CANDIDATE_DATA_FILE);
        seedIfMissing(SYSTEM_PROMPT_FILE);
        seedIfMissing(CANDIDATE_DATA_FRESHER_FILE);
        seedIfMissing(SYSTEM_PROMPT_FRESHER_FILE);
    }

    /**
     * Copies a classpath resource into the writable directory if the
     * file does not already exist there.
     */
    private void seedIfMissing(String filename) throws IOException {
        Path target = dataPath.resolve(filename);
        if (Files.exists(target)) {
            log.info("Writable file already exists: {}", target);
            return;
        }
        ClassPathResource resource = new ClassPathResource(filename);
        if (!resource.exists()) {
            log.warn("Classpath resource not found: {}", filename);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Seeded default {} into {}", filename, target);
    }

    /** Returns the resolved path for a file inside the data directory. */
    public Path resolve(String filename) {
        return dataPath.resolve(filename);
    }

    /** Reads the full contents of a data-directory file as a UTF-8 string. */
    public String readString(String filename) throws IOException {
        Path file = resolve(filename);
        if (Files.exists(file)) {
            return Files.readString(file);
        }
        // Fallback: read from classpath (e.g. if file was deleted)
        ClassPathResource resource = new ClassPathResource(filename);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /** Writes a UTF-8 string to a file in the data directory. */
    public void writeString(String filename, String content) throws IOException {
        Path file = resolve(filename);
        Files.writeString(file, content);
        log.info("Wrote {} bytes to {}", content.length(), file);
    }

    /** Returns the raw bytes of a data-directory file. */
    public byte[] readBytes(String filename) throws IOException {
        Path file = resolve(filename);
        if (Files.exists(file)) {
            return Files.readAllBytes(file);
        }
        ClassPathResource resource = new ClassPathResource(filename);
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }

    public Path getDataPath() {
        return dataPath;
    }
}
