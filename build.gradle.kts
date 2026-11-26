/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// r8-repro (see R8_REPRO.md): `-Dr8repro.r8=<version>` replaces the R8 bundled with AGP,
// e.g. -Dr8repro.r8=9.5.33. Without the property the bundled R8 is used.
buildscript {
    val r8Override: String? = System.getProperty("r8repro.r8")
    if (r8Override != null) {
        repositories {
            maven("https://storage.googleapis.com/r8-releases/raw")
        }
        dependencies {
            classpath("com.android.tools:r8:$r8Override")
        }
    }
}

/*
 * By listing all the plugins used throughout all subprojects in the root project build script, it
 * ensures that the build script classpath remains the same for all projects. This avoids potential
 * problems with mismatching versions of transitive plugin dependencies. A subproject that applies
 * an unlisted plugin will have that plugin and its dependencies _appended_ to the classpath, not
 * replacing pre-existing dependencies.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dependencyGuard) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.google.osslicenses) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.nowinandroid.root)
}
