# Change Log

## [v1.2.1.RELEASE](https://github.com/donovanmuller/spring-cloud-deployer-openshift/tree/v1.2.1.RELEASE) (2017-11-07)
[Full Changelog](https://github.com/donovanmuller/spring-cloud-deployer-openshift/compare/v1.2.0.RELEASE...v1.2.1.RELEASE)

**Implemented enhancements:**

- Update to 1.2.2 release of s-c-deployer [\#44](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/44)

**Fixed bugs:**

- Deployer should cancel all in progress deployments when undeploying [\#29](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/29)
- SCDF route naming uses '.' rather than '-' as a separator [\#47](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/47)

**Closed issues:**

- Investigate 504 Gateway Time-out errors when resolving Maven artifacts for the first time [\#28](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/28)
- Maven task launcher should use a deployment triggered by a build [\#21](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/21)
- Not able to pass custom spring.cloud.deployer.kubernetes.serviceAnnotations property [\#45](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/45)
- Investigate why Apache Velocity logs are being logged at DEBUG level on testCompile [\#42](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/42)
- Fix 'MavenResourceProjectExtractorTest\#extractMavenProject' that fails on Travis [\#31](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/31)
- Add CI configuration [\#9](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/9)

## [v1.2.0.RELEASE](https://github.com/donovanmuller/spring-cloud-deployer-openshift/tree/v1.2.0.RELEASE) (2017-07-09)
[Full Changelog](https://github.com/donovanmuller/spring-cloud-deployer-openshift/compare/v1.1.0.RELEASE...v1.2.0.RELEASE)

**Closed issues:**

- Update to 1.2.1 release of s-c-deployer [\#43](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/43)
- Use ImageStream when referencing container images [\#37](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/37)
- Update to 1.2.0 release of s-c-deployer [\#35](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/35)
- DeploymentConfig not deleted when undeploy stream [\#32](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/32)
- Identify the Maven remote repository type dynamically [\#15](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/15)
- Ensure there is always a running deployment after a deploy [\#10](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/10)

**Merged pull requests:**

- Scale down attempt should timeout [\#40](https://github.com/donovanmuller/spring-cloud-deployer-openshift/pull/40) ([hekonsek](https://github.com/hekonsek))
- Let's reduce default test logging level to INFO [\#39](https://github.com/donovanmuller/spring-cloud-deployer-openshift/pull/39) ([hekonsek](https://github.com/hekonsek))
- OpenShift configuration should provide default Maven properties [\#38](https://github.com/donovanmuller/spring-cloud-deployer-openshift/pull/38) ([hekonsek](https://github.com/hekonsek))
- Namespace of Image for ImageStreamTrigger is now Configurable [\#34](https://github.com/donovanmuller/spring-cloud-deployer-openshift/pull/34) ([retosbb](https://github.com/retosbb))

## [v1.1.0.RELEASE](https://github.com/donovanmuller/spring-cloud-deployer-openshift/tree/v1.1.0.RELEASE) (2016-12-01)
**Fixed bugs:**

- The spring.cloud.deployer.openshift.defaultDockerfile deployment property is not honoured [\#24](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/24)

**Closed issues:**

- Add Bintray distribution details [\#26](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/26)
- Allow integration tests to be skipped [\#25](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/25)
- Update to 1.1.0 release versions of Spring Cloud Data Flow / Kubernetes Deployer [\#23](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/23)
- Update README [\#22](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/22)
- Add GPG Maven plugin to sign artifacts [\#20](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/20)
- Investigate whether we can re-use the PodSpec creation found in Kubernetes deployer [\#19](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/19)
- Relook at Kubernetes integration tests [\#18](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/18)
- Translate all Kubernetes namespace prefixed properties are translated to OpenShift namespace prefixes. [\#17](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/17)
- Verify if comments around OpenShift Resource management still applies [\#16](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/16)
- Apply changes from the volume support added in upstream Kubernetes deployer [\#14](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/14)
- Update to Spring Boot 1.4 testing framework [\#11](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/11)
- Group related app Services in a stream [\#8](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/8)
- Add support for overriding container entry point and app specific environment variables [\#7](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/7)
- Add full support for volumes/volumeMounts [\#4](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/4)
- Update functionality picked from Kubernetes deployer [\#3](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/3)
- Add support for new deployer cpu and memory deployment properties [\#2](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/2)
- Update to RC1 release of Kubernetes Deployer [\#1](https://github.com/donovanmuller/spring-cloud-deployer-openshift/issues/1)

**Merged pull requests:**

- Added all upstream Kubernetes deployer features. [\#13](https://github.com/donovanmuller/spring-cloud-deployer-openshift/pull/13) ([donovanmuller](https://github.com/donovanmuller))



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*
