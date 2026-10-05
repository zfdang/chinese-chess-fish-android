# Pikafish source snapshot

Official release: [Pikafish-2026-09-06](https://github.com/official-pikafish/Pikafish/releases/tag/Pikafish-2026-09-06).
Commit: `4c17cee11f888ae1d48a9494f2e2239f019f0a1f`.
`src/` is the upstream source snapshot with two small adaptations: the release version string in `misc.cpp` is set to `2026-09-06` (rather than a build-date-dependent dev label), and `ucioption.cpp` uses the current OptionsMap size for insertion order instead of a process-global counter. The latter preserves UCI option reporting when JNI constructs more than one engine in a service process. Android integration lives in `jni_bridge.cpp` and `Android.mk`; the upstream executable entry point and universal executable launcher are excluded from the shared-library build.

The NNUE is from that exact release archive, not the rolling master network.
Compressed asset SHA-256: `7d13d73569a9b571ba0eb20cf1596247bc2a42738967e61afef6482b231e900e`.
Decompressed SHA-256: `e0af7813ce304337a565947bd91588ffa4a7c560a26575544ca895be9fa24fd6`.
Keep source, network, license and hashes together when updating.
