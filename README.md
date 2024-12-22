# Actinis Remote Keyboard

![Maven Central Version](https://img.shields.io/maven-central/v/io.actinis.remote/keyboard)

A custom keyboard view library designed for internal use within the [Actinis Remote](https://actinis.io) project.

Built using **Compose Multiplatform**.

## Features in Progress

The following features are planned for future development:

- Layout variants (e.g., email, URL)
- Autocomplete support
- Theming support
- iOS and macOS support

## Important Note

This project is an **early-stage MVP** and represents a small, non-critical component of a much larger MVP initiative.

As such, the current codebase prioritizes rapid prototyping over production-level quality. For now, you should not
expect:

- Perfect or polished architecture
- Full adherence to SOLID principles or a clean architecture
- Comprehensive documentation
- RTL support
- Unit or integration tests
- Full portability (e.g., tablet support)
- R8 rules

Contributions and feedback are welcome, but please keep the project's early-stage nature in mind.

## Usage

### Your app does not use Koin

Add to your `AndroidManifest.xml`:

```xml

<provider android:authorities="${applicationId}.androidx-startup" android:exported="false"
    android:name="androidx.startup.InitializationProvider" tools:node="merge">
    <meta-data android:name="io.actinis.remote.keyboard.domain.initialization.AndroidLibraryInitializer"
        android:value="androidx.startup" />
</provider>
```

### Your app uses Koin

Add keyboard modules to your modules:

```kotlin
val koinApp = startKoin {
    androidContext(context)
    modules(*keyboardModules)
}
koinApp.initializeKeyboard()
```

## License

This project is licensed under the **Apache License 2.0**. You may use, modify, and distribute the code, provided you
comply with the terms of the license. See the [LICENSE](./LICENSE) file for more details.
