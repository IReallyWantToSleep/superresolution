plugins {
    java
}

group = "io.homo.SuperResolution"
version = "0.0.1-alpha.1"

repositories {
    mavenCentral()
}

tasks.named("clean") {
    doFirst {
        delete("$projectDir/cpp/build")
        delete("$projectDir/cpp/output")
    }
}

tasks.register<Copy>("copyNativeLib") {
    from("$projectDir/cpp/output/lib") {
        include("libSuperResolution+*+*.so", "libSuperResolution+*+*.dll", "libSuperResolution+*+*.dylib")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    into("$projectDir/../common/src/main/resources/lib/")

    from("$projectDir/cpp/output/bin") {
        include("libSuperResolution+*+*.so", "libSuperResolution+*+*.dll", "libSuperResolution+*+*.dylib")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    into("$projectDir/../common/src/main/resources/lib/")
}

tasks.register<Copy>("copyNativeLibAll") {
    from("$projectDir/cpp/output/lib") {
        include("libSuperResolution*+*+*.so", "libSuperResolution*+*+*.dll", "libSuperResolution*+*+*.dylib")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    into("$projectDir/../common/src/main/resources/lib/")

    from("$projectDir/cpp/output/bin") {
        include("libSuperResolution*+*+*.so", "libSuperResolution*+*+*.dll", "libSuperResolution*+*+*.dylib")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    into("$projectDir/../common/src/main/resources/lib/")
}

val osName = System.getProperty("os.name").lowercase()

tasks.register<Exec>("buildNativeCppWindows") {
    group = "build"
    description = "Build native C/C++ DLLs via platform script"
    workingDir = file("$projectDir/cpp")
    if (osName.contains("windows")) {
        commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "build_windows.ps1")
    } else if (osName.contains("linux")) {
        commandLine("bash", "build_windows_docker.sh")
    } else {
        throw GradleException("Unsupported OS for native build: $osName")
    }
}

tasks.register<Exec>("buildNativeCppLinux") {
    group = "build"
    description = "Build native C/C++ .so Libraries via platform script"
    workingDir = file("$projectDir/cpp")
    commandLine("bash", "build_linux.sh")
}

tasks.register("buildNativeCpp") {
    group = "build"
    description = "Build native C/C++ libraries via platform script"
    if (osName.contains("windows")) {
        dependsOn("buildNativeCppWindows")
    }
    if (osName.contains("linux")) {
        dependsOn("buildNativeCppLinux")
    }
}

tasks.named("copyNativeLibAll") {
    mustRunAfter("buildNativeCpp")
    outputs.upToDateWhen { false }
}

tasks.register("buildNative") {
    dependsOn("buildNativeCpp")
    dependsOn("copyNativeLibAll")
}
