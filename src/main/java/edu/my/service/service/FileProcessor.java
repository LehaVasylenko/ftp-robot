package edu.my.service.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.my.service.Main;
import edu.my.service.model.Envelope;
import edu.my.service.model.EnvelopeType;
import edu.my.service.model.nomenclature.NomenclatureIncoming;
import edu.my.service.model.rests.OffersWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileProcessor {
    private static final Logger log = LoggerFactory.getLogger(FileProcessor.class);
    private final XmlMapper xmlMapper = new XmlMapper();

    private static final String GOODS = "goods";
    private static final String RESTS = "Rest";

    private static final Path TEMP_DIR = Paths.get("src/main/java/edu/my/service/ftp/unzip-temp");// временная папка для распаковки

    public FileProcessor() throws IOException {
        if (!Files.exists(TEMP_DIR)) {
            Files.createDirectories(TEMP_DIR);
        }
    }

    public Optional<Envelope> processZipFile(Path zipFile) {
        Path unzipFolder = TEMP_DIR.resolve(zipFile.getFileName().toString().replace(".zip", ""));
        try {
            Envelope e = unzip(zipFile, unzipFolder);
            e = parseXmlFiles(unzipFolder, e);
            return Optional.of(e);
        } catch (Exception ex) {
            log.error("❌ Error {}: {}", zipFile, ex.getMessage());
            return Optional.empty();
        } finally {
            try { deleteRecursive(unzipFolder); } catch (IOException ignored) {}
        }
    }

    private Envelope parseXmlFiles(Path folder, Envelope env) throws IOException {
        try (var s = Files.walk(folder)) {
            Path xml = s.filter(p -> p.toString().endsWith(".xml"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No XML files found in " + folder));

            byte[] xmlBytes = Files.readAllBytes(xml);
            if (env.type() == EnvelopeType.GOODS) {
                var dto = xmlMapper.readValue(xmlBytes, NomenclatureIncoming.class);
                return new Envelope(env.type(), env.branchId(), dto, null);
            } else {
                var dto = xmlMapper.readValue(xmlBytes, OffersWrapper.class);
                return new Envelope(env.type(), env.branchId(), null, dto);
            }
        }
    }

    private Envelope unzip(Path zipPath, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Envelope e = getEnvelope(zipPath);
        long totalBytes = 0;
        int entries = 0;
        final long MAX_TOTAL = 512L * 1024 * 1024; // 512MB
        final int  MAX_ENTRIES = 10_000;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains("..")) throw new IOException("Path traversal: " + entry.getName());
                Path out = targetDir.resolve(entry.getName()).normalize();
                if (!out.startsWith(targetDir)) throw new IOException("Outside target dir: " + entry.getName());

                if (entry.isDirectory()) { Files.createDirectories(out); }
                else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out)) {
                        long copied = zis.transferTo(os);
                        totalBytes += copied;
                        entries++;
                        if (totalBytes > MAX_TOTAL) throw new IOException("Zip too large");
                        if (entries > MAX_ENTRIES) throw new IOException("Too many files in zip");
                    }
                }
                zis.closeEntry();
            }
        }
        return e;
    }

    private Envelope getEnvelope(Path zipPath) {
        String filename = zipPath.getFileName().toString(); // например: Rest_12345_20250808.zip
        String base = filename.replaceFirst("\\.zip$", "");
        String[] parts = base.split("_");
        if (parts.length < 2) throw new IllegalArgumentException("Bad name: " + filename);

        String cleanName = parts[0]; // Rest | goods
        String branchId = parts[1];

        EnvelopeType type = switch (cleanName) {
            case GOODS -> EnvelopeType.GOODS;
            case RESTS -> EnvelopeType.REST;
            default -> throw new IllegalStateException("Unexpected type: " + cleanName);
        };
        return new Envelope(type, branchId, null, null);
    }

    private void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }
}
