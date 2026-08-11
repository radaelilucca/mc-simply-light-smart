package com.lucca.simplylightsmart.client.shader;

import com.lucca.simplylightsmart.SimplyLightSmart;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Produces shader-pack directories derived from compatible Complementary sources.
 *
 * <p>The original ZIP/folder is never changed. This deliberately mirrors Euphoria Patcher's
 * output model, so a player can select either the source pack or the generated Smart variant.
 * A second scan waits for Euphoria Patcher to finish creating its own derived folder.</p>
 */
final class SmartShaderPackPatcher {
    private static final String PATCH_LABEL = "Simply Light Smart";
    private static final String PATCH_FORMAT = "9";
    private static final String MANIFEST_FILE = ".simply-light-smart-patch.properties";
    private static final String TEMP_SUFFIX = ".simply-light-smart.tmp";
    private static final String BLOCK_PROPERTIES = "shaders/block.properties";
    private static final String TERRAIN_MATERIALS = "shaders/lib/materials/materialHandling/terrainMaterials.glsl";
    private static final String TERRAIN_IPBR = "shaders/lib/materials/materialHandling/terrainIPBR.glsl";
    private static final String EUPHORIA_SOURCE_PROPERTIES = "shaders/blockProperties/1.13+/block.10948-31015.properties";
    private static final String EUPHORIA_VOXELIZATION = "shaders/lib/voxelization/lightVoxelization.glsl";
    private static final String PHOTONICS_LIGHTS = "shaders/ph_lights.json";
    private static final String EUPHORIA_SIGNATURE = "This block.properties file is exclusive to Euphoria Patches";
    private static final String EUPHORIA_AUTOMATIC_EMISSION = "    } else if (mat > 20999 && mat < 21025){";
    private static final String STOCK_MARKER = "# Simply Light - Smart! dynamic emission start";
    private static final String EUPHORIA_MARKER = "# Simply Light - Smart! Euphoria texture dimming start";
    private static final int STOCK_MATERIAL_START = 12000;
    private static final int EUPHORIA_DYNAMIC_MAX_POWER = 11;
    private static final int EUPHORIA_NATIVE_MIN_POWER = EUPHORIA_DYNAMIC_MAX_POWER + 1;
    private static final int TEXTURE_MINIMUM_POWER = 2;
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SimplyLightSmart-ShaderPatcher");
        thread.setDaemon(true);
        return thread;
    });

    private SmartShaderPackPatcher() {
    }

    static void schedule(Path gameDirectory) {
        Path shaderpacks = gameDirectory.resolve("shaderpacks");
        EXECUTOR.schedule(() -> patchAvailable(shaderpacks), 2, TimeUnit.SECONDS);
        EXECUTOR.schedule(() -> patchAvailable(shaderpacks), 12, TimeUnit.SECONDS);
    }

    private static void patchAvailable(Path shaderpacks) {
        if (!Files.isDirectory(shaderpacks)) {
            return;
        }

        try (var entries = Files.list(shaderpacks)) {
            entries.filter(SmartShaderPackPatcher::isCandidate)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(source -> patchOne(shaderpacks, source));
        } catch (IOException exception) {
            SimplyLightSmart.LOGGER.warn("Unable to inspect shaderpacks for Simply Light - Smart! support", exception);
            SmartShaderPatchNotifications.failed("shaderpacks", conciseReason(exception));
        }
    }

    private static boolean isCandidate(Path path) {
        String name = path.getFileName().toString();
        boolean supportedName = name.startsWith("ComplementaryReimagined_") || name.startsWith("ComplementaryUnbound_");
        if (!supportedName || name.contains(" + " + PATCH_LABEL + "_")) {
            return false;
        }
        if (name.contains(" + EuphoriaPatches_")) {
            return Files.isDirectory(path);
        }
        return Files.isDirectory(path) || (Files.isRegularFile(path) && name.endsWith(".zip"));
    }

    private static void patchOne(Path shaderpacks, Path source) {
        String sourceName = source.getFileName().toString();
        String targetName = withoutZipExtension(sourceName) + " + " + PATCH_LABEL + "_" + SimplyLightSmart.version();
        Path target = shaderpacks.resolve(targetName);
        try {
            SourceKind kind = sourceName.contains(" + EuphoriaPatches_") ? SourceKind.EUPHORIA : SourceKind.STOCK;
            if (sourceAlreadyPatched(source, kind)) {
                return;
            }
            String fingerprint = fingerprint(source, kind);
            if (isCurrentOutput(target, sourceName, fingerprint, kind)) {
                removeStaleOutputs(shaderpacks, target, sourceName, kind);
                return;
            }

            if (Files.exists(target) && !isManagedOutput(target)) {
                throw new IOException("an existing folder has the generated-pack name but is not managed by Simply Light - Smart!");
            }

            Path temporary = shaderpacks.resolve(targetName + TEMP_SUFFIX);
            removeManagedTemporaryDirectory(temporary, shaderpacks);
            copySource(source, temporary);
            applyPatch(temporary, kind);
            writeManifest(temporary, sourceName, fingerprint, kind);
            replaceGeneratedOutput(temporary, target, shaderpacks);
            removeStaleOutputs(shaderpacks, target, sourceName, kind);
            SmartShaderPatchNotifications.created(targetName);
            SimplyLightSmart.LOGGER.info("Created patched shader pack {} from {}", targetName, sourceName);
        } catch (Exception exception) {
            SimplyLightSmart.LOGGER.warn("Could not create patched shader pack from {}", sourceName, exception);
            SmartShaderPatchNotifications.failed(sourceName, conciseReason(exception));
        }
    }

    private static boolean sourceAlreadyPatched(Path source, SourceKind kind) throws IOException {
        String marker = kind == SourceKind.EUPHORIA ? EUPHORIA_MARKER : STOCK_MARKER;
        if (Files.isDirectory(source)) {
            return readShaderText(source.resolve(BLOCK_PROPERTIES)).contains(marker);
        }
        try (ZipFile zip = new ZipFile(source.toFile())) {
            ZipEntry entry = zip.getEntry(BLOCK_PROPERTIES);
            if (entry == null) {
                throw new IOException("required shader file is missing: block.properties");
            }
            try (InputStream input = zip.getInputStream(entry)) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8).contains(marker);
            }
        }
    }

    private static String withoutZipExtension(String name) {
        return name.endsWith(".zip") ? name.substring(0, name.length() - ".zip".length()) : name;
    }

    private static boolean isCurrentOutput(Path target, String sourceName, String fingerprint, SourceKind kind) throws IOException {
        if (!Files.isDirectory(target) || !isManagedOutput(target)) {
            return false;
        }
        Map<String, String> manifest = readManifest(target);
        return PATCH_FORMAT.equals(manifest.get("format"))
                && SimplyLightSmart.version().equals(manifest.get("addonVersion"))
                && sourceName.equals(manifest.get("sourceName"))
                && fingerprint.equals(manifest.get("sourceFingerprint"))
                && kind.name().equals(manifest.get("sourceKind"));
    }

    /** Removes only older managed outputs derived from the same source shader pack. */
    private static void removeStaleOutputs(Path shaderpacks, Path currentTarget, String sourceName, SourceKind kind) {
        String managedPrefix = withoutZipExtension(sourceName) + " + " + PATCH_LABEL + "_";
        try (var entries = Files.list(shaderpacks)) {
            List<Path> candidates = entries
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(currentTarget))
                    .filter(path -> path.getFileName().toString().startsWith(managedPrefix))
                    .toList();
            for (Path candidate : candidates) {
                if (!isStaleOutput(candidate, sourceName, kind)) {
                    continue;
                }
                deleteManagedDirectory(candidate, shaderpacks);
                SimplyLightSmart.LOGGER.info("Removed outdated patched shader pack {}", candidate.getFileName());
            }
        } catch (IOException exception) {
            SimplyLightSmart.LOGGER.warn("Could not remove outdated Simply Light - Smart! shader packs for {}", sourceName, exception);
        }
    }

    private static boolean isStaleOutput(Path candidate, String sourceName, SourceKind kind) throws IOException {
        if (!isManagedOutput(candidate)) {
            return false;
        }
        Map<String, String> manifest = readManifest(candidate);
        return sourceName.equals(manifest.get("sourceName"))
                && kind.name().equals(manifest.get("sourceKind"))
                && !SimplyLightSmart.version().equals(manifest.get("addonVersion"));
    }

    private static boolean isManagedOutput(Path path) throws IOException {
        return Files.isDirectory(path) && Files.isRegularFile(path.resolve(MANIFEST_FILE));
    }

    private static Map<String, String> readManifest(Path directory) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String line : Files.readAllLines(directory.resolve(MANIFEST_FILE), StandardCharsets.UTF_8)) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                values.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return values;
    }

    private static void writeManifest(Path directory, String sourceName, String fingerprint, SourceKind kind) throws IOException {
        String content = String.join("\n",
                "format=" + PATCH_FORMAT,
                "addonVersion=" + SimplyLightSmart.version(),
                "sourceName=" + sourceName,
                "sourceFingerprint=" + fingerprint,
                "sourceKind=" + kind.name(),
                "");
        Files.writeString(directory.resolve(MANIFEST_FILE), content, StandardCharsets.UTF_8);
    }

    private static String fingerprint(Path source, SourceKind kind) throws IOException {
        MessageDigest digest = sha256();
        if (Files.isRegularFile(source)) {
            updateDigest(digest, source);
            return toHex(digest.digest());
        }
        List<String> paths = kind == SourceKind.EUPHORIA
                ? List.of(BLOCK_PROPERTIES, EUPHORIA_SOURCE_PROPERTIES, TERRAIN_MATERIALS, TERRAIN_IPBR, EUPHORIA_VOXELIZATION, PHOTONICS_LIGHTS)
                : List.of(BLOCK_PROPERTIES, TERRAIN_MATERIALS, TERRAIN_IPBR);
        for (String relative : paths) {
            digest.update(relative.getBytes(StandardCharsets.UTF_8));
            updateDigest(digest, source.resolve(relative));
        }
        return toHex(digest.digest());
    }

    private static MessageDigest sha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }

    private static void updateDigest(MessageDigest digest, Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("required shader file is missing: " + file.getFileName());
        }
        try (InputStream input = Files.newInputStream(file)) {
            input.transferTo(new java.io.OutputStream() {
                @Override
                public void write(int value) {
                    digest.update((byte) value);
                }

                @Override
                public void write(byte[] bytes, int offset, int length) {
                    digest.update(bytes, offset, length);
                }
            });
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format(Locale.ROOT, "%02x", value));
        }
        return result.toString();
    }

    private static void copySource(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);
        if (Files.isRegularFile(source)) {
            copyZip(source, destination);
            return;
        }
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                Files.createDirectories(destination.resolve(source.relativize(directory).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file).toString()), StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyZip(Path source, Path destination) throws IOException {
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        try (ZipFile zip = new ZipFile(source.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path output = normalizedDestination.resolve(entry.getName().replace('\\', '/')).normalize();
                if (!output.startsWith(normalizedDestination)) {
                    throw new IOException("shader ZIP contains an unsafe entry");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }
                Files.createDirectories(output.getParent());
                try (InputStream input = zip.getInputStream(entry); OutputStream stream = Files.newOutputStream(output)) {
                    input.transferTo(stream);
                }
            }
        }
    }

    private static void applyPatch(Path pack, SourceKind kind) throws IOException {
        if (kind == SourceKind.EUPHORIA) {
            applyEuphoriaPatch(pack);
        } else {
            applyStockPatch(pack);
        }
    }

    private static void applyStockPatch(Path pack) throws IOException {
        Path propertiesPath = pack.resolve(BLOCK_PROPERTIES);
        Path terrainPath = pack.resolve(TERRAIN_MATERIALS);
        Path ipbrPath = pack.resolve(TERRAIN_IPBR);
        String properties = readShaderText(propertiesPath);
        String terrain = readShaderText(terrainPath);
        String ipbr = readShaderText(ipbrPath);
        if (isEuphoria(properties, terrain)) {
            throw new IOException("the source is an Euphoria pack and must use the Euphoria output path");
        }
        ensureNoMarker(properties, terrain, STOCK_MARKER);
        for (int material = STOCK_MATERIAL_START; material <= STOCK_MATERIAL_START + 15; material++) {
            if (properties.contains("\nblock." + material + "=") || properties.startsWith("block." + material + "=")) {
                throw new IOException("material ID " + material + " is already used by this Complementary version");
            }
        }
        properties = addAfterUnique(properties, "block.10028=", stockPropertySection(), "block.properties");
        terrain = addAfterUnique(terrain, stockTerrainAnchor(), stockTerrainSection(), "terrainMaterials.glsl");
        ipbr = replaceUnique(ipbr, stockIpbrAnchor(), stockIpbrSection(), "terrainIPBR.glsl");
        writeShaderText(propertiesPath, properties);
        writeShaderText(terrainPath, terrain);
        writeShaderText(ipbrPath, ipbr);
    }

    private static void applyEuphoriaPatch(Path pack) throws IOException {
        Path propertiesPath = pack.resolve(BLOCK_PROPERTIES);
        Path sourcePropertiesPath = pack.resolve(EUPHORIA_SOURCE_PROPERTIES);
        Path terrainPath = pack.resolve(TERRAIN_MATERIALS);
        Path ipbrPath = pack.resolve(TERRAIN_IPBR);
        Path voxelizationPath = pack.resolve(EUPHORIA_VOXELIZATION);
        Path photonicsPath = pack.resolve(PHOTONICS_LIGHTS);
        String properties = readShaderText(propertiesPath);
        String sourceProperties = readShaderText(sourcePropertiesPath);
        String terrain = readShaderText(terrainPath);
        String ipbr = readShaderText(ipbrPath);
        String voxelization = readShaderText(voxelizationPath);
        String photonics = readShaderText(photonicsPath);
        if (!isEuphoria(properties, terrain)) {
            throw new IOException("the generated Euphoria pack signature was not found");
        }
        ensureNoMarker(properties, terrain, EUPHORIA_MARKER);
        properties = euphoriaPropertyContent(properties);
        sourceProperties = euphoriaPropertyContent(sourceProperties);
        terrain = replaceUnique(terrain, euphoriaTerrainAnchor(), euphoriaTerrainSection(), "terrainMaterials.glsl");
        ipbr = replaceUnique(ipbr, euphoriaIpbrAnchor(), euphoriaIpbrSection(), "terrainIPBR.glsl");
        voxelization = replaceUnique(voxelization, euphoriaVoxelizationAnchor(), euphoriaVoxelizationSection(), "lightVoxelization.glsl");
        photonics = euphoriaPhotonicsContent(photonics);
        writeShaderText(propertiesPath, properties);
        writeShaderText(sourcePropertiesPath, sourceProperties);
        writeShaderText(terrainPath, terrain);
        writeShaderText(ipbrPath, ipbr);
        writeShaderText(voxelizationPath, voxelization);
        writeShaderText(photonicsPath, photonics);
    }

    private static boolean isEuphoria(String properties, String terrain) {
        return properties.contains(EUPHORIA_SIGNATURE)
                || terrain.contains(EUPHORIA_AUTOMATIC_EMISSION)
                || terrain.contains(EUPHORIA_MARKER);
    }

    private static void ensureNoMarker(String properties, String terrain, String marker) throws IOException {
        if (properties.contains(marker) || terrain.contains(marker)) {
            throw new IOException("the source already contains a Simply Light - Smart! patch marker");
        }
    }

    private static String readShaderText(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("required shader file is missing: " + path.getFileName());
        }
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static void writeShaderText(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String addAfterUnique(String content, String anchor, String addition, String description) throws IOException {
        int first = content.indexOf(anchor);
        if (first < 0 || first != content.lastIndexOf(anchor)) {
            throw new IOException("expected exactly one supported anchor in " + description);
        }
        return content.substring(0, first + anchor.length()) + "\n" + addition + content.substring(first + anchor.length());
    }

    private static String replaceUnique(String content, String anchor, String replacement, String description) throws IOException {
        int first = content.indexOf(anchor);
        if (first < 0 || first != content.lastIndexOf(anchor)) {
            throw new IOException("expected exactly one supported anchor in " + description);
        }
        return content.substring(0, first) + replacement + content.substring(first + anchor.length());
    }

    private static String stockPropertySection() {
        List<String> lines = new ArrayList<>();
        lines.add(STOCK_MARKER);
        lines.add("# Maps original Simply Light blocks by smart_power for dynamic shader emission.");
        for (int power = 0; power <= 15; power++) {
            List<String> states = new ArrayList<>();
            for (String block : simplyLightBlocks()) {
                states.add(block + ":smart_power=" + power);
            }
            lines.add("block." + (STOCK_MATERIAL_START + power) + "=" + String.join(" ", states));
        }
        lines.add("# Simply Light - Smart! dynamic emission end");
        return String.join("\n", lines) + "\n";
    }

    private static String stockTerrainAnchor() {
        return "    } else if (mat == 10028) { // Modded Light Sources\n"
                + "        noSmoothLighting = true; noDirectionalShading = true;\n"
                + "        emission = GetLuminance(color.rgb) * 2.5;\n"
                + "    }";
    }

    private static String stockTerrainSection() {
        return "    else if (mat >= 12000 && mat <= 12015) { // Simply Light - Smart! dynamic emission\n"
                + "        noSmoothLighting = true; noDirectionalShading = true;\n"
                + visualDimmingVariables("        ", "mat - 12000")
                + applyVisualLightmapDimmingCode("        ", "mat - 12000")
                + "        emission = GetLuminance(color.rgb) * 2.5 * simplyLightSmartVisualEmissionPower;\n"
                + applyTextureDimmingCode("        ")
                + "    }";
    }

    private static String stockIpbrAnchor() {
        return "if (mat >= 10000) {\n"
                + "if (mat < 11024) {";
    }

    private static String stockIpbrSection() {
        return "if (mat >= 10000) {\n"
                + "if (mat >= 12000 && mat <= 12015) { // Simply Light - Smart! dynamic emission\n"
                + "    noSmoothLighting = true; noDirectionalShading = true;\n"
                + visualDimmingVariables("    ", "mat - 12000")
                + applyVisualLightmapDimmingCode("    ", "mat - 12000")
                + "    emission = GetLuminance(color.rgb) * 2.5 * simplyLightSmartVisualEmissionPower;\n"
                + applyTextureDimmingCode("    ")
                + "}\n"
                + "else if (mat < 11024) {";
    }

    /**
     * Keeps the first three redstone steps at full visual brightness, then transitions
     * through steps four and five before dimming bright texels normally. The real
     * block-light level is intentionally untouched.
     */
    private static String visualDimmingVariables(String indentation, String lightLevelExpression) {
        String continuation = indentation + "    ";
        return indentation + "float simplyLightSmartDimming = 15.0 - float(" + lightLevelExpression + ");\n"
                + indentation + "float simplyLightSmartCompressedDimming = simplyLightSmartDimming <= 3.0\n"
                + continuation + "? 0.0\n"
                + continuation + ": simplyLightSmartDimming <= 5.0\n"
                + continuation + "? (simplyLightSmartDimming - 3.0) * 0.5\n"
                + continuation + ": 1.0 + (simplyLightSmartDimming - 5.0) * 1.4;\n"
                + indentation + "float simplyLightSmartTextureBrightness = clamp(1.0 - simplyLightSmartCompressedDimming / 15.0, 0.0, 1.0);\n"
                + indentation + "float simplyLightSmartVisualEmissionPower = smoothstep(0.0, 1.0, simplyLightSmartTextureBrightness);\n"
                + indentation + "float simplyLightSmartTexturePower = max(simplyLightSmartVisualEmissionPower, "
                + TEXTURE_MINIMUM_POWER + ".0 / 15.0);\n"
                + indentation + "float simplyLightSmartColorPeak = max(color.r, max(color.g, color.b));\n"
                + indentation + "float simplyLightSmartTextureMask = smoothstep(0.55, 0.90, simplyLightSmartColorPeak);\n";
    }

    private static String applyTextureDimmingCode(String indentation) {
        return indentation + "color.rgb *= mix(1.0, simplyLightSmartTexturePower, simplyLightSmartTextureMask);\n";
    }

    /**
     * Remaps only this material's lighting input to the visual dimming curve. This keeps
     * the bright bloom during the first three redstone steps without changing emitted
     * block light, colored-light voxelization, or the world lightmap for other blocks.
     */
    private static String applyVisualLightmapDimmingCode(String indentation, String lightLevelExpression) {
        return indentation + "float simplyLightSmartActualLightLevel = float(" + lightLevelExpression + ") / 15.0;\n"
                + indentation + "if (simplyLightSmartActualLightLevel > 0.0) {\n"
                + indentation + "    float simplyLightSmartLightmapScale = simplyLightSmartTextureBrightness / simplyLightSmartActualLightLevel;\n"
                + indentation + "    lmCoordM.x = clamp(lmCoordM.x * simplyLightSmartLightmapScale, 0.0, 1.0);\n"
                + indentation + "}\n";
    }

    private static String euphoriaPropertyContent(String content) {
        StringBuilder result = new StringBuilder(content.length() + 8192);
        int index = 0;
        while (index < content.length()) {
            int start = content.indexOf("simplylight:", index);
            if (start < 0) {
                result.append(content, index, content.length());
                break;
            }
            boolean tokenBoundary = start == 0 || Character.isWhitespace(content.charAt(start - 1));
            int end = start;
            while (end < content.length() && !Character.isWhitespace(content.charAt(end)) && content.charAt(end) != '\\') {
                end++;
            }
            result.append(content, index, start);
            String selector = content.substring(start, end);
            if (tokenBoundary && !selector.contains(":smart_power=")) {
                for (int power = EUPHORIA_NATIVE_MIN_POWER; power <= 15; power++) {
                    if (power > EUPHORIA_NATIVE_MIN_POWER) {
                        result.append(' ');
                    }
                    result.append(selector).append(":smart_power=").append(power);
                }
            } else {
                result.append(selector);
            }
            index = end;
        }
        return result.toString().stripTrailing() + "\n\n" + euphoriaPropertySection() + "\n";
    }

    private static String euphoriaPropertySection() {
        List<String> lines = new ArrayList<>();
        lines.add(EUPHORIA_MARKER);
        lines.add("# Keeps Euphoria's native bright material through smart_power 12-15; dims below that.");
        for (EuphoriaColor color : EuphoriaColor.values()) {
            if (color.combinedWhite) {
                continue;
            }
            lines.add("block." + color.materialId + "=" + String.join(" ", euphoriaSelectors(List.of(color))));
        }
        lines.add("block.21100=" + String.join(" ", euphoriaSelectors(List.of(EuphoriaColor.WHITE, EuphoriaColor.GRAY, EuphoriaColor.LIGHT_GRAY, EuphoriaColor.BLACK))));
        lines.add("# Simply Light - Smart! Euphoria texture dimming end");
        return String.join("\n", lines);
    }

    private static List<String> euphoriaSelectors(List<EuphoriaColor> colors) {
        List<String> selectors = new ArrayList<>();
        for (EuphoriaColor color : colors) {
            String suffix = color.blockSuffix.isEmpty() ? "" : "_" + color.blockSuffix;
            for (String family : List.of("lightbulb", "illuminant_slab", "illuminant_panel", "wall_lamp", "rodlamp", "edge_light", "edge_light_top")) {
                for (int power = 0; power <= EUPHORIA_DYNAMIC_MAX_POWER; power++) {
                    selectors.add("simplylight:" + family + suffix + ":smart_power=" + power);
                }
            }
            for (int power = 0; power <= EUPHORIA_DYNAMIC_MAX_POWER; power++) {
                selectors.add("simplylight:lamp_post" + suffix + ":position=top:smart_power=" + power);
            }
            String illuminant = color.blockSuffix.isEmpty() ? "illuminant_block" : "illuminant_" + color.blockSuffix + "_block";
            for (String block : List.of(illuminant, illuminant + "_on")) {
                for (int power = 0; power <= EUPHORIA_DYNAMIC_MAX_POWER; power++) {
                    selectors.add("simplylight:" + block + ":on=true:smart_power=" + power);
                }
            }
        }
        return selectors;
    }

    private static String euphoriaTerrainAnchor() {
        return "    } else if (mat > 20999 && mat < 21025){\n"
                + "        emission = DoAutomaticEmission(noSmoothLighting, noDirectionalShading, color.rgb, lmCoord.x, blockLightEmission, 1.0);\n"
                + "    }";
    }

    private static String euphoriaTerrainSection() {
        return "    } else if ((mat > 20999 && mat < 21025) || (mat > 21099 && mat < 21125)) {\n"
                + "        if (mat > 21099 && mat < 21125) { // Simply Light - Smart! texture dimming\n"
                + "            noSmoothLighting = true; noDirectionalShading = true;\n"
                + visualDimmingVariables("            ", "blockLightEmission")
                + applyVisualLightmapDimmingCode("            ", "blockLightEmission")
                + euphoriaVisualEmissionCode("            ")
                + applyTextureDimmingCode("            ")
                + "        }\n"
                + "        else {\n"
                + "            emission = DoAutomaticEmission(noSmoothLighting, noDirectionalShading, color.rgb, lmCoord.x, blockLightEmission, 1.0);\n"
                + "        }\n"
                + "    }";
    }

    private static String euphoriaIpbrAnchor() {
        return "    } else if (mat > 20999 && mat < 21025) {\n"
                + "    #ifdef GBUFFERS_TERRAIN\n"
                + "        emission = DoAutomaticEmission(noSmoothLighting, noDirectionalShading, color.rgb, lmCoord.x, blockLightEmission, 1.0);\n"
                + "    #else\n"
                + "        bool doesNothing;\n"
                + "        emission = DoAutomaticEmission(noSmoothLighting, doesNothing, color.rgb, 0.0, 15, 1.0);\n"
                + "    #endif\n"
                + "}";
    }

    private static String euphoriaIpbrSection() {
        return "    } else if ((mat > 20999 && mat < 21025) || (mat > 21099 && mat < 21125)) {\n"
                + "    #ifdef GBUFFERS_TERRAIN\n"
                + "        if (mat > 21099 && mat < 21125) { // Simply Light - Smart! texture dimming\n"
                + "            noSmoothLighting = true; noDirectionalShading = true;\n"
                + visualDimmingVariables("            ", "blockLightEmission")
                + applyVisualLightmapDimmingCode("            ", "blockLightEmission")
                + euphoriaVisualEmissionCode("            ")
                + applyTextureDimmingCode("            ")
                + "        }\n"
                + "        else {\n"
                + "            emission = DoAutomaticEmission(noSmoothLighting, noDirectionalShading, color.rgb, lmCoord.x, blockLightEmission, 1.0);\n"
                + "        }\n"
                + "    #else\n"
                + "        bool doesNothing;\n"
                + "        emission = DoAutomaticEmission(noSmoothLighting, doesNothing, color.rgb, 0.0, 15, 1.0);\n"
                + "    #endif\n"
                + "}";
    }

    /** Mirrors Euphoria's material-emission formula with the visual dimming curve only. */
    private static String euphoriaVisualEmissionCode(String indentation) {
        return indentation + "float simplyLightSmartVisualLightLevel = max(float(lmCoord.x > 0.99), simplyLightSmartTextureBrightness);\n"
                + indentation + "float simplyLightSmartVisualBaseEmission = pow3(GetLuminance(color.rgb)) * pow1_5(simplyLightSmartVisualLightLevel);\n"
                + indentation + "emission = max(1.0, (simplyLightSmartVisualBaseEmission - 0.1) * 2.5);\n";
    }

    private static String euphoriaVoxelizationAnchor() {
        return "                                if (mat == 21024) return  80; // Pink Modded Blocks\n"
                + "                                if (mat == 30008) return 254; // Tinted Glass";
    }

    private static String euphoriaVoxelizationSection() {
        return "                                if (mat == 21024) return  80; // Pink Modded Blocks\n"
                + "                                if (mat == 21100) return  97; // White Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21102) return  43; // Brown Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21104) return  70; // Red Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21106) return  71; // Orange Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21108) return  72; // Yellow Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21110) return  73; // Lime Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21112) return  74; // Green Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21114) return  75; // Cyan Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21116) return  76; // Light Blue Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21118) return  77; // Blue Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21120) return  78; // Purple Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21122) return  79; // Magenta Simply Light - Smart! Blocks\n"
                + "                                if (mat == 21124) return  80; // Pink Simply Light - Smart! Blocks\n"
                + "                                if (mat == 30008) return 254; // Tinted Glass";
    }

    private static String euphoriaPhotonicsContent(String content) throws IOException {
        String patched = content;
        for (EuphoriaColor color : EuphoriaColor.values()) {
            if (color == EuphoriaColor.GRAY || color == EuphoriaColor.LIGHT_GRAY || color == EuphoriaColor.BLACK) {
                continue;
            }
            int sourceMaterial = color.materialId - 100;
            String anchor = "\"blocks\": [\"*" + sourceMaterial + "\"]";
            String replacement = "\"blocks\": [\"*" + sourceMaterial + "\", \"*" + color.materialId + "\"]";
            patched = replaceUnique(patched, anchor, replacement, "ph_lights.json");
        }
        return patched;
    }

    private static List<String> simplyLightBlocks() {
        List<String> blocks = new ArrayList<>();
        List<String> colors = List.of("", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
        List<String> families = List.of("lightbulb", "illuminant_slab", "illuminant_panel", "wall_lamp", "rodlamp", "edge_light", "edge_light_top", "lamp_post");
        for (String family : families) {
            for (String color : colors) {
                blocks.add("simplylight:" + family + (color.isEmpty() ? "" : "_" + color));
            }
        }
        for (String color : colors) {
            String base = color.isEmpty() ? "illuminant_block" : "illuminant_" + color + "_block";
            blocks.add("simplylight:" + base);
            blocks.add("simplylight:" + base + "_on");
        }
        return blocks;
    }

    private static void replaceGeneratedOutput(Path temporary, Path target, Path shaderpacks) throws IOException {
        if (Files.exists(target)) {
            deleteManagedDirectory(target, shaderpacks);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target);
        }
    }

    private static void removeManagedTemporaryDirectory(Path temporary, Path shaderpacks) throws IOException {
        if (!Files.exists(temporary)) {
            return;
        }
        if (!temporary.getFileName().toString().endsWith(TEMP_SUFFIX)) {
            throw new IOException("refusing to remove an unexpected temporary path");
        }
        deleteDirectory(temporary, shaderpacks);
    }

    private static void deleteManagedDirectory(Path directory, Path shaderpacks) throws IOException {
        if (!isManagedOutput(directory)) {
            throw new IOException("refusing to replace an unmanaged generated-pack directory");
        }
        deleteDirectory(directory, shaderpacks);
    }

    private static void deleteDirectory(Path directory, Path shaderpacks) throws IOException {
        Path normalizedRoot = shaderpacks.toAbsolutePath().normalize();
        Path normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!normalizedDirectory.startsWith(normalizedRoot) || normalizedDirectory.equals(normalizedRoot)) {
            throw new IOException("refusing to remove a path outside shaderpacks");
        }
        Files.walkFileTree(normalizedDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path folder, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(folder);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String conciseReason(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private enum SourceKind {
        STOCK,
        EUPHORIA
    }

    private enum EuphoriaColor {
        WHITE("", 21100, true),
        BROWN("brown", 21102, false),
        RED("red", 21104, false),
        ORANGE("orange", 21106, false),
        YELLOW("yellow", 21108, false),
        LIME("lime", 21110, false),
        GREEN("green", 21112, false),
        CYAN("cyan", 21114, false),
        LIGHT_BLUE("light_blue", 21116, false),
        BLUE("blue", 21118, false),
        PURPLE("purple", 21120, false),
        MAGENTA("magenta", 21122, false),
        PINK("pink", 21124, false),
        GRAY("gray", 21100, true),
        LIGHT_GRAY("light_gray", 21100, true),
        BLACK("black", 21100, true);

        private final String blockSuffix;
        private final int materialId;
        private final boolean combinedWhite;

        EuphoriaColor(String blockSuffix, int materialId, boolean combinedWhite) {
            this.blockSuffix = blockSuffix;
            this.materialId = materialId;
            this.combinedWhite = combinedWhite;
        }
    }
}
