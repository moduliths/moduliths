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

import static com.tngtech.archunit.base.DescribedPredicate.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static java.util.stream.Collectors.*;

import de.olivergierke.moduliths.Modulith;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * @author Oliver Gierke
 * @author Peter Gafert
 */
public class Modules implements Iterable<Module> {

	private static final Map<Key, Modules> CACHE = new HashMap<>();

	private static final List<String> FRAMEWORK_PACKAGES = Arrays.asList(//
			"javax.persistence", //
			"org.springframework.context.event", //
			"org.springframework.data.repository", //
			"org.springframework.stereotype", //
			"org.springframework.web.bind.annotation" //
	);

	private final Map<String, Module> modules;
	private final JavaClasses allClasses;
	private final List<JavaPackage> rootPackages;

	private boolean verified;

	private Modules(Collection<String> packages, DescribedPredicate<JavaClass> ignored,
			boolean useFullyQualifiedModuleNames) {

		List<String> toImport = new ArrayList<>(packages);
		toImport.addAll(FRAMEWORK_PACKAGES);

		this.allClasses = new ClassFileImporter() //
				.withImportOption(new ImportOption.DontIncludeTests()) //
				.importPackages(toImport) //
				.that(not(ignored));

		Classes classes = Classes.of(allClasses);

		this.modules = packages.stream() //
				.flatMap(it -> getSubpackages(classes, it)) //
				.map(it -> new Module(it, useFullyQualifiedModuleNames)) //
				.collect(toMap(Module::getName, Function.identity()));

		this.rootPackages = packages.stream() //
				.map(it -> JavaPackage.forNested(classes, it).toSingle()) //
				.collect(Collectors.toList());
	}

	/**
	 * Creates a new {@link Modules} relative to the given modulith type. Will inspect the {@link Modulith} annotation on
	 * the class given for advanced customizations of the module setup.
	 * 
	 * @param modulithType must not be {@literal null}.
	 * @return
	 */
	public static Modules of(Class<?> modulithType) {
		return of(modulithType, alwaysFalse());
	}

	/**
	 * Creates a new {@link Modules} relative to the given modulith type and a {@link DescribedPredicate} which types and
	 * packages to ignore. Will inspect the {@link Modulith} annotation on the class given for advanced customizations of
	 * the module setup.
	 * 
	 * @param modulithType must not be {@literal null}.
	 * @param ignored must not be {@literal null}.
	 * @return
	 */
	public static Modules of(Class<?> modulithType, DescribedPredicate<JavaClass> ignored) {

		Key key = Key.of(modulithType, ignored);

		return CACHE.computeIfAbsent(key, it -> {

			Assert.notNull(modulithType, "Modulith root type must not be null!");
			Assert.notNull(ignored, "Predicate to describe ignored types must not be null!");

			Modulith modulith = AnnotatedElementUtils.findMergedAnnotation(modulithType, Modulith.class);

			Assert.notNull(modulith,
					() -> String.format("Modules can only be retrieved from a @%s root type, but %s is not annotated with @%s",
							Modulith.class.getSimpleName(), modulithType.getSimpleName(), Modulith.class.getSimpleName()));

			Set<String> basePackages = new HashSet<>();
			basePackages.add(modulithType.getPackage().getName());
			basePackages.addAll(Arrays.asList(modulith.additionalPackages()));

			return new Modules(basePackages, ignored, modulith.useFullyQualifiedModuleNames());
		});
	}

	/**
	 * Returns whether the given {@link JavaClass} is contained within the {@link Modules}.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public boolean contains(JavaClass type) {

		Assert.notNull(type, "Type must not be null!");

		return modules.values().stream() //
				.anyMatch(module -> module.contains(type));
	}

	/**
	 * Returns whether the given type is contained in one of the root packages (not including sub-packages) of the
	 * modules.
	 * 
	 * @param className must not be {@literal null} or empty.
	 * @return
	 */
	public boolean withinRootPackages(String className) {

		Assert.hasText(className, "Class name must not be null or empty!");

		return rootPackages.stream().anyMatch(it -> it.contains(className));
	}

	/**
	 * Returns the {@link Module} with the given name.
	 * 
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	public Optional<Module> getModuleByName(String name) {

		Assert.hasText(name, "Module name must not be null or empty!");

		return Optional.ofNullable(modules.get(name));
	}

	/**
	 * Returns the module that contains the given {@link JavaClass}.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public Optional<Module> getModuleByType(JavaClass type) {

		Assert.notNull(type, "Type must not be null!");

		return modules.values().stream() //
				.filter(it -> it.contains(type)) //
				.findFirst();
	}

	public Optional<Module> getModuleByBasePackage(String name) {

		return modules.values().stream() //
				.filter(it -> it.getBasePackage().getName().equals(name)) //
				.findFirst();
	}

	public void verify() {

		if (verified) {
			return;
		}

		rootPackages.forEach(it -> {

			SlicesRuleDefinition.slices() //
					.matching(it.getName().concat(".(*)..")) //
					.should().beFreeOfCycles() //
					.check(allClasses.that(resideInAPackage(it.getName().concat(".."))));
		});

		modules.values().forEach(it -> {
			it.verifyDependencies(this);
		});

		this.verified = true;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Module> iterator() {
		return modules.values().iterator();
	}

	@Value(staticConstructor = "of")
	private static final class Key {

		Class<?> type;
		DescribedPredicate<?> predicate;
	}

	private static Stream<JavaPackage> getSubpackages(Classes types, String rootPackage) {
		return JavaPackage.forNested(types, rootPackage).getDirectSubPackages().stream();
	}
}
