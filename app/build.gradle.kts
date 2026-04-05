import java.io.File
import java.nio.file.Files
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class CheckNoHardcodedUrlsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoot: DirectoryProperty

    @get:Input
    abstract val allowedFiles: ListProperty<String>

    @TaskAction
    fun check() {
        val root = sourceRoot.asFile.get()
        if (!root.exists()) return

        val allowed = allowedFiles.get().toSet()
        val urlRegex = Regex("""https?://[^\s"']+""")
        val violations = mutableListOf<String>()

        Files.walk(root.toPath()).use { paths ->
            paths.forEach { path ->
                val file = path.toFile()
                if (!file.isFile) return@forEach
                if (!(file.name.endsWith(".kt") || file.name.endsWith(".java"))) return@forEach
                if (allowed.contains(file.name)) return@forEach

                val text = file.readText()
                if (urlRegex.containsMatchIn(text)) {
                    violations += root.toPath().relativize(path).toString()
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("URL absoluta hardcoded detectada fora de ApiConfig:")
                    violations.forEach { appendLine("- $it") }
                }
            )
        }
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

fun extractJsonField(content: String, field: String): String? =
    Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        .find(content)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }

fun readGoogleClientId(file: File): String {
    val content = file.readText()
    return extractJsonField(content, "client_id")
        ?: throw GradleException("Arquivo ${file.path} encontrado, mas sem client_id valido.")
}

fun readGoogleProjectId(file: File): String? = extractJsonField(file.readText(), "project_id")

val secretsRoot = rootProject.file("../../Secrets-client-OAuth-google")
val backendSecretsDir = File(secretsRoot, "backend")
val backendSecretFile: File? = backendSecretsDir
    .listFiles()
    ?.filter { it.isFile && it.name.startsWith("client_secret_") && it.extension == "json" }
    ?.sortedBy { it.name }
    ?.firstOrNull()

val appSecretsDir = File(secretsRoot, "App")
val appSecretFileLegacy = File(appSecretsDir, "client_secret_app2.json")
val appSecretFile: File? = when {
    appSecretFileLegacy.exists() -> appSecretFileLegacy
    else -> appSecretsDir
        .listFiles()
        ?.filter { it.isFile && it.name.startsWith("client_secret_") && it.extension == "json" }
        ?.sortedBy { it.name }
        ?.firstOrNull()
}

val backendWebClientIdFromFile: String? = backendSecretFile?.let(::readGoogleClientId)
val backendProjectIdFromFile: String? = backendSecretFile?.let(::readGoogleProjectId)
val appProjectIdFromFile: String? = appSecretFile?.let(::readGoogleProjectId)

if (
    backendProjectIdFromFile != null &&
    appProjectIdFromFile != null &&
    backendProjectIdFromFile != appProjectIdFromFile
) {
    logger.warn(
        "OAuth Google inconsistente: client do app esta no projeto '{}' e backend no projeto '{}'. " +
            "Isso costuma causar erro 10 no Google Login quando pacote/SHA-1 nao estao no mesmo projeto OAuth.",
        appProjectIdFromFile,
        backendProjectIdFromFile
    )
}

val googleWebClientId: String = backendWebClientIdFromFile
    ?: providers.gradleProperty("googleWebClientId").orNull
    ?: "201818722010-8fqjl9673qn5hn3g4c1gs52cj0ftbuh6.apps.googleusercontent.com"
val apiBaseUrl: String = providers.gradleProperty("apiBaseUrl").orNull ?: "http://10.0.2.2:8080"

android {
    namespace = "com.tcc.androidnative"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tcc.androidnative"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val checkNoHardcodedUrls by tasks.registering(CheckNoHardcodedUrlsTask::class) {
    group = "verification"
    description = "Fails if URL absoluta estiver hardcoded fora de ApiConfig."
    sourceRoot.set(layout.projectDirectory.dir("src/main/java"))
    allowedFiles.set(listOf("ApiConfig.kt"))
}

tasks.named("preBuild") {
    dependsOn(checkNoHardcodedUrls)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}



