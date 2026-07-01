# xmake-gradle (Kotlin)

A Kotlin implementation of the XMake Gradle plugin. It configures, builds, rebuilds, installs, and cleans Android
native libraries produced by [XMake](https://xmake.io).

## Requirements

- XMake 2.9.6 or newer on `PATH`, or an explicit `program` path
- An Android NDK, supplied explicitly or through the Android Gradle Plugin
- A Gradle project containing an `xmake.lua` file

## Apply the plugin

```kotlin
plugins {
    id("com.ifarbod.xmake-gradle-plugin") version "0.0.1"
}
```

When developing this repository, the plugin is provided by the included build in `plugin-build`.

## Configure

```kotlin
xmake {
    path = "jni/xmake.lua"
    program = "xmake"              // optional
    ndk = "/path/to/android-ndk"   // optional
    sdkver = 24                     // optional
    stl = "c++_shared"             // optional
    stdcxx = true                   // optional
    buildDir = "build/xmake"       // optional
    buildMode = "release"          // optional
    logLevel = "verbose"           // normal, verbose, or debug

    arguments("--toolchain=clang")
    cFlags("-DANDROID")
    cppFlags("-std=c++20")
    abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    targets("native")
}
```

If `abiFilters` is omitted, the plugin reads `android.defaultConfig.ndk.abiFilters` when available, then falls back
to `armeabi-v7a`. Relative paths are resolved from the Gradle project directory.

## Tasks

The plugin creates aggregate tasks and architecture-specific variants:

- `xmakeConfigureFor<Arch>`
- `xmakeBuild` and `xmakeBuildFor<Arch>`
- `xmakeRebuild` and `xmakeRebuildFor<Arch>`
- `xmakeInstall` and `xmakeInstallFor<Arch>`
- `xmakeClean` and `xmakeCleanFor<Arch>`

Supported architecture suffixes are `Arm64`, `Armv7`, `Arm`, `RiscV64`, `X64`, and `X86`. `xmakeInstall` is wired
before Android's `preBuild`, and `xmakeClean` is wired before `clean`. Installed shared libraries are placed under
`src/main/jniLibs/<abi>`.

The tasks are registered only when the configured `xmake.lua` exists.

## Verify this repository

```shell
./gradlew preMerge
```

The original Groovy implementation used for this port is retained in `old_groovy` for comparison.

## License

The integrated XMake plugin implementation is derived from
[xmake-io/xmake-gradle](https://github.com/xmake-io/xmake-gradle) and retains its Apache-2.0 licensing and attribution.
The surrounding Kotlin Gradle plugin template retains its existing MIT license.
