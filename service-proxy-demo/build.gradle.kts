import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.5.10"
  kotlin("kapt") version "1.5.10"
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
  // id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

group = "org.zunpeng"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.1.0"
val junitJupiterVersion = "5.7.0"
val log4jVersion = "2.14.1"
val cronUtilsVersion = "9.1.5"
val kotlinLoggingVersion = "2.0.8"

val mainVerticleName = "org.zunpeng.vertx.kotlin.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation(kotlin("stdlib-jdk8"))
  implementation("io.vertx:vertx-mysql-client")
  implementation("io.vertx:vertx-sql-client-templates")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-redis-client")
  implementation("io.vertx:vertx-web-validation")
  implementation("io.vertx:vertx-json-schema")
  implementation("io.vertx:vertx-config")
  implementation("io.vertx:vertx-config-hocon")
  implementation("io.vertx:vertx-rabbitmq-client")
  implementation("io.vertx:vertx-service-proxy")

  kapt("io.vertx:vertx-codegen:$vertxVersion:processor")
  kapt("io.vertx:vertx-lang-kotlin-gen:$vertxVersion")
  compileOnly("io.vertx:vertx-codegen:$vertxVersion")

  runtimeOnly("mysql:mysql-connector-java:6.0.6")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")

  implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
  implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
  implementation("com.lmax:disruptor:3.4.4")
  implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

  implementation("com.cronutils:cron-utils:$cronUtilsVersion")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}

kapt {
  javacOptions {
    option("-proc:only")
    option("-processor", "io.vertx.codegen.CodeGenProcessor")
    option("-Acodegen.output", "${project.projectDir}/src/main")
    option("-AoutputDirectory", "$projectDir/src/main")
  }
  arguments {
    arg("destinationDir", "src/main/generated")
  }
}

tasks.register("printVersion") {
  doLast {
    println(version)
  }
}
