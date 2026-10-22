package io.github.tarun922.lifabt;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * For every requested icon: use the cached SVG if present, otherwise download it
 * (first run only), then convert it to a VectorDrawable in the generated res dir.
 */
public abstract class GenerateLucideIconsTask extends DefaultTask {

    private static final Pattern ICON = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern VERSION = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern PREFIX = Pattern.compile("[a-z][a-z0-9_]*");

    @Input
    public abstract ListProperty<String> getIcons();

    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getPrefix();

    /** Generated res root; drawables go in drawable/ below it. */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /** Persistent SVG cache. Commit it to git to make builds work offline / on CI. */
    @Internal
    public abstract DirectoryProperty getCacheDir();

    @TaskAction
    public void generate() throws IOException {
        String version = getVersion().get();
        String prefix = getPrefix().get();
        if (!VERSION.matcher(version).matches()) throw new GradleException("lifabt.version has invalid characters: " + version);
        if (!PREFIX.matcher(prefix).matches()) throw new GradleException("lifabt.prefix must match [a-z][a-z0-9_]*: " + prefix);

        Path cache = getCacheDir().get().getAsFile().toPath().resolve(version);
        Path drawables = getOutputDir().get().getAsFile().toPath().resolve("drawable");
        // Start clean so icons removed from the list disappear from the app.
        getProject().delete(drawables.toFile());
        Files.createDirectories(drawables);
        Files.createDirectories(cache);

        for (String icon : getIcons().get()) {
            if (!ICON.matcher(icon).matches()) {
                throw new GradleException("Invalid icon name '" + icon + "'. Use the lucide.dev name, e.g. \"arrow-right\".");
            }
            Path svg = cache.resolve(icon + ".svg");
            if (!Files.exists(svg)) {
                download(icon, version, svg);
            }
            String xml;
            try {
                xml = SvgToVector.convert(new String(Files.readAllBytes(svg), StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new GradleException("Could not convert icon '" + icon + "': " + e.getMessage(), e);
            }
            String resName = prefix + icon.replace('-', '_');
            Files.write(drawables.resolve(resName + ".xml"), xml.getBytes(StandardCharsets.UTF_8));
        }
        getLogger().lifecycle("lifabt: generated " + getIcons().get().size() + " icon(s) (lucide-static@" + version + ")");
    }

    private void download(String icon, String version, Path target) throws IOException {
        String url = "https://cdn.jsdelivr.net/npm/lucide-static@" + version + "/icons/" + icon + ".svg";
        getLogger().lifecycle("lifabt: downloading " + icon);
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15_000);
        c.setReadTimeout(30_000);
        try {
            int code = c.getResponseCode();
            if (code == 404) {
                throw new GradleException("Icon '" + icon + "' not found in lucide-static@" + version
                        + ". Check the name on https://lucide.dev/icons");
            }
            if (code != 200) {
                throw new GradleException("Download of '" + icon + "' failed: HTTP " + code + " (" + url + ")");
            }
            Path tmp = Files.createTempFile(target.getParent(), icon, ".part");
            try (InputStream in = c.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new GradleException("Could not download '" + icon + "' and no cached copy exists at " + target
                    + ". Connect to the internet once, or copy the SVG there manually.", e);
        } finally {
            c.disconnect();
        }
    }
}
