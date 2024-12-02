import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// https://discuss.kotlinlang.org/t/use-git-hash-as-version-number-in-build-gradle-kts/19818/2
// this function will return the git version number, and will be used as versionCode
fun gitVersion(): Int {
    val os = org.apache.commons.io.output.ByteArrayOutputStream()
    project.exec {
        commandLine = "git rev-list HEAD --count".split(" ")
        standardOutput = os
    }
    return String(os.toByteArray()).trim().toInt()
}

android {
    namespace = "com.zfdang.chess"
    compileSdk = 34

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.zfdang.chess"
        minSdk = 26
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28
        versionCode = gitVersion()
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    flavorDimensions += "pikafish"
    productFlavors {
        create("armv8-") {
            //flavor configurations here
            dimension = "pikafish"
            buildConfigField("String", "PIKAFISH_ENGINE_FILE", "\"pikafish-armv8\"")
        }
        create("armv8-dotprod-") {
            //flavor configurations here
            dimension = "pikafish"
            buildConfigField("String", "PIKAFISH_ENGINE_FILE", "\"pikafish-armv8-dotprod\"")
        }
    }

    // https://gist.github.com/mileskrell/7074c10cb3298a2c9d75e733be7061c2
    // Example of declaring Android signing configs using Gradle Kotlin DSL
    signingConfigs {
        create("release") {
            storeFile = file("cchess.release.jks")
            keyAlias = "cchess"
            storePassword = "cchess-store-pwd"
            keyPassword = "cchess-key-pwd"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs["release"]
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    ndkVersion = "25.1.8937393"

    // https://gist.github.com/pankajXdev/574063901ada2fafa329068f41ddb076
    // Config your output file name in Gradle Kotlin DSL
    applicationVariants.all {
        outputs.all { output ->
            if (output is BaseVariantOutputImpl) {
                val date = SimpleDateFormat("yyyyMMdd").format(Date())
                val filename = "ChessFish_${date}_${versionCode}_${name}.apk"
                output.outputFileName = filename
            }
            true
        }
    }
}

dependencies {
    implementation("com.readystatesoftware.sqliteasset:sqliteassethelper:+")
    // https://mvnrepository.com/artifact/com.igormaznitsa/jbbp
    implementation("com.igormaznitsa:jbbp:3.0.0")
    // https://github.com/PhilJay/MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(project(":filepicker"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.junit.jupiter)
}
