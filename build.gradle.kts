plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // SLF4J API
    implementation("org.slf4j:slf4j-api:1.7.36")

    // SLF4J Logger implementation (np. Logback)
    implementation("ch.qos.logback:logback-classic:1.2.10")

    // DataStax Cassandra Java Driver
    implementation("com.datastax.cassandra:cassandra-driver-core:3.11.2")
}

tasks.test {
    useJUnitPlatform()
}