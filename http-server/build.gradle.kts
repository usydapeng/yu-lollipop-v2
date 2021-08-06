import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  // 使用grpc必须需要设置idea
  idea
  id("com.github.johnrengelman.shadow") version "6.1.0"
  id("com.google.protobuf") version "0.8.16"
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

val vertxVersion = "4.1.2"
val mutinyVertxVersion = "2.12.0"
val junitJupiterVersion = "5.7.0"
val log4jVersion = "2.14.1"
val queryDslVersion = "4.4.0"
val cronUtilsVersion = "9.1.5"
val grpcVersion = "1.30.2"
val protobufVersion = "3.12.2"

val mainVerticleName = "org.zunpeng.vertx.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClassName = launcherClassName
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-service-proxy")
  implementation("io.vertx:vertx-mysql-client")
  implementation("io.vertx:vertx-sql-client-templates")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-redis-client")
  implementation("io.vertx:vertx-web-validation")
  implementation("io.vertx:vertx-json-schema")
  implementation("io.vertx:vertx-config")
  implementation("io.vertx:vertx-config-hocon")
  implementation("io.vertx:vertx-rabbitmq-client")
  implementation("io.vertx:vertx-grpc")

  compileOnly("io.vertx:vertx-codegen")
  annotationProcessor("io.vertx:vertx-codegen:$vertxVersion")
  implementation("io.vertx:vertx-service-proxy")

  runtimeOnly("mysql:mysql-connector-java:6.0.6")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")

  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-core:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-mysql-client:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-sql-client-templates:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-web:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-redis-client:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-web-validation:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-json-schema:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-config:$mutinyVertxVersion")
  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-rabbitmq-client:$mutinyVertxVersion")
  // implementation("io.smallrye.reactive:smallrye-mutiny-vertx-grpc:$mutinyVertxVersion")

  implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
  implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
  implementation("com.lmax:disruptor:3.4.4")

  implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
  implementation("io.grpc:grpc-core:$grpcVersion")
  implementation("io.grpc:grpc-protobuf:$grpcVersion")
  implementation("io.grpc:grpc-stub:$grpcVersion")

  implementation("com.cronutils:cron-utils:$cronUtilsVersion")


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
  description = "Generates the ServiceProxy"
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

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:$protobufVersion"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
    }
    id("vertx") {
      artifact = "io.vertx:vertx-grpc-protoc-plugin:$vertxVersion"
    }
  }
  generateProtoTasks {
    all().forEach {
      it.plugins {
        id("grpc")
        id("vertx")
      }
    }
  }
  // generatedFilesBaseDir = "${project.buildDir}/generated-proto"
}
