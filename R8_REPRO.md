# R8 9.5.x narrows a Compose restart lambda's changed-flags field to `byte`

The minimal app is the `:r8-repro` module in this branch. Outside the module, this branch only
bumps the toolchain versions and adds an optional R8 override to the root `build.gradle.kts`.

## Summary

In an optimized build (minified, **not debuggable**), R8 9.5 stores one of the `$changed` ints
captured by a Compose restart lambda in a **`byte`** field (`iput-byte` / `iget-byte`). This
happens with R8 9.5.28 (AGP 9.5.0), 9.5.29 (AGP 9.5.1 and 9.5.2) and 9.5.33. The int really uses
up to 31 bits.

On restart recomposition the flags are truncated to 8 bits and sign-extended. The composable then
makes a different sequence of `Composer.changed()` calls than on the normal path, and the slot
table shifts by one slot. The app crashes:

```
java.lang.ClassCastException: com.example.r8repro.BannerActionKt$$ExternalSyntheticLambda1
    cannot be cast to com.example.r8repro.TapCounterImpl
```

R8 9.4.24 (AGP 9.4.1) keeps the field as `int`, and the app works.

## Versions

| Item | Version |
|---|---|
| AGP | 9.5.1 (R8 9.5.29); for comparison 9.4.1 (R8 9.4.24) |
| R8 override checked | 9.5.33 (still reproduces) |
| Kotlin + Compose compiler plugin | 2.4.21 |
| Compose BOM | 2026.09.00 (runtime 1.12.1) |
| Gradle | 9.8.1 |
| Build type | `release`: `isMinifyEnabled = true`, `isDebuggable = false`, R8 full mode, signed with the debug key. The dex marker reads `"compilation-mode":"release"`. |
| Device | Emulator, API 30 (Medium Phone AVD) |

A minified build with `isDebuggable = true` does not reproduce. Its dex marker reads
`"compilation-mode":"debug"`, and the restart lambda keeps all four ints (`$changed`, `$changed1`,
`$changed2`, `$default`) as `int`.

## Build and run

```sh
./gradlew :r8-repro:assembleRelease
adb install -r r8-repro/build/outputs/apk/release/r8-repro-release.apk
adb shell am start -n com.example.r8repro/.MainActivity
```

To compare R8 versions:

- **AGP 9.4.1 (R8 9.4.24):** set `androidGradlePlugin = "9.4.1"` and `androidTools = "32.4.1"` in
  `gradle/libs.versions.toml`.
- **Another R8 with AGP 9.5.1:** run with `-Dr8repro.r8=<version>`, e.g.
  `./gradlew :r8-repro:assembleRelease -Dr8repro.r8=9.5.33`. The override is in the `buildscript`
  block of the root `build.gradle.kts`.

`r8-repro/proguard-rules.pro` only contains `-dontobfuscate`, to keep the dex readable. The `byte`
field also appears with obfuscation on.

## Steps to reproduce

The screen has two buttons. **Recompose parent** changes state that only `FeedScreen` reads.
**Restart child** changes state that only `FeedContent` reads.

1. Tap **Recompose parent**.
2. Tap **Restart child**.
3. Tap **Recompose parent**.

**Expected:** nothing visible happens. Logcat (`adb logcat -s R8Repro`) shows
`rememberBannerAction: keyed remember computed` only once, at first composition, because its keys
never change.

**Actual (R8 9.5.29 / 9.5.33):** at step 2 the keyed `remember` is computed again although its keys
are unchanged. Step 3 crashes.

Each test run was a fresh launch, then parent, child, parent, child, parent, child:

| R8 | Runs that crashed | Keyed `remember` computations per run |
|---|---|---|
| 9.5.29 (AGP 9.5.1) | 3 of 3, at step 3 | 3 |
| 9.5.33 (override) | 3 of 3, at step 3 | 3 |
| 9.4.24 (AGP 9.4.1) | 0 of 3 | 1 |

