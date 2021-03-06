import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dev.sandrocaseiro"
version = "2.0.1"
description = "Kotlin Spring Template API"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val beanioVersion = "2.1.0"
val cucumberVersion = "6.2.2"
val dependencyCheckVersion = "5.3.2"
//Needed for RestAssured. Remove as soon Spring BOM is updated
val groovyVersion = "3.0.2"
val guavaVersion = "28.2-jre"
val jacocoVersion = "0.8.5"
val jjwtVersion = "0.11.2"
val restAssuredVersion = "4.3.1"
// If you need to set Feign Headers dynamically, use Hoxton.SR4
val springCloudVersion = "Hoxton.SR6"
val springDocOpenApiVersion = "1.4.3"
val wiremockVersion = "2.27.0"

plugins {
    id("org.springframework.boot") version "2.3.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("org.owasp.dependencycheck") version "5.3.2"
    id("org.sonarqube") version "3.0"
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.spring") version "1.3.72"
    war
    jacoco
}

project.ext {
    set("env", "dev")
}

sourceSets {
    val main by getting
    val test by getting
    val integrationTest by creating {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("src/integration-test/kotlin")
        }
        resources.srcDir(file("src/integration-test/resources"))
        compileClasspath += main.compileClasspath + test.compileClasspath
        runtimeClasspath += main.runtimeClasspath + test.runtimeClasspath
    }
}

configurations {
    all {
        exclude("org.springframework.boot", "spring-boot-starter-logging")
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}")
    }
}

dependencies {
    //Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude("org.springframework.boot", "spring-boot-starter-tomcat")
    }

    if (gradle.startParameter.taskNames.any { listOf(tasks.bootJar.name, tasks.bootWar.name, tasks.bootRun.name).any { l -> it.startsWith(l) } })
        providedCompile("org.springframework.boot:spring-boot-starter-undertow")
    else
        implementation("org.springframework.boot:spring-boot-starter-undertow")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")

    implementation("io.github.openfeign", "feign-jackson")
    implementation("io.github.openfeign", "feign-okhttp")

    implementation("io.jsonwebtoken", "jjwt-api", jjwtVersion)
    implementation("io.jsonwebtoken", "jjwt-impl", jjwtVersion)
    implementation("io.jsonwebtoken", "jjwt-jackson", jjwtVersion)

    implementation("org.beanio", "beanio", beanioVersion)

    implementation("org.flywaydb:flyway-core")
    runtimeOnly("com.oracle.database.jdbc", "ojdbc8")

    implementation("com.google.guava", "guava", guavaVersion)

    implementation("org.springdoc", "springdoc-openapi-ui", springDocOpenApiVersion)
    implementation("org.springdoc", "springdoc-openapi-security", springDocOpenApiVersion)
    implementation("org.springdoc", "springdoc-openapi-kotlin", springDocOpenApiVersion)

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("com.h2database", "h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.cucumber", "cucumber-java8", cucumberVersion)
    testImplementation("io.cucumber", "cucumber-junit", cucumberVersion)
    testImplementation("io.cucumber", "cucumber-spring", cucumberVersion)
    testImplementation("io.rest-assured", "rest-assured-all", restAssuredVersion)
    testImplementation("io.rest-assured", "rest-assured", restAssuredVersion)
    testImplementation("io.rest-assured", "kotlin-extensions", restAssuredVersion) {
        exclude("io.rest-assured", "rest-assured")
    }
    testImplementation("org.codehaus.groovy", "groovy", groovyVersion)
    testImplementation("org.codehaus.groovy", "groovy-xml", groovyVersion)
    testImplementation("com.github.tomakehurst", "wiremock-jre8", wiremockVersion)
}

dependencyCheck {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
    failOnError = false
    failBuildOnCVSS = 11F
    junitFailOnCVSS = 11F
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

listOf("dev", "test", "prod").forEach { env ->
    tasks.register("bootJar${env.capitalize()}") {
        ext["env"] = env

        finalizedBy(tasks.bootJar)
    }

    tasks.register("bootWar${env.capitalize()}") {
        ext["env"] = env

        finalizedBy(tasks.bootWar)
    }

    tasks.register("bootRun${env.capitalize()}") {
        ext["env"] = env

        finalizedBy(tasks.bootRun)
    }
}

tasks.bootRun {
    if (ext["env"] == "test")
        classpath += sourceSets["integrationTest"].runtimeClasspath
}

tasks.processResources {
    filesMatching("application.yml") {
        expand(project.properties)
    }
}

tasks.register<Test>("integration-test") {
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
}
