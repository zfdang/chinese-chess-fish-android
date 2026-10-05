# Android Pikafish integration

The bundled engine and NNUE are pinned to the official Pikafish-2026-09-06 release. Source provenance, hashes and licenses are under `app/src/main/cpp/pikafish/UPSTREAM.md` and `app/src/main/assets/pikafish/NNUE-License.md`.

## Runtime

`PikafishNativeEngine` carries UCI commands over Binder to a non-exported bound `PikafishService` in the `:pikafish` process. The service loads `libpikafish.so` (ARMv8) or `libpikafish_dotprod.so` (ARMv8.2 dot product) with JNI. It does not execute a binary or download native code.

The service serializes sessions because upstream uses global UCI streams. Shutdown cancels queued commands and sends `quit`; upstream destroys the engine and its search threads before another session starts. An upstream fatal error terminates only the service. The UI reports the failure and a later search can create a fresh session.

The release network is an APK asset. On first use the service worker copies it atomically to a versioned private directory, checks its size and SHA-256, and uses that exact file for EvalFile. Existing valid-length copies are reused. UCI configuration is kept in a writable private `pikafish.ini`; EvalFile is always redirected to the bundled release network.

## Build and Play artifacts

Use JDK 17, Android SDK 36, NDK 27.0.12077973 and the Gradle wrapper. The app targets API 36 and retains minSdk 26. NDK flexible page sizes plus explicit 16 KB ELF alignment and AGP 8.10.1 uncompressed native packaging cover the native page-size requirements.

```sh
./gradlew assembleRelease bundleRelease
./gradlew 'testArmv8-DebugUnitTest' 'connectedArmv8-DebugAndroidTest'
```

Release AABs are in `app/build/outputs/bundle/armv8-Release/` and `armv8-dotprod-Release/`. Use the general ARMv8 AAB for Play distribution across supported ARM64 devices; the dot-product flavor requires a CPU supporting that instruction set. CI attaches both APK ZIPs and AABs to releases only after merging a PR into master.

Target API and native packaging address the technical migration. Actual Play acceptance is established by uploading the AAB to Play Console, which also checks listing and policy declarations.

## Updating again

Choose a released upstream tag, copy its source snapshot and matching NNUE from the same release archive, retain the licenses, update the release label in `src/misc.cpp`, and update `EngineAssets.VERSION`, size and SHA-256. Do not mix the rolling master network with a released engine. Run both flavors on hardware, including UCI handshake, MultiPV, evaluation, stop/restart, and invalid-FEN service isolation tests. Check every packaged ELF LOAD alignment and the release APK 16 KB ZIP alignment.
