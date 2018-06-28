/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.olivergierke.moduliths.model;

import lombok.ToString;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;

/**
 * @author Oliver Gierke
 */
@ToString
public class Module {

	private final JavaPackage javaPackage;
	private final Optional<de.olivergierke.moduliths.Module> moduleAnnotation;

	Module(JavaPackage javaPackage) {

		this.javaPackage = javaPackage;
		this.moduleAnnotation = javaPackage.getAnnotation(de.olivergierke.moduliths.Module.class);
	}

	public String getName() {
		return javaPackage.getLocalName();
	}

	public String getDisplayName() {

		return moduleAnnotation.map(de.olivergierke.moduliths.Module::displayName)//
				.orElseGet(() -> javaPackage.getLocalName());
	}

	/**
	 * Returns all modules that contain the types, the types of the current module depend on.
	 *
	 * @param modules must not be {@literal null}.
	 * @return
	 */
	public Set<Module> getDependentModules(Modules modules) {

		Assert.notNull(modules, "Modules must not be null!");

		return getTypesDependedOn() //
				.map(modules::getModuleByType) //
				.flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty)) //
				.collect(Collectors.toSet());
	}

	public Classes getSpringBeans() {

		return javaPackage.that(CanBeAnnotated.Predicates.annotatedWith(Component.class)
				.or(CanBeAnnotated.Predicates.metaAnnotatedWith(Component.class)));
	}

	public boolean contains(JavaClass type) {
		return javaPackage.contains(type);
	}

	public NamedInterface getPrimaryNamedInterface() {
		return new NamedInterface(javaPackage.toSingle().getClasses());
	}

	public boolean isExposed(JavaClass type) {
		return getPrimaryNamedInterface().contains(type);
	}

	public void verifyDependencies(Modules modules) {

		getTypesDependedOn().forEach(it -> {

			modules.getModuleByType(it).ifPresent(module -> {

				Assert.state(module.isExposed(it),
						() -> String.format("Type %s is not exposed by module %s!", it.getName(), module.getName()));
			});
		});
	}

	private Stream<JavaClass> getTypesDependedOn() {

		return javaPackage.stream() //
				.flatMap(it -> getDependencyTypes(it)) //
				.filter(it -> !contains(it));
	}

    private static Stream<JavaClass> getDependencyTypes(JavaClass type) {

        Stream<JavaClass> parameters = getConstructorAndMethodParameters(type);
        Stream<JavaClass> fieldTypes = getFieldTypes(type);
        Stream<JavaClass> directDependencies = type.getDirectDependenciesFromSelf().stream() //
                .map(it -> it.getTargetClass());

        // FIXME: Checking for 'java' is not reliable against further external libraries, we might want to look for Source == JAR/absent again?
        // Other than that we could limit this to dependencies that were imported as well, since those should be the ones we're interested in
        // when we check our Modulith? The only way at the moment AFAIK unfortunately is checking JavaClasses.contain(type) :-(
        return Stream.concat(Stream.concat(directDependencies, parameters), fieldTypes)
                .distinct()
                .filter(it -> !it.getPackage().startsWith("java"));
    }

    private static Stream<JavaClass> getConstructorAndMethodParameters(JavaClass type) {
        return type.getCodeUnits().stream()
                .flatMap(it -> it.getParameters().stream());
    }

    private static Stream<JavaClass> getFieldTypes(JavaClass type) {
        return type.getFields().stream().map(it -> it.getType());
    }
}
