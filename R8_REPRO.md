# R8 9.5.x narrows a Compose restart lambda's changed-flags field to `byte`

The minimal app is the `:r8-repro` module in this branch. The rest of nowinandroid is not
involved; the only change outside the module is the version bump below.

## Summary

In an optimized build (minified, **not debuggable**), R8 9.5.29 (bundled with AGP 9.5.0, 9.5.1
and 9.5.2) and R8 9.5.33 store one of the `$changed` ints captured by a Compose restart lambda in a
**`byte`** field (`iput-byte` / `iget-byte`). The value really uses up to 31 bits. On restart
recomposition the flags are truncated to 8 bits and sign-extended, the composable makes a different
sequence of `Composer.changed()` calls than on the normal path, and the slot table shifts by one
slot. The app then crashes:

```
java.lang.ClassCastException: com.example.r8repro.BannerActionKt$$ExternalSyntheticLambda0
    cannot be cast to com.example.r8repro.ClickGuardImpl
```

R8 9.4.24 (AGP 9.4.1) keeps the field as `int` and the app works.

## Versions

| | |
|---|---|
| AGP | 9.5.1 (R8 9.5.29); for comparison 9.4.1 (R8 9.4.24) |
| R8 override checked | 9.5.33 (still reproduces) |
| Kotlin + Compose compiler plugin | 2.4.21 |
| Compose BOM | 2026.09.00 (runtime 1.12.1) |
| Gradle | 9.8.1 |
| Build type | `release`: `isMinifyEnabled = true`, `isDebuggable = false`, R8 full mode, signed with the debug key |
| Device | Emulator, API 30 (Medium Phone AVD) |

A debuggable minified build does not reproduce, because R8 skips most optimizations there.

## Build and run

```sh
./gradlew :r8-repro:assembleRelease
adb install -r r8-repro/build/outputs/apk/release/r8-repro-release.apk
adb shell am start -n com.example.r8repro/.MainActivity
```

To compare the R8 versions:

- **AGP 9.4.1 (R8 9.4.24):** in `gradle/libs.versions.toml`, set `androidGradlePlugin = "9.4.1"`
  and `androidTools = "32.4.1"`.
- **Another R8 with AGP 9.5.1:** pass `-Dr8repro.r8=<version>`, e.g.
  `./gradlew :r8-repro:assembleRelease -Dr8repro.r8=9.5.33`. See the `buildscript` block in the
  root `build.gradle.kts`.

`r8-repro/proguard-rules.pro` only contains `-dontobfuscate`, so the dex is readable. The byte
field also appears with obfuscation on.

## Steps to reproduce

The screen has two buttons:

- **Recompose parent** changes state that only `FeedScreen` reads.
- **Restart child** changes state that only `FeedContent` reads.

1. Tap **Recompose parent**.
2. Tap **Restart child**.
3. Tap **Recompose parent**.

**Expected:** nothing visible happens. Logcat (`adb logcat -s R8Repro`) shows
`rememberBannerAction: keyed remember computed` only once, at first composition, because its keys
never change.

**Actual (R8 9.5.29 / 9.5.33):** the keyed `remember` recomputes at step 2 even though its keys
are unchanged. Step 3 crashes. Each test run was a fresh launch followed by parent, child,
parent, child, parent, child:

| R8 | Runs that crashed | Keyed `remember` computations per run |
|---|---|---|
| 9.5.29 (AGP 9.5.1) | 3 of 3, at step 3 | 3 |
| 9.5.33 (override) | 2 of 2, at step 3 | 3 |
| 9.4.24 (AGP 9.4.1) | 0 of 3 | 1 |

Retraced stack (9.5.29):

```
java.lang.ClassCastException: com.example.r8repro.BannerActionKt$$ExternalSyntheticLambda0 cannot be cast to com.example.r8repro.ClickGuardImpl
	at com.example.r8repro.FeedScreenKt.FeedScreen(FeedScreen.kt:36)
	at com.example.r8repro.ClickGuardKt.rememberClickGuard(ClickGuard.kt:36)
	at com.example.r8repro.BannerActionKt.rememberBannerAction(BannerAction.kt:33)
	at com.example.r8repro.FeedContentKt.FeedContent(FeedContent.kt:70)
	at com.example.r8repro.FeedScreenKt.FeedScreen(FeedScreen.kt:79)
	at com.example.r8repro.FeedScreenKt.FeedScreen$lambda$5(FeedScreen.kt)
	at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:204)
	at androidx.compose.runtime.GapComposer.recomposeToGroupEnd(GapComposer.kt:1678)
	...
```

`ClickGuard.kt:36` is the unkeyed `remember { ClickGuard.create() }`. It reads the slot that holds
the lambda stored by the keyed `remember(markAsShown, onOpenLink, title)` just before it.

## Shape of the code

- **`FeedContent`** (`FeedContent.kt`) has 26 parameters, so the Compose compiler passes
  `$changed`, `$changed1`, `$changed2` and `$default`. Parameters 10–19 are encoded in
  `$changed1`, 3 bits each starting at bit 1. Parameter 12 occupies bits 7–9. `onOpenLink` is
  parameter 16, at bits 19–21.
- **`FeedScreen`** (`FeedScreen.kt`) is a restartable parent with 25 parameters. It forwards them
  all, so the flags it passes are computed from its own dirty ints at runtime, not folded into
  constants. When it recomposes it passes "Same" for every unchanged argument. It also passes a
  new `Modifier`, so `FeedContent` runs instead of skipping, and its restart lambda is replaced by
  one that captures these "Same" flags.
