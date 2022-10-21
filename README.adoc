= Moduliths

IMPORTANT: The Moduliths project has been discontinued and been transferred to https://github.com/spring-projects-experimental/spring-modulith[Spring Modulith].
The current 1.3 release branch will see futher bugfix upgrades for as long as https://spring.io/projects/spring-boot#support[Spring Boot 2.7 is under open-source support].
For migration instructions, please consult the https://docs.spring.io/spring-modulith/docs/0.1.x/reference/html/#appendix.migrating-from-moduliths[Spring Modulith migration guide].

A playground to build technology supporting the development of modular monolithic (modulithic) Java applications.

image:https://github.com/moduliths/moduliths/actions/workflows/build.yaml/badge.svg["Maven Build", link="https://github.com/moduliths/moduliths/actions/workflows/build.yaml"]

== tl;dr

Moduliths is a Spring Boot extension based on ArchUnit to achieve the following goals:

* _Verify modular structure between individual logical modules in monolithic Spring Boot applications._
+
Prevents cyclic dependencies as well as explicitly defined allowed dependencies to other modules.
Verifies access to public components in API packages (convention based, customizable).
* _Bootstrap a subset of the modules of a monolithic Spring Boot application._
+
Limits the bootstrap of Spring Boot (component scanning and application of auto-configuration) to a single module, a module plus its direct dependents or an entire tree.
* _Derive PlantUML documentation about the modules._
+
Translates the actual module structure into a PlantUML component diagram via Structurizr for easy inclusion in e.g. Asciidoctor based documentation. Diagrams can be rendered for a single module plus its collaborators or the entire system at once.

[[quickstart]]
== Quickstart

