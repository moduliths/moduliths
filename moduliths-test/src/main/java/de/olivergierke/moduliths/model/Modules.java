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

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.*;
import static java.util.Collections.*;

import de.olivergierke.moduliths.Modulith;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;

/**
 * @author Oliver Gierke
 */
public class Modules {

	private final Classes classes;
	private final Map<String, Module> modules = new HashMap<>();

	private Modules(Class<?> modulithType, DescribedPredicate<JavaClass> ignored) {
		URI rootUri = getRootUriOf(modulithType);
		JavaClasses importedClasses = new ClassFileImporter().importLocations(singleton(Location.of(rootUri))).that(DescribedPredicate.not(ignored));

		this.classes = Classes.of(importedClasses);

		getModules().forEach(it -> this.modules.put(it.getName(), it));
	}

	private URI getRootUriOf(Class<?> modulithType) {
		URI uriOfModulith = new ClassFileImporter().importClass(modulithType).getSource().get().getUri();
		String root = uriOfModulith.toString().replaceAll("[^/]+\\.class$", "");
		return URI.create(root);
	}

	public static Modules of(Class<?> modulithType) {
		return of(modulithType, DescribedPredicate.alwaysFalse());
	}

	public static Modules of(Class<?> modulithType, DescribedPredicate<JavaClass> ignored) {
		checkArgument(modulithType.getAnnotation(Modulith.class) != null,
				"Modules can only be retrieved from a @%s root type, but %s is not annotated with @%s",
				Modulith.class.getSimpleName(), modulithType.getSimpleName(), Modulith.class.getSimpleName());
		return new Modules(modulithType, ignored);
	}

	public String getRootPackage() {

		return classes.that(annotatedWith(Modulith.class)).stream() //
				.findFirst() //
				.orElseThrow(IllegalStateException::new) //
				.getPackage();
	}

	public boolean contain(JavaClass javaClass) {
		return modules.values().stream().anyMatch(module -> module.contains(javaClass));
	}

	public Collection<Module> getModules() {

		String rootPackage = getRootPackage();

		return JavaPackage.forNested(classes, rootPackage) //
				.getDirectSubPackages().stream() //
				.map(Module::new) //
				.collect(Collectors.toSet());
	}

	public Optional<Module> getModuleByName(String name) {
		return Optional.ofNullable(modules.get(name));
	}

	public Optional<Module> getModuleByType(JavaClass type) {

		return modules.values().stream() //
				.filter(it -> it.contains(type)) //
				.findFirst();
	}

	public void verify() {
		modules.values().forEach(it -> it.verifyDependencies(this));
	}
}
