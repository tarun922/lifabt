# lifabt

**lifabt** = Lucide icons for Android, by Tarun.

Use [Lucide](https://lucide.dev) icons in native Android apps. List the icons you want in `build.gradle`. On the first build the plugin downloads them, converts them to Android VectorDrawables, and compiles them into your app.

## Setup

1. Publish the plugin locally once:

   ```
   ./gradlew publishToMavenLocal
   ```

2. In your app's `settings.gradle`:

   ```groovy
   pluginManagement {
       repositories {
           mavenLocal()
           google()
           mavenCentral()
           gradlePluginPortal()
       }
   }
   ```

3. In your app module's `build.gradle`:

   ```groovy
   plugins {
       id 'com.android.application'
       id 'io.github.tarun922.lifabt' version '0.1.0'
   }

   lifabt {
       icons = ['camera', 'arrow-right', 'circle-user', 'settings']
       // version = '0.460.0'   // optional: pin a lucide-static version (default "latest")
       // prefix  = 'lucide_'   // optional
   }
   ```

Icon names are the ones shown on https://lucide.dev/icons.

## Use the icons

`arrow-right` becomes `lucide_arrow_right`.

XML:

```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/lucide_camera"
    app:tint="?attr/colorOnSurface" />
```

Java:

```java
imageView.setImageResource(R.drawable.lucide_camera);
```

Compose:

```kotlin
Icon(painterResource(R.drawable.lucide_camera), contentDescription = "Camera")
```

Icons are black by default. Set a tint to recolor them.

## How it works

- First build: each SVG is downloaded from the `lucide-static` npm package (via jsDelivr) and saved in `lucide-icons/<version>/` next to your `build.gradle`.
- Every build after that uses the saved SVGs, so nothing is downloaded again and it works offline. Commit `lucide-icons/` to git so CI and teammates don't need to download either.
- The SVGs are converted to VectorDrawables in `build/generated/lucide/res`, which is added to your app's resources automatically.
- Run `./gradlew generateLucideIcons` to regenerate manually.

## Notes

- If the res folder isn't picked up automatically, add `sourceSets.main.res.srcDir("build/generated/lucide/res")` to your `android {}` block.
- Pin `version` if you want identical icons across machines. With `latest`, the cached copy is what keeps builds stable.
- Supported SVG elements: path, line, polyline, polygon, circle, ellipse, rect, g. These cover all Lucide icons.