1. Create a simple Spring Boot application (e.g. via Spring Initializer).
2. Add the Moduliths dependencies to your project:
+
[source,xml]
----
<dependencies>

  <!-- For the @Modulith annotation -->
  <dependency>
    <groupId>org.moduliths</groupId>
    <artifactId>moduliths-core</artifactId>
    <version>${modulith.version}</version>
  </dependency>

  <!-- Test support -->
  <dependency>
    <groupId>org.moduliths</groupId>
    <artifactId>moduliths-test</artifactId>
    <version>${modulith.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>

<!-- If you use snapshots -->
<repositories>
  <repository>
    <id>spring-snapshots</id>
    <url>https://repo.spring.io/libs-snapshot</url>
  </repository>
</repositories>
----
3. Setup your package structure like described <<modules,here>>.
4. Create a module test like described <<modules.running-tests,here>>.

[[context]]
== Context

When it comes to designing applications we currently deal with two architectural approaches: monolithic applications and microservices.
While often presented as opposed approaches, in their extremes, they actually form the ends of a spectrum into which a particular application architecture can be positioned.
The trend towards smaller systems is strongly driven by the fact that monolithic applications tend to architecturally degrade over time, even if – at the beginning of their lives – an architecture is defined.
Architecture violations creep into the projects over time unnoticed. Evolvability suffers as systems become harder to change.

Microservices on the other hand promise stronger means of separation but at the same time introduce a lot of complexity as even for small applications teams have to deal with the challenges of distributed systems.

This repo acts as a playground to experiment with different approaches to allow defining modular monoliths, so that it's easy to maintain modularity over time and detect violations early.
This will keep the ability to modify and advance the codebase over time and ease the effort to split up the system in the attempt to extract parts of it into a dedicated project.

[[the-architecture-code-gap]]
=== The architecture-code-gap

In software projects, architectural design decisions and constraints are usually defined in some way and then have to be implemented in a code base.
Traditionally the connection between the architectural decisions and the actual have been naming conventions that easily diverge and cause the architecture actually implemented in the code base to slowly degrade over time.
We'd like to explore stronger means of connections between architecture and code and even look into advanced support of frameworks and libraries to e.g. allow testability of individual components within an overall system.

There already exists a variety of technologies that attempts to bridge that gap from the architectural definition side, mostly by trying to capture the architectural definitions in executable form (see https://jqassistant.org/[jQAssistant] and <<existing-tools>>) and verify whether the code base adheres to the conventions defined.
In this playground, we're going to explore the opposite way: providing conventions as well as library and framework means, to express architectural definitions directly inside the code base with two major goals:

1. _**Getting the validation of the conventions closer to the code / developer**_ -- If architectural decisions are driven by the development team, it might feel more natural to define architectural concepts in the code base.
The more seamless an architectural rule validation system integrates with the codebase, the more likely it is that the system is used.
An architectural rule that can be verified by the compiler is preferred over a rule verified by executing a test, which in turn is preferred over a verification via dedicated build steps.
2. _**Integration with existing tools**_ - Even in combination with existing tools, it might just help them to ship generic architectural rules out of the box with the developer just following conventions or explicitly annotating code to trigger the actual validation.

[[design-goals]]
== Design goals

* Enable developers to write architecturally evident code, i.e. provide means to express architectural concepts in code to close the gap between the two.
* Provide means to verify defined architectural constraints as close as possible to the code (by the compiler, through tests or additional build tools).
* As little invasive as possible technology. I.e. we prefer documented conventions over annotations over required type dependencies.

[[feature-set]]
== Current feature set

[[modules]]
=== A module model for Java packages

[[modules.simple]]
==== The most simple module setup

At its very core, Modulith assumes you have your application centered around a single Java package (let's assume `com.acme.myapp`).
The application base package is defined by declaring a class that is equipped with the `@Modulith` annotation.
It's basically equivalent to `@SpringBootApplication` but indicates you're opting in into the module programming model and package structures.

[NOTE]
.Notation conventions
====
[source]
----
+ – public type
o – package protected type
----
====

Every direct sub-package of this package is considered to describe a module:

[source]
----
com.acme.myapp                          <1>
+ @Modulith ….MyApplication

com.acme.myapp.moduleA                  <2>
+ ….MyComponentA(MyComponentB)

com.acme.myapp.moduleB                  <3>
+ ….MyComponentB(MySupportingComponent)
o ….MySupportingComponent

com.acme.myapp.moduleC                  <4>
+ ….MyComponentC(MyComponentA)
----
<1> The application root package.
<2> `moduleA`, implicitly depending on `moduleB`, only public components.
<3> `moduleB`, not depending on other modules, hiding an internal component.
<4> `moduleC`, depending on `moduleA` and thus `moduleB` in turn.

In this simple scenario, the only additional means of encapsulation is the Java package scope, that allows developers to hide internal components from other modules.
This is surprisingly simple and effective.
For more complex structural scenarios, see <<modules.complex>>.

[[modules.running-tests]]
==== Running tests for a module

An individual module can be run for tests using the `@ModuleTest` annotation as follows:

[source,java]
----
package com.acme.myapp.moduleB;

@RunWith(SpringRunner.class)
@ModuleTest
public class ModuleBTest { … }
----

Running the test like this will cause the root application class be considered as well as all explicit configuration inside it.
The test run will customize the configuration to limit the component scanning, the auto-configuration and entity scan packages to the package of the module test.
It will also verify dependencies between the modules.
See more on that in <<modules.complex>>.

For `moduleB` this is very simple as it doesn't depend on any other modules in the application.

===== Handling module dependencies in tests

Without any further configuration, running an integration test for a module that depends on other modules, will cause the `ApplicationContext` to start to fail as Spring beans depended on are not available.
One option to resolve this is to declare ``@MockBean``s for all dependencies required:

[source, java]
----
package com.acme.myapp.moduleA;

@RunWith(SpringRunner.class)
@ModuleTest
public class ModuleATest {

  @MockBean MyComponentB myComponentB;
}
----

An alternative approach to this can be to broaden the scope of the test by defining an alternative bootstrap mode of `DIRECT_DEPENDENCIES`.

[source, java]
----
package com.acme.myapp.moduleA;

@RunWith(SpringRunner.class)
@ModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
public class ModuleATest { … }
----

This will now inspect the module structure of the system, detect the dependency of Module A to Module B and include the latter into the component scan as well as auto-configuration and entity scan packages.
If the direct dependency has dependencies in turn, you now need to mock those using `@MockBean` in the test setup.

In case you want to run all modules up the dependency chain of the module to be tested use `BootstrapMode.ALL_DEPENDENCIES`.
This will cause all dependent modules to be bootstrapped but unrelated ones to be excluded.

[[modules.general-recommendations]]
===== General recommendations

If you find yourself having to mock too many components of upstream modules or include too many modules into the test run, it usually indicates that your modules are too tightly coupled.
You might want to look into replacing those direct invocations of beans in other modules by rather publishing an application event from the source module and consume it from the other module.
See <<sos>> for further details.

[[modules.complex]]
==== More complex modules

Sometimes, a single package is not enough to capture all components of a single module and developers would like to organize code into additional packages.
Let's assume Module B is using the following structure:

[source]
----
com.acme.myapp
+ @Modulith ….MyApplication

com.acme.myapp.moduleA
+ ….MyComponentA(MyComponentB)

com.acme.myapp.moduleB
+ ….MyComponentB(MySupportingComponent, MyInternal)
o ….MySupportingComponent
com.acme.myapp.moduleB.internal
+ ….MyInternal(MyOtherInternal, InternalSupporting)
o ….InternalSupporting
com.acme.myapp.moduleB.otherinternal
+ ….MyOtherInternal
----

In this case we have two supporting packages that contain components that depend on each other (`MyInternal` depending on `InternalSupport` in the same package as well as `MyOtherInternal` in the other supporting package).
By convention, on the module level, only dependencies to the top-level module package are allowed.
I.e. any type residing in another module that depends on types in either `….moduleB.internal` or `moduleB.otherInternal` will cause an `@ModuleTest` to fail.

[[modules.complex.named-interfaces]]
===== Named interfaces

In case a single public package defining the module root is not enough, modules can define so called named interface packages that will constitute packages that are eligible targets for dependencies from components of other modules.

[source]
----
com.acme.myapp
+ @Modulith ….MyApplication

com.acme.myapp.moduleA
+ ….MyComponentA(MyComponentB)

com.acme.myapp.complex.api
+ @NamedInterface("API") ….package-info.java
com.acme.myapp.complex.spi
+ @NamedInterface("SPI") ….package-info.java
com.acme.myapp.complex.internal
o ….MyInternal
----

As you can see, we have dedicated packages of the module annotated with `@NamedInterface`.
The annotation will cause each of the packages to be referable from other modules dependencies, whereas non-annotated packages of the module (`internal`) won't (including the module root package).

[[architectural-rule-enforcement]]
=== Enforcement of architectural rules

[NOTE]
.Conventions
====
icon:check-circle[] – already implemented

icon:question-circle[] – not yet implemented
====

Given the module conventions we can already implement a couple of derived rules:

icon:check-circle[] _**Assume top-level module package the API package**_ -- If sub-packages are used, we could assume that only the top-level one contains API to be referred to from other modules.

icon:check-circle[] _**Provide an annotation to be used on packages so that multiple different named interfaces to a module can be defined.**_

icon:check-circle[] _**Prevent invalid dependencies into module internal package.**_ -- All module sub-packages by default except explicitly declared as named interface.

icon:question-circle[] `allowedDependencies` would then have to use `moduleA.API`, `moduleB.SPI`. If a single named interface exists, referring to the module implicitly refers to the single only named interface.

icon:question-circle[] _**Verify module setup**_ -- We can verify the validity of the module setup to prevent configuration errors to go unnoticed:

* icon:question-circle[] Catch invalid module and named interface references in `allowedDependencies`.

icon:question-circle[] _**Derive default allowed dependencies based on the Spring bean component tree**_ -- by default we can inspect the Spring beans in the individual modules, their dependencies and assume the beans structure describes the allowed dependency structure.
This can be overridden by explicitly declaring `@Module(allowedDependencies = …)` on the package level.

icon:question-circle[] _**Correlate actual dependencies with the ones defined (implicit or explicit)**_ -- Even with dependencies only defined implicitly by the Spring bean structure, the code can contain ordinary type dependencies that violate the module structure.

icon:question-circle[] _**No cycles on the module level**_ -- We should generally disallow cycles on the module level.

== Sample applications

* https://github.com/odrotbohm/spring-restbucks[Spring RESTBucks] - an implementation of the RESTBucks API from the ”REST in Practice” book. Primary a showcase for hypermedia APIs but still using Moduliths primarily for documentation purposes.
* https://github.com/st-tu-dresden/salespoint[Salespoint] - a POS library developed by the TU Dresden to be used in the software engineering lab to teach third semester students how to build web applications with Spring Boot.

== Ideas

=== In the works

* <<modules, A default module programming model based on Java packages that can be customized using annotations>>
* <<modules.running-tests, A Spring Boot extension that allows bootstrapping individual modules in various modes>>
* <<architectural-rule-enforcement, Out of the box module dependency tests>>

=== Unapproached yet

* <<apt-rule-verification, Rule verification via APT>>


[[boot-module-tests]]
=== Spring Boot based module tests

==== Further ideas

* As Spring https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#context-functionality-events[Application Events] are a recommended means to implement inter-module interaction, we could register an `ApplicationListener` that exposes API to easily verify events being triggered, event listeners being triggered etc.

[[apt-rule-verification]]
=== Rule verification via APT

Assuming we're able to get an APT implemented that's run on top of the current code base, we could run the aforementioned verifications and issue compiler errors for violations.

[[existing-tools]]
== Existing tools

* https://github.com/TNG/ArchUnit[ArchUnit] -- Tool to define allowed dependencies on a type and package based level, usually executed via JUnit.
[[jqassistant]]
* https://jqassistant.org/[jQAssistant] -- Broader tool to analyze projects using a Neo4j-based meta-model and concepts and constraints described via Cypher queries.
* https://structurizr.com/[Structurizr] -- Software architecture description and visualization tool by Simon Brown.
Includes Spring integration via automatic stereotype annotation detection.

[appendix]
== Appendix

[bibliography]
=== Further resources

- [[[safd]]] Simon Brown -- Software Architecture for Developers (https://leanpub.com/b/software-architecture[Books], https://softwarearchitecturefordevelopers.com/[Website])
- [[[sos]]] Oliver Gierke -- Refactoring to a System of Systems (https://speakerdeck.com/olivergierke/refactoring-to-a-system-of-systems[Slidedeck], https://www.youtube.com/watch?v=VWefNT8Lb74[Recording])
- [[[whoops]]] Oliver Gierke -- Whoops, where did my architecture go? (http://olivergierke.de/2013/01/whoops-where-did-my-architecture-go/[Webpage])

[glossary]
=== Glossary
Named Interface:: Given a module, a sub-set of types that constitute the API of the module, i.e. candidates for referral by other modules.

=== Release instructions

* `mvn versions:set -DnewVersion=$version -DgenerateBackupPoms=false`
* Change `/scm/tag` im `pom.xml` to `$version`
* Commit against release ticket id
* Tag commit
* Push commit and tag
* `mvn clean deploy -Psonatype`
* `mvn versions:set -DnewVersion=$snapshotVersion -DgenerateBackupPoms=false`
* Commit against release ticket id with message "Prepare next development iteration."
* Push commit.
