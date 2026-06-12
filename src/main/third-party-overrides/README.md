# Third-party overrides

This source root is reserved for narrow compatibility patches to third-party libraries.

`com.luciad.imageio.webp.NativeLibraryUtils` intentionally keeps the original package name from `org.sejda.imageio:webp-imageio`. The library calls that package-private helper directly, so changing the Java package would stop the WebP native loader override from being used.

Application code must stay under `src/main/java/pt/isel/gape`.