- **`FeedRoute` / `FeedCallbacks`** (`MainActivity.kt`) supply the callbacks as properties of an
  object created outside composition. This keeps the flags non-constant.
- **`rememberBannerAction`** (`BannerAction.kt`) is `remember(markAsShown, onOpenLink, title) {
  … }.rememberClickGuard()`. `rememberClickGuard` (`ClickGuard.kt`) is
  `remember { ClickGuard.create() }`. R8 inlines both into `FeedContent`.

Mechanism with R8 9.5.29:

1. The restart lambda keeps the captured `$changed1` in a `byte` field.
2. After step 1, `$changed1` has "Same" (`0b001`) for parameter 12, which sets bit 7. On restart
   (step 2), `iget-byte` sign-extends the value, so bits 8–31 all become 1. `onOpenLink`'s 3 bits
   become `0b111`.
3. The inlined keyed `remember` decides whether to call `changed(onOpenLink)` from those bits:
   `((flags & 0x380) ^ 0x180) > 0x100`. The call is skipped for Same, Different or Static, and
   made for `0b111`. So on the restart path it makes one extra `changed()` call and consumes one
   extra slot. The keyed `remember` recomputes, and the slots after it are written one position
   off.
4. At step 3 the normal path, with correct flags, reads that layout back. The unkeyed `remember`
   then gets the lambda, and the cast fails.

R8 9.5.29 also assumes the high bits of `$changed1` are zero in the callee. In `FeedContent` it
keeps the `($changed1 & mask) == 0` test only for the first parameter slot. It calls
`changed()` / `changedInstance()` unconditionally for the other nine. R8 9.4.24 keeps all ten
tests. In the 9.5.29 build, `$changed2` is gone from the signature entirely: `FeedContent` takes
two ints instead of three. Both look like the same wrong assumption about the value the caller
computes.

## Dex evidence

Restart lambda `com.example.r8repro.FeedContentKt$$ExternalSyntheticLambda4`, flag fields only
(dexdump `-d`).

**R8 9.5.29 (AGP 9.5.1):**

```
<init>
|004b: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$26:I
|004f: iput-byte v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$27:B
invoke
|000d: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$26:I
|000f: or-int/lit8 v1, v1, #int 1 // #01
|0011: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
|0015: iget-byte v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$27:B
|0017: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
```

**R8 9.4.24 (AGP 9.4.1):**

```
<init>
|004b: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$26:I
|004f: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$27:I
|0053: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$28:I
invoke
|000d: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$26:I
|000f: or-int/lit8 v1, v1, #int 1 // #01
|0011: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
|0015: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$27:I
|0017: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
|001b: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda4;.f$28:I
|001d: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
```

The value stored in `f$27` is not byte-sized. With R8 9.5.29, `FeedScreen` computes it like this
before calling `FeedContent`. It is the last int argument, `v27`, with masks up to `0x70000000`:

```
|026e: shr-int/lit8 v17, v17, #int 27 // #1b
|0270: and-int/lit8 v17, v17, #int 14 // #0e
|0272: shl-int/lit8 v0, v23, #int 3 // #03
|0274: and-int/lit8 v1, v0, #int 112 // #70
|0276: or-int v17, v17, v1
|0278: and-int/lit16 v1, v0, #int 896 // #0380
|027a: or-int v17, v17, v1
|027c: and-int/lit16 v1, v0, #int 7168 // #1c00
|027e: or-int v17, v17, v1
|0280: const v1, #float 8.03561e-41 // #0000e000
|0283: and-int/2addr v1, v0
|0284: or-int v17, v17, v1
|0286: const/high16 v1, #int 458752 // #7
|0288: and-int/2addr v0, v1
|0289: or-int v17, v17, v0
|028b: shl-int/lit8 v0, v20, #int 6 // #06
|028d: and-int/2addr v0, v2
|028e: or-int v17, v17, v0
|0290: shl-int/lit8 v23, v23, #int 6 // #06
|0292: and-int v3, v23, v3
|0294: or-int v17, v17, v3
|0296: and-int v5, v23, v5
|0298: or-int v17, v17, v5
|029a: and-int v23, v23, v6        // v6 = 0x70000000
|029c: or-int v27, v17, v23
```

`FeedContent`'s handling of `$changed1`, the first two parameter slots. With 9.5.29 the second
slot's mask test is gone and `changed()` is unconditional:

```
// R8 9.5.29                                   // R8 9.4.24
|00f4: and-int/lit8 v29, v64, #int 6          |011d: and-int/lit8 v17, v14, #int 6
|00f6: if-nez v29, 010a                       |0121: if-nez v17, 0131
|00fa: invoke-virtual ... changedInstance     |0123: invoke-virtual ... changedInstance
...                                           ...
|010d: invoke-virtual ... changed  (no test)  |0133: and-int/lit8 v31, v14, #int 48
                                              |0135: if-nez v31, 014d
                                              |013b: invoke-virtual ... changed
```

R8 version markers in the two APKs' dex: `"version":"9.5.29"` and `"version":"9.4.24"`.

## Notes

- AGP 9.5.2 bundles the same R8 9.5.29 and also produces the `byte` field.
- In the app this was found in, the trigger was rotating the screen twice in an Activity that
  handles `configChanges` itself. The first rotation recomposed the parent; the second restarted
  the child alone. The two buttons here replace that sequence with a deterministic one.
