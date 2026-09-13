plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.javafaker:javafaker:1.0.2")
    // Source: https://mvnrepository.com/artifact/org.jfree/jfreechart
    implementation("org.jfree:jfreechart:1.5.6")
}

tasks.test {
    useJUnitPlatform()
}

val consoleEncoding = listOf(
    "-Dfile.encoding=UTF-8",
    "-Dstdout.encoding=UTF-8",
    "-Dstderr.encoding=UTF-8",
)

tasks.withType<JavaExec>().configureEach {
    jvmArgs(consoleEncoding)
    defaultCharacterEncoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    jvmArgs(consoleEncoding)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}