Retraced stack (R8 9.5.29):

```
java.lang.ClassCastException: com.example.r8repro.BannerActionKt$$ExternalSyntheticLambda1 cannot be cast to com.example.r8repro.TapCounterImpl
	at com.example.r8repro.FeedScreenKt.FeedScreen(FeedScreen.kt:32)
	at com.example.r8repro.TapCounterKt.rememberTapCounter(TapCounter.kt:32)
	at com.example.r8repro.BannerActionKt.rememberBannerAction(BannerAction.kt:38)
	at com.example.r8repro.FeedContentKt.FeedContent(FeedContent.kt:68)
	at com.example.r8repro.FeedScreenKt.FeedScreen(FeedScreen.kt:62)
	at com.example.r8repro.FeedScreenKt.FeedScreen$lambda$1(FeedScreen.kt)
	at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:204)
	...
```

`TapCounter.kt:32` is the unkeyed `remember { TapCounter.create() }`. It reads the slot that holds
the lambda stored by the keyed `remember(recordOpen, onOpenLink, title)` just before it.

## Shape of the code

Parameter numbers below are zero-based.

- **`FeedContent`** (`FeedContent.kt`) has 26 parameters, so the Compose compiler passes
  `$changed`, `$changed1`, `$changed2` and `$default`. `$changed1` encodes parameters 10–19 in 3
  bits each, starting at bit 1. Parameter 12 occupies bits 7–9, and `onOpenLink` (parameter 16)
  occupies bits 19–21.
- **`FeedScreen`** (`FeedScreen.kt`) is a restartable parent that forwards 25 parameters. The flags
  it passes are therefore computed from its own dirty ints at runtime. When it recomposes, it passes
  "Same" for every unchanged argument. It also passes a new `Modifier`, so `FeedContent` runs
  instead of skipping, and its restart lambda is replaced by one that captures those "Same" flags.
- **`FeedRoute` / `FeedCallbacks`** (`MainActivity.kt`) supply the callbacks as properties of an
  object created outside composition. With compile-time-static lambdas instead, R8 folds the flags
  into constants and the `byte` field does not appear.
- **`rememberBannerAction`** (`BannerAction.kt`) calls `remember(recordOpen, onOpenLink, title) {
  … }.rememberTapCounter()`, and `rememberTapCounter` (`TapCounter.kt`) calls
  `remember { TapCounter.create() }`. R8 inlines both into `FeedContent`.

Mechanism with R8 9.5.29:

1. The restart lambda keeps the captured `$changed1` in a `byte` field.
2. After step 1, `$changed1` carries "Same" (`0b001`) for parameter 12, so bit 7 is set.
3. On restart (step 2), `iget-byte` sign-extends the value, so bits 8–31 all become 1 and
   `onOpenLink`'s 3 bits become `0b111`.
4. The inlined keyed `remember` calls `changed(onOpenLink)` only when
   `((flags & 0x380) ^ 0x180) > 0x100`. That is false for Same, Different and Static, and true for
   `0b111`. So the restart path makes one extra `changed()` call and consumes one extra slot. The
   keyed `remember` recomputes, and the slots after it are written one position off.
5. At step 3 the normal path, with correct flags, reads that layout back. The unkeyed `remember`
   gets the lambda, and the cast fails.

On the callee side, R8 9.5.29 also seems to assume that the high bits of `$changed1` are zero:

- `FeedContent` keeps the `($changed1 & mask) == 0` test only for the first parameter slot.
- It calls `changed()` / `changedInstance()` unconditionally for the other nine slots.
- `$changed2` disappears from the signature entirely: `FeedContent` takes two ints
  (`…GapComposer;II)V`) instead of three (`…GapComposer;III)V` with R8 9.4.24).

R8 9.4.24 keeps all ten tests.

## Dex evidence

Restart lambda `com.example.r8repro.FeedContentKt$$ExternalSyntheticLambda3`, flag fields only
(`dexdump -d`).

