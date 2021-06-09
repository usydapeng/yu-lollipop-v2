import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "org.zunpeng"
version = "1.0.0-SNAPSHOT"

repositories {
  maven {
    url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    mavenContent {
      snapshotsOnly()
    }
  }
  mavenCentral()
}

val vertxVersion = "4.1.0"
val mutinyVertxVersion = "2.6.0"
val junitJupiterVersion = "5.7.0"
val log4jVersion = "2.14.1"

val mainVerticleName = "org.zunpeng.vertx.HttpServerVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClassName = launcherClassName
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-service-proxy")
  implementation("io.vertx:vertx-sql-client-templates")
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-redis-client")
  implementation("io.vertx:vertx-shell")

  compileOnly("io.vertx:vertx-codegen")
  annotationProcessor("io.vertx:vertx-codegen:$vertxVersion")
  implementation("io.vertx:vertx-service-proxy")

  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-core:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-sql-client-templates:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-redis-client:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-shell:$mutinyVertxVersion")

  implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
  implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")

  implementation("com.corundumstudio.socketio:netty-socketio:1.7.19")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}

tasks.register<JavaCompile>("annotationProcessing") {
  group = "other"
  source = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java
  destinationDir = project.file("${project.buildDir}/generated/main/java")
  classpath = configurations.compileClasspath.get()
  options.annotationProcessorPath = configurations.compileClasspath.get()
  options.compilerArgs = listOf(
    "-proc:only",
    "-processor", "io.vertx.codegen.CodeGenProcessor",
    "-Acodegen.output=${project.projectDir}/src/main"
  )
}

tasks.compileJava {
  dependsOn(tasks.named("annotationProcessing"))
}

sourceSets {
  main {
    java {
      srcDirs(project.file("${project.buildDir}/generated/main/java"))
    }
  }
}
