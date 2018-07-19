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

import static com.tngtech.archunit.core.domain.Formatters.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.*;
import static java.lang.System.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.thirdparty.com.google.common.base.Supplier;
import com.tngtech.archunit.thirdparty.com.google.common.base.Suppliers;

/**
 * @author Oliver Gierke
 */
@EqualsAndHashCode
public class Module {

	private static final DescribedPredicate<JavaClass> NO_CLASS = DescribedPredicate.alwaysFalse();

	private final @Getter JavaPackage basePackage;
	private final Optional<de.olivergierke.moduliths.Module> moduleAnnotation;
	private final @Getter NamedInterfaces namedInterfaces;
	private final boolean useFullyQualifiedModuleNames;

	private final Supplier<Classes> springBeans;

	Module(JavaPackage basePackage, boolean useFullyQualifiedModuleNames) {

		this.basePackage = basePackage;
		this.moduleAnnotation = basePackage.getAnnotation(de.olivergierke.moduliths.Module.class);
		this.namedInterfaces = discoverNamedInterfaces(basePackage);
		this.useFullyQualifiedModuleNames = useFullyQualifiedModuleNames;

		this.springBeans = Suppliers.memoize(() -> filterSpringBeans(basePackage));
	}

	private static Classes filterSpringBeans(JavaPackage source) {

		DescribedPredicate<JavaClass> atBeanTypes = source.that(annotatedWith(Configuration.class)).stream() //
				.flatMap(it -> it.getMethods().stream()) //
				.filter(it -> it.isAnnotatedWith(Bean.class) || it.isMetaAnnotatedWith(Bean.class)) //
				.map(JavaMethod::getReturnType) //
				.map(JavaClass::reflect) //
				.reduce(NO_CLASS, (it, type) -> it.or(type(type)), (left, right) -> right);

		return source.that(atBeanTypes.or(assignableTo("org.springframework.data.repository.Repository") //
				.or(annotatedWith(Component.class)) //
				.or(metaAnnotatedWith(Component.class))));
	}

	private static NamedInterfaces discoverNamedInterfaces(JavaPackage basePackage) {

		List<NamedInterface> explicitlyAnnotated = basePackage
				.getSubPackagesAnnotatedWith(de.olivergierke.moduliths.NamedInterface.class) //
				.map(NamedInterface::of) //
				.collect(Collectors.toList());

		return NamedInterfaces.of(explicitlyAnnotated.isEmpty() //
				? Collections.singletonList(NamedInterface.unnamed(basePackage)) //
				: explicitlyAnnotated);
	}

	public String getName() {
		return useFullyQualifiedModuleNames ? basePackage.getName() : basePackage.getLocalName();
	}

	public String getDisplayName() {

		return moduleAnnotation.map(de.olivergierke.moduliths.Module::displayName) //
				.orElseGet(() -> basePackage.getLocalName());
	}

