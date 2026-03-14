plugins {
    alias(libs.plugins.neoforge.moddev)
    alias(libs.plugins.shadow)
}

neoForge {
    version = libs.versions.neoforge.version.get()

    runs {
        create("server") {
            server()
        }
    }

    mods {
        create("takaro") {
            sourceSet(sourceSets.main.get())
        }
    }
}

val shade: Configuration by configurations.creating {
    isTransitive = true
}

dependencies {
    shade(project(":core"))
    compileOnly(project(":core"))
}

tasks.shadowJar {
    configurations = listOf(shade)
    archiveClassifier.set("")
    relocate("org.java_websocket", "io.takaro.libs.websocket")
    relocate("com.google.gson", "io.takaro.libs.gson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
