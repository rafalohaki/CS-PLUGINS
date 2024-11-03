import com.lagradost.cloudstream3.gradle.CloudstreamExtension 
import com.android.build.gradle.BaseExtension
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.kotlin
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    }
}

// Configure repositories for all projects including root
allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = 
    extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = 
    extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "rafalohaki/CS-PLUGINS")
    }

    android {
        defaultConfig {
            minSdk = 21
            compileSdkVersion(33)
            targetSdk = 33
        }

        // Enable build caching
        buildFeatures {
            buildConfig = false // Disable if not needed
        }

        // Optimize dex options
        dexOptions {
            preDexLibraries = !System.getenv("CI").toBoolean()
            javaMaxHeapSize = "2g"
        }

        // Optimize lint options
        lintOptions {
            isCheckDependencies = true
            isAbortOnError = false
            disable("MissingTranslation")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
            isCoreLibraryDesugaringEnabled = true
        }

        // Optimize compile tasks
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs +
                        "-Xno-call-assertions" +
                        "-Xno-param-assertions" +
                        "-Xno-receiver-assertions" +
                        "-Xopt-in=kotlin.RequiresOptIn" +
                        "-Xskip-prerelease-check"
            }
        }

        // Configure test options
        testOptions {
            unitTests.all {
                it.apply {
                    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
                    testLogging {
                        events = setOf(TestLogEvent.FAILED)
                    }
                }
            }
        }
    }

    configurations.all {
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
            preferProjectModules()
        }
    }

    dependencies {
        val apk by configurations
        val implementation by configurations
        val coreLibraryDesugaring by configurations

        apk("com.lagradost:cloudstream3:pre-release")

        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.4") 
        implementation("org.jsoup:jsoup:1.16.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
    }

    // Configure tasks
    tasks {
        withType<JavaCompile> {
            options.isFork = true
            options.isIncremental = true
        }

        withType<Test> {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        }
    }
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }

    // Disable unnecessary tasks in CI
    if (System.getenv("CI").toBoolean()) {
        matching { it.name.contains("lint") }.configureEach { enabled = false }
        matching { it.name.contains("test") }.configureEach { enabled = false }
    }
}