	public List<Module> getDependencies(Modules modules, DependencyType... type) {

		return getAllModuleDependencies(modules) //
				.filter(it -> type.length == 0 ? true : Arrays.stream(type).anyMatch(it::hasType)) //
				.map(it -> modules.getModuleByType(it.target)) //
				.flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty)) //
				.collect(Collectors.toList());
	}

	/**
	 * Returns all modules that contain types which the types of the current module depend on.
	 *
	 * @param modules must not be {@literal null}.
	 * @return
	 */
	public List<Module> getBootstrapDependencies(Modules modules) {

		Assert.notNull(modules, "Modules must not be null!");

		return getBootstrapDependencies(modules, DependencyDepth.IMMEDIATE);
	}

	public List<Module> getBootstrapDependencies(Modules modules, DependencyDepth depth) {

		Assert.notNull(modules, "Modules must not be null!");
		Assert.notNull(depth, "Dependency depth must not be null!");

		return streamDependencies(modules, depth).collect(Collectors.toList());
	}

	/**
	 * Returns all {@link JavaPackage} for the current module including the ones by its dependencies.
	 * 
	 * @param modules must not be {@literal null}.
	 * @param depth must not be {@literal null}.
	 * @return
	 */
	public Stream<JavaPackage> getBasePackages(Modules modules, DependencyDepth depth) {

		Assert.notNull(modules, "Modules must not be null!");
		Assert.notNull(depth, "Dependency depth must not be null!");

		Stream<Module> dependencies = streamDependencies(modules, depth);

		return Stream.concat(Stream.of(this), dependencies) //
				.map(Module::getBasePackage);
	}

	public Classes getSpringBeans() {
		return springBeans.get();
	}

	public boolean contains(JavaClass type) {
		return basePackage.contains(type);
	}

	/**
	 * Returns whether the given {@link JavaClass} is exposed by the current module, i.e. whether it's part of any of the
	 * module's named interfaces.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public boolean isExposed(JavaClass type) {

		Assert.notNull(type, "Type must not be null!");

		return namedInterfaces.stream().anyMatch(it -> it.contains(type));
	}

	public void verifyDependencies(Modules modules) {

		getAllModuleDependencies(modules) //
				.forEach(it -> it.isValidDependencyWithin(modules));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return toString(null);
	}

	public String toString(@Nullable Modules modules) {

		StringBuilder builder = new StringBuilder("## ").append(getDisplayName()).append(" ##\n");
		builder.append("> Logical name: ").append(getName()).append('\n');
		builder.append("> Base package: ").append(basePackage.getName()).append('\n');

		if (namedInterfaces.hasExplicitInterfaces()) {

			builder.append("> Named interfaces:\n");

			namedInterfaces.forEach(it -> builder.append("  + ") //
					.append(it.toString()) //
					.append('\n'));
		}

		if (modules != null) {

			List<Module> dependencies = getBootstrapDependencies(modules);

			builder.append("> Direct dependencies: ");
			builder.append(dependencies.isEmpty() ? "none"
					: dependencies.stream().map(Module::getName).collect(Collectors.joining(", ")));
			builder.append('\n');
		}

		Classes beans = getSpringBeans();

		if (beans.isEmpty()) {

			builder.append("> Spring beans: none\n");

		} else {

			builder.append("> Spring beans:\n");
			beans.forEach(it -> builder.append("  ") //
					.append(Classes.format(it, basePackage.getName()))//
					.append('\n'));
		}

		return builder.toString();
	}

	private Stream<ModuleDependency> getAllModuleDependencies(Modules modules) {

		return basePackage.stream() //
				.flatMap(it -> getModuleDependenciesOf(it, modules));
	}

	private Stream<Module> streamDependencies(Modules modules, DependencyDepth depth) {

		switch (depth) {

			case NONE:
				return Stream.empty();
			case IMMEDIATE:
				return getDirectDependencies(modules);
			case ALL:
			default:
				return getDirectDependencies(modules) //
						.flatMap(it -> Stream.concat(Stream.of(it), it.streamDependencies(modules, DependencyDepth.ALL))) //
						.distinct();
		}
	}

	private Stream<Module> getDirectDependencies(Modules modules) {

		return getSpringBeans().stream() //
				.flatMap(it -> ModuleDependency.fromConstructorOf(it, DependencyType.USES_COMPONENT)) //
				.filter(it -> isDependencyToOtherModule(it.target, modules)) //
				.map(it -> modules.getModuleByType(it.target)) //
				.distinct() //
				.flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty));
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
		return modules.contains(dependency) && !contains(dependency);
	}

	private Stream<ModuleDependency> getDependenciesFromFields(JavaClass type, Modules modules) {

		return type.getFields().stream() //
				.filter(it -> isDependencyToOtherModule(it.getType(), modules)) //
				.map(ModuleDependency::fromField);
	}

	public enum DependencyDepth {

		NONE,

		IMMEDIATE,

		ALL;
	}

	@ToString
	@EqualsAndHashCode
	@RequiredArgsConstructor
	private static class ModuleDependency {

		private final @NonNull @Getter JavaClass origin, target;
		private final @NonNull String description;
		private final @NonNull DependencyType type;

		ModuleDependency(Dependency dependency) {
			this(dependency.getOriginClass(), dependency.getTargetClass(), dependency.getDescription(),
					DependencyType.forDependency(dependency));
		}

		boolean hasType(DependencyType type) {
			return this.type.equals(type);
		}

		void isValidDependencyWithin(Modules modules) {

			Module targetModule = getExistingModuleOf(target, modules);

			Assert.state(targetModule.isExposed(target), () -> {

				Module originModule = getExistingModuleOf(origin, modules);
				String violationText = String.format("Module '%s' depends on non-exposed type %s within module '%s'!",
						originModule.getName(), target.getName(), targetModule.getName());

				return violationText + lineSeparator() + description;
			});
		}

		private Module getExistingModuleOf(JavaClass javaClass, Modules modules) {

			Optional<Module> module = modules.getModuleByType(javaClass);

			return module.orElseThrow(() -> new IllegalStateException(
					String.format("Origin/Target of a %s should always be within a module, but %s is not",
							getClass().getSimpleName(), javaClass.getName())));
		}

		static ModuleDependency fromCodeUnitParameter(JavaCodeUnit codeUnit, JavaClass parameter) {

			String description = createDescription(codeUnit, parameter, "parameter");

			DependencyType type = DependencyType.forCodeUnit(codeUnit) //
					.or(() -> DependencyType.forParameter(parameter));

			return new ModuleDependency(codeUnit.getOwner(), parameter, description, type);
		}

		static ModuleDependency fromCodeUnitReturnType(JavaCodeUnit codeUnit) {

			String description = createDescription(codeUnit, codeUnit.getReturnType(), "return type");

			return new ModuleDependency(codeUnit.getOwner(), codeUnit.getReturnType(), description, DependencyType.DEFAULT);
		}

		static ModuleDependency fromField(JavaField field) {

			String description = String.format("field %s is of type %s in %s", field.getFullName(), field.getType().getName(),
					formatLocation(field.getOwner(), 0));

			DependencyType type = DependencyType.forField(field);

			return new ModuleDependency(field.getOwner(), field.getType(), description, type);
		}

		static Stream<ModuleDependency> fromConstructorOf(JavaClass source, DependencyType type) {

			return source.getConstructors().stream() //
					.flatMap(it -> it.getParameters().stream() //
							.map(parameter -> new ModuleDependency(source, parameter, createDescription(it, source, "constructor"),
									type)));
		}

		static Stream<ModuleDependency> allFrom(JavaCodeUnit codeUnit) {

			Stream<ModuleDependency> parameterDependencies = codeUnit.getParameters()//
					.stream() //
					.map(it -> fromCodeUnitParameter(codeUnit, it));

			Stream<ModuleDependency> returnType = Stream.of(fromCodeUnitReturnType(codeUnit));

			return Stream.concat(parameterDependencies, returnType);
		}

		private static String createDescription(JavaCodeUnit codeUnit, JavaClass declaredElement,
				String declarationDescription) {

			String codeUnitDescription = formatMethod(codeUnit.getOwner().getName(), codeUnit.getName(),
					codeUnit.getParameters());
			String declaration = declarationDescription + " " + declaredElement.getName();
			String location = formatLocation(codeUnit.getOwner(), 0);

			return String.format("%s declares %s in %s", codeUnitDescription, declaration, location);
		}
	}

	public enum DependencyType {

		USES_COMPONENT,

		ENTITY,

		EVENT_LISTENER,

		DEFAULT {

			/* 
			 * (non-Javadoc)
			 * @see de.olivergierke.moduliths.test.model.Module.ModuleDependency.DependencyType#or(com.tngtech.archunit.thirdparty.com.google.common.base.Supplier)
			 */
			@Override
			public DependencyType or(Supplier<DependencyType> supplier) {
				return supplier.get();
			}
		};

		public static DependencyType forParameter(JavaClass type) {
			return type.isAnnotatedWith("javax.persistence.Entity") ? ENTITY : DEFAULT;
		}

		public static DependencyType forCodeUnit(JavaCodeUnit codeUnit) {
			return codeUnit.isAnnotatedWith(EventListener.class) ? EVENT_LISTENER : DEFAULT;
		}

		public static DependencyType forDependency(Dependency dependency) {
			return forParameter(dependency.getTargetClass());
		}

		public static DependencyType forField(JavaField field) {
			return forParameter(field.getType());
		}

		public DependencyType or(Supplier<DependencyType> supplier) {
			return this;
		}
	}
}
