import io.gitlab.arturbosch.detekt.Detekt
import java.io.File
import java.nio.file.Files
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

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

abstract class CheckNoKotlinSuppressionsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val rootPath: Property<String>

    @TaskAction
    fun check() {
        val root = File(rootPath.get()).toPath()
        val annotationPattern = Regex("@(?:Sup" + "press|Sup" + "pressLint)\\s*\\(")
        val cpdPattern = Regex("CPD" + "-(?:OFF|ON)")
        val violations = mutableListOf<String>()

        sourceFiles.files.sortedBy { it.path }.forEach { file ->
            Files.readAllLines(file.toPath()).forEachIndexed { index, line ->
                if (annotationPattern.containsMatchIn(line) || cpdPattern.containsMatchIn(line)) {
                    violations += "${root.relativize(file.toPath())}:${index + 1}: ${line.trim()}"
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Kotlin suppression mechanisms are forbidden:")
                    violations.forEach(::appendLine)
                }
            )
        }
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

val cpdTool by configurations.creating

fun extractJsonField(content: String, field: String): String? = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
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
    else ->
        appSecretsDir
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

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
        disable += setOf("GradleDependency", "IconLauncherShape", "OldTargetApi")
        textReport = true
        htmlReport = true
        xmlReport = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

kapt {
    correctErrorTypes = true
}

detekt {
    toolVersion = "1.23.8"
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    ignoreFailures = false
    basePath = rootProject.projectDir.absolutePath
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        txt.required.set(true)
    }
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    outputToConsole.set(true)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
}

val checkNoHardcodedUrls by tasks.registering(CheckNoHardcodedUrlsTask::class) {
    group = "verification"
    description = "Fails if URL absoluta estiver hardcoded fora de ApiConfig."
    sourceRoot.set(layout.projectDirectory.dir("src/main/java"))
    allowedFiles.set(listOf("ApiConfig.kt"))
}

val checkNoKotlinSuppressions by tasks.registering(CheckNoKotlinSuppressionsTask::class) {
    group = "verification"
    description = "Fails when Kotlin sources or scripts contain suppression mechanisms."
    sourceFiles.from(
        rootProject.fileTree(rootProject.projectDir) {
            include("**/*.kt", "**/*.kts")
            exclude("**/.gradle/**", "**/.idea/**", "**/build/**", "**/generated/**")
        }
    )
    rootPath.set(rootProject.projectDir.absolutePath)
}

val cpdMinimumTokens = providers.gradleProperty("cpdMinimumTokens").orElse("120")

val cpdKotlin by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs PMD CPD against all Kotlin production and test sources."
    classpath = cpdTool
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    args(
        "cpd",
        "--minimum-tokens",
        cpdMinimumTokens.get(),
        "--language",
        "kotlin",
        "--format",
        "text",
        "--fail-on-error",
        "--fail-on-violation",
        "--dir",
        file("src/main/java").absolutePath,
        "--dir",
        file("src/test/java").absolutePath,
        "--dir",
        file("src/androidTest/java").absolutePath
    )
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs the complete pull-request quality policy."
    dependsOn(
        checkNoHardcodedUrls,
        checkNoKotlinSuppressions,
        cpdKotlin,
        "detekt",
        "ktlintCheck",
        "lint",
        "testDebugUnitTest"
    )
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
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
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
    kapt("com.google.dagger:hilt-compiler:2.52")
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

    cpdTool("net.sourceforge.pmd:pmd-cli:7.28.0")
    cpdTool("net.sourceforge.pmd:pmd-kotlin:7.28.0")
}
