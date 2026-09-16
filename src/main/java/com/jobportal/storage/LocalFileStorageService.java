package com.jobportal.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Active when app.storage.mode=local (the default) — used for local development. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "mode", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties storageProperties;

    @Override
    public void store(MultipartFile file, String storageKey) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        file.transferTo(target);
    }

    @Override
    public byte[] load(String storageKey) throws IOException {
        Path path = resolve(storageKey);
        if (!Files.exists(path)) {
            throw new IOException("File not found in local storage: " + storageKey);
        }
        return Files.readAllBytes(path);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete local file: " + storageKey, e);
        }
    }

    private Path resolve(String storageKey) {
        // storageKey is always generated server-side (UUID-based), never
        // taken from user input directly, but .normalize() is still cheap
        // defense-in-depth against path traversal.
        return Paths.get(storageProperties.getLocal().getBasePath()).resolve(storageKey).normalize();
    }
}