**R8 9.5.29 (AGP 9.5.1):**

```
<init>
|004b: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$26:I
|004f: iput-byte v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$27:B
invoke
|000d: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$26:I
|000f: or-int/lit8 v1, v1, #int 1 // #01
|0011: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
|0015: iget-byte v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$27:B
|0017: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
```

**R8 9.4.24 (AGP 9.4.1):**

```
<init>
|004b: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$26:I
|004f: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$27:I
|0053: iput v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$28:I
invoke
|000d: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$26:I
|000f: or-int/lit8 v1, v1, #int 1 // #01
|0011: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
|0015: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$27:I
|0017: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
|001b: iget v1, v0, FeedContentKt$$ExternalSyntheticLambda3;.f$28:I
|001d: invoke-static {v1}, Landroidx/compose/runtime/Updater;.updateChangedFlags:(I)I
```

The value stored in `f$27` does not fit in a byte. With R8 9.5.29, `FeedScreen` computes it as
follows before calling `FeedContent`. It is the last int argument, `v27`, with masks up to
`0x70000000`:

```
|021a: const/high16 v7, #int 1879048192 // #7000
|021f: shr-int/lit8 v17, v17, #int 27 // #1b
|0221: and-int/lit8 v17, v17, #int 14 // #0e
|0223: shl-int/lit8 v1, v23, #int 3 // #03
|0225: and-int/lit8 v2, v1, #int 112 // #70
|0227: or-int v17, v17, v2
|0229: and-int/lit16 v2, v1, #int 896 // #0380
|022b: or-int v17, v17, v2
|022d: and-int/lit16 v2, v1, #int 7168 // #1c00
|022f: or-int v17, v17, v2
|0231: const v2, #float 8.03561e-41 // #0000e000
|0234: and-int/2addr v2, v1
|0235: or-int v17, v17, v2
|0237: const/high16 v2, #int 458752 // #7
|0239: and-int/2addr v1, v2
|023a: or-int v17, v17, v1
|023c: shl-int/lit8 v1, v20, #int 6 // #06
|023e: and-int/2addr v1, v3
|023f: or-int v17, v17, v1
|0241: shl-int/lit8 v23, v23, #int 6 // #06
|0243: and-int v5, v23, v5
|0245: or-int v17, v17, v5
|0247: and-int v6, v23, v6
|0249: or-int v17, v17, v6
|024b: and-int v23, v23, v7
|024d: or-int v27, v17, v23
```

`FeedContent`'s handling of the first two slots of `$changed1` (`v67` with 9.5.29, `v6` with
9.4.24). With 9.5.29 the second slot's mask test is gone, and `changed()` is called
unconditionally:

```
R8 9.5.29                                      R8 9.4.24
|00f4: and-int/lit8 v30, v67, #int 6           |011e: and-int/lit8 v16, v6, #int 6
|00f6: if-nez v30, 010a                        |0122: if-nez v16, 0132
|00fa: invoke-virtual ... changedInstance      |0124: invoke-virtual ... changedInstance
|0105: or-int v30, v67, v30                    |012f: or-int v16, v6, v16
...                                            |0134: and-int/lit8 v31, v6, #int 48
|010d: invoke-virtual ... changed              |0136: if-nez v31, 014e
       (no mask test before it)                |0138: and-int/lit8 v31, v6, #int 64
                                               |013c: invoke-virtual ... changed
```

R8 version markers in the APKs' dex: `"version":"9.5.29"`, `"version":"9.4.24"`,
`"version":"9.5.33"` (override) and `"version":"9.5.28"` (AGP 9.5.0).

## Notes

In the app where this was found, the trigger was rotating the screen twice in an Activity that
handles `configChanges` itself:

- The first rotation recomposed the parent.
- The second rotation restarted the child alone.

The two buttons here replace that with a deterministic sequence. Rotation was not tried with this
repro.
