plugins {
    `maven-publish`
    signing
    kotlin("jvm") version "1.9.21"
    application
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "ru.fiddlededee"
version = "0.7.4"
val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
    testImplementation("com.approvaltests:approvaltests:18.5.0")
    testImplementation("org.asciidoctor:asciidoctorj:2.5.11")
    testImplementation("org.libreoffice:juh:7.4.7")
    testImplementation("org.libreoffice:unoil:7.4.7")
    testImplementation("org.libreoffice:ridl:7.4.7")
    testImplementation("org.libreoffice:libreoffice:7.4.7")
    testImplementation("org.libreoffice:unoloader:7.4.7")
    testImplementation("org.libreoffice:jurt:7.4.7")
    testImplementation("de.redsix:pdfcompare:1.2.2")
    testImplementation("org.dom4j:dom4j:2.1.4")
    testImplementation("com.helger:ph-css:7.0.1")

    implementation("org.jsoup:jsoup:1.18.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    implementation("org.redundent:kotlin-xml-builder:1.9.1")
}

tasks.test {
    useJUnitPlatform()
}


kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
//            val releaseRepo = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
//            val snapshotRepo = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
//
//            name = "OSSRH"
//            url = if (version.toString().endsWith("SNAPSHOT")) snapshotRepo else releaseRepo
//            credentials {
//                username = project.properties["ossrhUsername"].toString()
//                password = project.properties["ossrhPassword"].toString()
//            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = group.toString()
            artifactId = "unidoc-publisher"
            version = version.toString()

            from(components["java"])

            pom {
                name = "UniDoc Publisher"
                description = """
                            UniDoc Publisher is a highly customizable Kotlin library that 
                            provides a comprehensive set of tools to parse HTML files into 
                            an abstract syntax tree (AST), transform it in any desirable way 
                            and render the Open Document flat format (FODT)
                        """.trimIndent()
                url = "https://github.com/fiddlededee/unidoc-publisher"
                packaging = "jar" // jar is the default, but still set it to make it clear

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                scm {
                    url = "https://github.com/fiddlededee/unidoc-publisher"
                    connection = "scm:git://github.com:fiddlededee/unidoc-publisher.git"
                    developerConnection = "scm:git://github.com:fiddlededee/unidoc-publisher.git"
                }

                developers {
                    developer {
                        id = "fiddlededee"
                        name = "Nikolaj Potashnikov"
                        email = "consulting@yandex.ru"
                    }
                }
            }
        }
    }

}

val checkVersionUpdated = tasks.register("checkVersionUpdated") {
    println("Current version: $version")
    arrayOf(
        "example/builder/table.main.kts",
        "example/ps-118/ps-118.main.kts",
        "example/customize-everything/customize-everything.main.kts",
        "example/writerside-tutorial/writerside.main.kts",
        "doc/build-doc.main.kts",
    ).forEach { fileName ->
        File(fileName).readText().lines()
            .firstOrNull { it.contains("ru.fiddlededee:unidoc-publisher") }
            ?.apply {
                if (!this.contains("\"ru.fiddlededee:unidoc-publisher:$version\""))
                    throw StopExecutionException("Dependency $this \nin $fileName \nshould depend on version $version")
            } ?: throw StopExecutionException("UniDoc Publisher dependency \nin $fileName not found")
    }
}

tasks.build {
    dependsOn(checkVersionUpdated)
}

signing {
    sign(publishing.publications)
}
//tasks.withType<Sign> {
//    onlyIf { isReleaseVersion }
//}
