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
    implementation("org.json:json:20240303")
    implementation("com.mysql:mysql-connector-j:9.1.0")
}

tasks.test {
    useJUnitPlatform()
}