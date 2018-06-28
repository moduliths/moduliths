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

import de.olivergierke.moduliths.Modulith;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOptions;

/**
 * @author Oliver Gierke
 */
public class Modules {

	private final Classes classes;
	private final Map<String, Module> modules;

	public Modules() {
		this("");
	}

	public Modules(String path) {
		this(path, DescribedPredicate.alwaysFalse());
	}

	public Modules(String path, DescribedPredicate<? super JavaClass> ignored) {

		// FIXME: Is this property really reliable? I thought the only guarantee is "path where java was executed"
		// We could
		//     * Make the public API Modules(Class<?> modulithAnnotatedType) and scan that package;
		//         in the end we're just looking for that type as root anyway, right?
		//     * Just scan the classpath with the given ImportOptions? (since it skips JARs then anyway)
		String workingDirectory = System.getProperty("user.dir") + "/" + path;

		ImportOptions options = new ImportOptions() //
				.with(ImportOption.Predefined.DONT_INCLUDE_TESTS) //
				.with(ImportOption.Predefined.DONT_INCLUDE_JARS);

		ClassFileImporter importer = new ClassFileImporter(options);

		this.classes = Classes.of(importer.importPath(workingDirectory).that(DescribedPredicate.not(ignored)));

		this.modules = new HashMap<>();

		getModules().forEach(it -> this.modules.put(it.getName(), it));
	}

	public String getRootPackage() {

		return classes.that(CanBeAnnotated.Predicates.annotatedWith(Modulith.class)).stream() //
				.findFirst() //
				.orElseThrow(() -> new IllegalStateException()) //
				.getPackage();
	}

	public Collection<Module> getModules() {

		String rootPackage = getRootPackage();

		return JavaPackage.forNested(classes, rootPackage)//
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
