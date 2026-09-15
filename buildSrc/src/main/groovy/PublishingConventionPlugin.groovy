import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

class PublishingConventionPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply('java')
        project.plugins.apply('maven-publish')

        // Ensure project.version is set to an immutable release version (never SNAPSHOT)
        String resolvedVersion = (project.findProperty('version')
                ?: System.getenv('RELEASE_VERSION')
                ?: project.rootProject.version) as String

        if (resolvedVersion == null || resolvedVersion == 'unspecified' || resolvedVersion.endsWith('-SNAPSHOT')) {
            resolvedVersion = '1.0.0'
        }
        project.version = resolvedVersion

        project.publishing {
            publications {
                gpr(MavenPublication) {
                    from project.components.java
                    artifactId = project.name.toLowerCase()
                    version = project.version
                }
            }
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = project.uri("https://maven.pkg.github.com/" + (System.getenv('GITHUB_REPOSITORY') ?: 'IngsisPrintScript2026/PrintScript'))
                    credentials {
                        username = System.getenv('USERNAME') ?: System.getenv('GITHUB_ACTOR') ?: project.findProperty('gprUser')
                        password = System.getenv('TOKEN') ?: System.getenv('GITHUB_TOKEN') ?: project.findProperty('gprToken')
                    }
                }
            }
        }
    }
}
