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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;

import static com.tngtech.archunit.core.domain.Formatters.*;
import static com.tngtech.archunit.thirdparty.com.google.common.base.Preconditions.*;
import static java.lang.System.*;
import static java.util.Objects.*;

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

		return moduleAnnotation.map(de.olivergierke.moduliths.Module::displayName) //
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

		return getDependenciesToOther(modules)
				.map(it -> it.target) //
				.map(modules::getModuleByType) //
				.flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty)) //
				.collect(Collectors.toSet());
	}

	public Classes getSpringBeans() {

		return javaPackage.that(CanBeAnnotated.Predicates.annotatedWith(Component.class) //
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

		getDependenciesToOther(modules).forEach(it -> {
			it.isValidDependencyWithin(modules);
		});
	}

	private Stream<ModuleDependency> getDependenciesToOther(Modules modules) {
		return javaPackage.stream().flatMap(it -> getModuleDependenciesOf(it, modules));
	}

	private Stream<ModuleDependency> getModuleDependenciesOf(JavaClass type, Modules modules) {

		Stream<ModuleDependency> parameters = getDependenciesFromCodeUnitParameters(type, modules);
		Stream<ModuleDependency> fieldTypes = getDependenciesFromFields(type, modules);
		Stream<ModuleDependency> directDependencies = type.getDirectDependenciesFromSelf().stream() //
				.filter(dependency -> isDependencyToOtherModule(dependency.getTargetClass(), modules)) //
				.map(ModuleDependency::new);

		return Stream.concat(Stream.concat(directDependencies, parameters), fieldTypes).distinct();
	}

	private Stream<ModuleDependency> getDependenciesFromCodeUnitParameters(JavaClass type, Modules modules) {
		return type.getCodeUnits().stream() //
				.flatMap(ModuleDependency::allFrom) //
				.filter(moduleDependency -> isDependencyToOtherModule(moduleDependency.target, modules));
	}

	private boolean isDependencyToOtherModule(JavaClass dependency, Modules modules) {
		return modules.contain(dependency) && !this.contains(dependency);
	}

	private Stream<ModuleDependency> getDependenciesFromFields(JavaClass type, Modules modules) {
		return type.getFields().stream() //
				.filter(it -> isDependencyToOtherModule(it.getType(), modules)) //
				.map(ModuleDependency::fromField);
	}

	@ToString
	@EqualsAndHashCode
	private static class ModuleDependency {
		private final JavaClass origin;
		private final JavaClass target;
		private final String description;

		ModuleDependency(Dependency dependency) {
			this(dependency.getOriginClass(), dependency.getTargetClass(), dependency.getDescription());
		}

		ModuleDependency(JavaClass origin, JavaClass target, String description) {
			this.origin = requireNonNull(origin);
			this.target = requireNonNull(target);
			this.description = requireNonNull(description);
		}

		void isValidDependencyWithin(Modules modules) {
			Module targetModule = getExistingModuleOf(target, modules);

			Assert.state(targetModule.isExposed(target),
					() -> {
						Module originModule = getExistingModuleOf(origin, modules);
						String violationText = String.format("Module '%s' depends on non-exposed type %s within module '%s'!",
								originModule.getName(), target.getName(), targetModule.getName());
						return violationText + lineSeparator() + description;
					});
		}

		private Module getExistingModuleOf(JavaClass javaClass, Modules modules) {
			Optional<Module> module = modules.getModuleByType(javaClass);
			checkState(module.isPresent(),
					"Origin/Target of a %s should always be within a module, but %s is not",
					getClass().getSimpleName(), javaClass.getName());
			return module.get();
		}

		static ModuleDependency fromCodeUnitParameter(JavaCodeUnit codeUnit, JavaClass parameter) {
			String description = createDescription(codeUnit, parameter, "parameter");
			return new ModuleDependency(codeUnit.getOwner(), parameter, description);
		}

		static ModuleDependency fromCodeUnitReturnType(JavaCodeUnit codeUnit) {
			String description = createDescription(codeUnit, codeUnit.getReturnType(), "return type");
			return new ModuleDependency(codeUnit.getOwner(), codeUnit.getReturnType(), description);
		}

		static ModuleDependency fromField(JavaField field) {
			String description = String.format("field %s is of type %s in %s",
					field.getFullName(), field.getType().getName(), formatLocation(field.getOwner(), 0));
			return new ModuleDependency(field.getOwner(), field.getType(), description);
		}

		static Stream<ModuleDependency> allFrom(JavaCodeUnit codeUnit) {
			Stream<ModuleDependency> parameterDependencies =
					codeUnit.getParameters().stream().map(it -> fromCodeUnitParameter(codeUnit, it));
			Stream<ModuleDependency> returnType = Stream.of(fromCodeUnitReturnType(codeUnit));
			return Stream.concat(parameterDependencies, returnType);
		}

		private static String createDescription(JavaCodeUnit codeUnit, JavaClass declaredElement, String declarationDescription) {
			String codeUnitDescription = formatMethod(codeUnit.getOwner().getName(), codeUnit.getName(), codeUnit.getParameters());
			String declaration = declarationDescription + " " + declaredElement.getName();
			String location = formatLocation(codeUnit.getOwner(), 0);
			return String.format("%s declares %s in %s", codeUnitDescription, declaration, location);
		}
	}
}
