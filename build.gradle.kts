import com.lagradost.cloudstream3.gradle.CloudstreamExtension 
import com.android.build.gradle.BaseExtension

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

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs +
                        "-Xno-call-assertions" +
                        "-Xno-param-assertions" +
                        "-Xno-receiver-assertions" +
                        "-Xopt-in=kotlin.RequiresOptIn"
            }
        }

        lintOptions {
            isCheckDependencies = true
            isAbortOnError = false
            disable("MissingTranslation")
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

        apk("com.lagradost:cloudstream3:pre-release")
        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.4")
        implementation("org.jsoup:jsoup:1.16.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    }

    tasks.withType<JavaCompile> {
        options.isFork = true
        options.isIncremental = true
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}