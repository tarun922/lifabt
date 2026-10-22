package io.github.tarun922.lifabt;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/** The `lifabt { ... }` block in the user's build.gradle. */
public abstract class LifabtExtension {

    /** Icon names exactly as on lucide.dev, e.g. "camera", "arrow-right". */
    public abstract ListProperty<String> getIcons();

    /** lucide-static version on npm. Pin it (e.g. "0.460.0") for reproducible builds. */
    public abstract Property<String> getVersion();

    /** Prefix for generated drawable names. "camera" becomes R.drawable.lucide_camera. */
    public abstract Property<String> getPrefix();
}
