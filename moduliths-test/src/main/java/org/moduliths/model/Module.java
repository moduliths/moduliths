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
package org.moduliths.model;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.*;
import static java.lang.System.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.SourceCodeLocation;
import com.tngtech.archunit.thirdparty.com.google.common.base.Supplier;
import com.tngtech.archunit.thirdparty.com.google.common.base.Suppliers;

/**
 * @author Oliver Gierke
 */
@EqualsAndHashCode(doNotUseGetters = true)
public class Module {

	private final @Getter JavaPackage basePackage;
	private final Optional<org.moduliths.Module> moduleAnnotation;
	private final @Getter NamedInterfaces namedInterfaces;
	private final boolean useFullyQualifiedModuleNames;

	private final Supplier<Classes> springBeans;

	Module(JavaPackage basePackage, boolean useFullyQualifiedModuleNames) {

		this.basePackage = basePackage;
		this.moduleAnnotation = basePackage.getAnnotation(org.moduliths.Module.class);
		this.namedInterfaces = NamedInterfaces.discoverNamedInterfaces(basePackage);
		this.useFullyQualifiedModuleNames = useFullyQualifiedModuleNames;

		this.springBeans = Suppliers.memoize(() -> filterSpringBeans(basePackage));
	}

	private static Classes filterSpringBeans(JavaPackage source) {

		Classes atBeanTypes = source.that(annotatedWith(Configuration.class)).stream() //
				.flatMap(it -> it.getMethods().stream()) //
				.filter(it -> it.isAnnotatedWith(Bean.class) || it.isMetaAnnotatedWith(Bean.class)) //
				.map(JavaMethod::getRawReturnType) //
				.collect(Collectors.collectingAndThen(Collectors.toList(), Classes::of));

		// Keep module defined beans first
		Map<Boolean, List<JavaClass>> collect = atBeanTypes.stream() //
				.collect(Collectors.groupingBy(it -> source.contains(it)));

		Classes coreComponents = source.that(assignableTo("org.springframework.data.repository.Repository") //
				.or(annotatedWith(Component.class)) //
				.or(metaAnnotatedWith(Component.class)));

		return coreComponents //
				.and(collect.getOrDefault(true, Collections.emptyList())) //
				.and(collect.getOrDefault(false, Collections.emptyList()));
	}

	public String getName() {
		return useFullyQualifiedModuleNames ? basePackage.getName() : basePackage.getLocalName();
	}

	public String getDisplayName() {

		return moduleAnnotation.map(org.moduliths.Module::displayName) //
				.filter(StringUtils::hasText) //
				.orElseGet(() -> basePackage.getLocalName());
	}

	public List<Module> getDependencies(Modules modules, DependencyType... type) {

		return getAllModuleDependencies(modules) //
				.filter(it -> type.length == 0 ? true : Arrays.stream(type).anyMatch(it::hasType)) //
				.map(it -> modules.getModuleByType(it.target)) //
				.distinct() //
				.flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty)) //
				.collect(Collectors.toList());
	}

	/**
	 * Returns all modules that contain types which the types of the current module depend on.
	 *
	 * @param modules must not be {@literal null}.
	 * @return
	 */
	public Stream<Module> getBootstrapDependencies(Modules modules) {

		Assert.notNull(modules, "Modules must not be null!");

		return getBootstrapDependencies(modules, DependencyDepth.IMMEDIATE);
	}

	public Stream<Module> getBootstrapDependencies(Modules modules, DependencyDepth depth) {

		Assert.notNull(modules, "Modules must not be null!");
		Assert.notNull(depth, "Dependency depth must not be null!");

		return streamDependencies(modules, depth);
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
	@Override
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

			List<Module> dependencies = getBootstrapDependencies(modules).collect(Collectors.toList());

			builder.append("> Direct module dependencies: ");
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

	/**
	 * Returns all allowed module dependencies, either explicitly declared or defined as shared on the given
	 * {@link Modules} instance.
	 *
	 * @param modules must not be {@literal null}.
	 * @return
	 */
	List<Module> getAllowedDependencies(Modules modules) {

		Assert.notNull(modules, "Modules must not be null!");

		List<String> allowedDependencyNames = moduleAnnotation.map(it -> Arrays.stream(it.allowedDependencies())) //
				.orElse(Stream.empty()).collect(Collectors.toList());

		if (allowedDependencyNames.isEmpty()) {
			return Collections.emptyList();
		}

		Stream<Module> explicitlyDeclaredModules = allowedDependencyNames.stream() //
				.map(modules::getModuleByName) //
				.flatMap(it -> it.map(Stream::of).orElse(Stream.empty()));

		return Stream.concat(explicitlyDeclaredModules, modules.getSharedModules().stream()) //
				.distinct() //
				.collect(Collectors.toList());
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
				return getDirectModuleDependencies(modules);
			case ALL:
			default:
				return getDirectModuleDependencies(modules) //
						.flatMap(it -> Stream.concat(Stream.of(it), it.streamDependencies(modules, DependencyDepth.ALL))) //
						.distinct();
		}
	}

	private Stream<Module> getDirectModuleDependencies(Modules modules) {

		return getSpringBeans().stream() //
				.flatMap(it -> ModuleDependency.fromType(it)) //
				.filter(it -> isDependencyToOtherModule(it.target, modules)) //
				.map(it -> modules.getModuleByType(it.target)) //
				.distinct() //
				.flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty));
	}

	private Stream<ModuleDependency> getModuleDependenciesOf(JavaClass type, Modules modules) {

		Stream<ModuleDependency> injections = ModuleDependency.fromType(type) //
				.filter(it -> isDependencyToOtherModule(it.getTarget(), modules)); //
		// Stream<ModuleDependency> parameters = getDependenciesFromCodeUnitParameters(type, modules);
		Stream<ModuleDependency> directDependencies = type.getDirectDependenciesFromSelf().stream() //
				.filter(it -> isDependencyToOtherModule(it.getTargetClass(), modules)) //
				.map(ModuleDependency::new);

		return Stream.concat(injections, directDependencies).distinct();
	}

	private boolean isDependencyToOtherModule(JavaClass dependency, Modules modules) {
		return modules.contains(dependency) && !contains(dependency);
	}

	public enum DependencyDepth {

		NONE,

		IMMEDIATE,

		ALL;
	}

	@ToString
	@EqualsAndHashCode
	@RequiredArgsConstructor
	static class ModuleDependency {

		private static final List<String> INJECTION_TYPES = Arrays.asList(//
				Autowired.class.getName(), //
				Resource.class.getName(), //
				"javax.inject.Inject");

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

			Module originModule = getExistingModuleOf(origin, modules);
			Module targetModule = getExistingModuleOf(target, modules);

			List<Module> allowedTargets = originModule.getAllowedDependencies(modules);

			Assert.state(allowedTargets.isEmpty() || allowedTargets.contains(targetModule), () -> {

				String allowedTargetsString = allowedTargets.stream() //
						.map(Module::getName) //
						.collect(Collectors.joining(", "));

				return String.format("Module '%s' depends on module '%s' via %s -> %s. Allowed target modules: %s.",
						originModule.getName(), targetModule.getName(), origin.getName(), target.getName(), allowedTargetsString);
			});

			Assert.state(targetModule.isExposed(target), () -> {

				String violationText = String.format("Module '%s' depends on non-exposed type %s within module '%s'!",
						originModule.getName(), target.getName(), targetModule.getName());

				return violationText + lineSeparator() + description;
			});
		}

		Module getExistingModuleOf(JavaClass javaClass, Modules modules) {

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

			String description = createDescription(codeUnit, codeUnit.getRawReturnType(), "return type");

			return new ModuleDependency(codeUnit.getOwner(), codeUnit.getRawReturnType(), description,
					DependencyType.DEFAULT);
		}

		static Stream<ModuleDependency> fromType(JavaClass source) {
			return Stream.concat(Stream.concat(fromConstructorOf(source), fromMethodsOf(source)), fromFieldsOf(source));
		}

		private static Stream<ModuleDependency> fromConstructorOf(JavaClass source) {

			Set<JavaConstructor> constructors = source.getConstructors();

			return constructors.stream() //
					.filter(it -> constructors.size() == 1 || isInjectionPoint(it)) //
					.flatMap(it -> it.getRawParameterTypes().stream() //
							.map(parameter -> new InjectionModuleDependency(source, parameter, it)));
		}

		private static Stream<ModuleDependency> fromFieldsOf(JavaClass source) {

			Stream<ModuleDependency> fieldInjections = source.getAllFields().stream() //
					.filter(ModuleDependency::isInjectionPoint) //
					.map(field -> new InjectionModuleDependency(source, field.getRawType(), field));

			return fieldInjections;
		}

		private static Stream<ModuleDependency> fromMethodsOf(JavaClass source) {

			Set<JavaMethod> methods = source.getAllMethods().stream() //
					.filter(it -> !it.getOwner().isEquivalentTo(Object.class)) //
					.collect(Collectors.toSet());

			if (methods.isEmpty()) {
				return Stream.empty();
			}

			Stream<ModuleDependency> returnTypes = methods.stream() //
					.filter(it -> !it.getRawReturnType().isPrimitive()) //
					.filter(it -> !it.getRawReturnType().getPackageName().startsWith("java")) //
					.map(it -> fromCodeUnitReturnType(it));

			Set<JavaMethod> injectionMethods = methods.stream() //
					.filter(ModuleDependency::isInjectionPoint) //
					.collect(Collectors.toSet());

			Stream<ModuleDependency> methodInjections = injectionMethods.stream() //
					.flatMap(it -> it.getRawParameterTypes().stream() //
							.map(parameter -> new InjectionModuleDependency(source, parameter, it)));

			Stream<ModuleDependency> otherMethods = methods.stream() //
					.filter(it -> !injectionMethods.contains(it)) //
					.flatMap(it -> it.getRawParameterTypes().stream() //
							.map(parameter -> fromCodeUnitParameter(it, parameter)));

			return Stream.concat(Stream.concat(methodInjections, otherMethods), returnTypes);
		}

		static Stream<ModuleDependency> allFrom(JavaCodeUnit codeUnit) {

			Stream<ModuleDependency> parameterDependencies = codeUnit.getRawParameterTypes()//
					.stream() //
					.map(it -> fromCodeUnitParameter(codeUnit, it));

			Stream<ModuleDependency> returnType = Stream.of(fromCodeUnitReturnType(codeUnit));

			return Stream.concat(parameterDependencies, returnType);
		}

		private static String createDescription(JavaMember codeUnit, JavaClass declaringElement,
				String declarationDescription) {

			String type = declaringElement.getSimpleName();

			String codeUnitDescription = JavaConstructor.class.isInstance(codeUnit) //
					? String.format("%s", declaringElement.getSimpleName()) //
					: String.format("%s.%s", declaringElement.getSimpleName(), codeUnit.getName());

			if (JavaCodeUnit.class.isInstance(codeUnit)) {
				codeUnitDescription = String.format("%s(%s)", codeUnitDescription,
						JavaCodeUnit.class.cast(codeUnit).getRawParameterTypes().stream() //
								.map(JavaClass::getSimpleName) //
								.collect(Collectors.joining(", ")));
			}

			String annotations = codeUnit.getAnnotations().stream() //
					.filter(it -> INJECTION_TYPES.contains(it.getRawType().getName())) //
					.map(it -> "@" + it.getRawType().getSimpleName()) //
					.collect(Collectors.joining(" ", "", " "));

			annotations = StringUtils.hasText(annotations) ? annotations : "";

			String declaration = declarationDescription + " " + annotations + codeUnitDescription;
			String location = SourceCodeLocation.of(codeUnit.getOwner(), 0).toString();

			return String.format("%s declares %s in %s", type, declaration, location);
		}

		private static boolean isInjectionPoint(JavaMember unit) {
			return INJECTION_TYPES.stream().anyMatch(type -> unit.isAnnotatedWith(type));
		}
	}

	private static class InjectionModuleDependency extends ModuleDependency {

		private final JavaMember member;
		private final boolean isConfigurationClass;

		/**
		 * @param origin
		 * @param target
		 * @param member
		 */
		public InjectionModuleDependency(JavaClass origin, JavaClass target, JavaMember member) {

			super(origin, target, ModuleDependency.createDescription(member, origin, getDescriptionFor(member)),
					DependencyType.USES_COMPONENT);

			this.member = member;
			this.isConfigurationClass = origin.isAnnotatedWith(Configuration.class)
					|| origin.isMetaAnnotatedWith(Configuration.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.Module.ModuleDependency#isValidDependencyWithin(org.moduliths.model.Modules)
		 */
		@Override
		void isValidDependencyWithin(Modules modules) {

			JavaClass owner = member.getOwner();
			Module module = getExistingModuleOf(owner, modules);

			Assert.state(!JavaField.class.isInstance(member) || isConfigurationClass,
					String.format("Class %s in module %s uses field injection in %s! Prefer constructor injection instead.",
							owner.getSimpleName(), module, member));

			super.isValidDependencyWithin(modules);
		}

		private static String getDescriptionFor(JavaMember member) {

			if (JavaConstructor.class.isInstance(member)) {
				return "constructor";
			} else if (JavaMethod.class.isInstance(member)) {
				return "injection method";
			} else if (JavaField.class.isInstance(member)) {
				return "injected field";
			}

			throw new IllegalArgumentException(String.format("Invalid member type %s!", member.toString()));
		}
	}

	public enum DependencyType {

		/**
		 * Indicates that the module depends on the other one by a component dependency, i.e. that other module needs to be
		 * bootstrapped to run the source module.
		 */
		USES_COMPONENT,

		/**
		 * Indicates that the module refers to an entity of the other.
		 */
		ENTITY,

		/**
		 * Indicates that the module depends on the other by declaring an event listener for an event exposed by the other
		 * module. Thus, the target module does not have to be bootstrapped to run the source one.
		 */
		EVENT_LISTENER,

		DEFAULT {

			/*
			 * (non-Javadoc)
			 * @see org.moduliths.test.model.Module.ModuleDependency.DependencyType#or(com.tngtech.archunit.thirdparty.com.google.common.base.Supplier)
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

		public DependencyType or(Supplier<DependencyType> supplier) {
			return this;
		}

		/**
		 * Returns all {@link DependencyType}s except the given ones.
		 *
		 * @param types must not be {@literal null}.
		 * @return
		 */
		public static Stream<DependencyType> allBut(Collection<DependencyType> types) {

			Assert.notNull(types, "Types must not be null!");

			Predicate<DependencyType> isIncluded = types::contains;

			return Arrays.stream(values()) //
					.filter(isIncluded.negate());
		}

		public static Stream<DependencyType> allBut(Stream<DependencyType> types) {
			return allBut(types.collect(Collectors.toList()));
		}

		/**
		 * Returns all {@link DependencyType}s except the given ones.
		 *
		 * @param types must not be {@literal null}.
		 * @return
		 */
		public static Stream<DependencyType> allBut(DependencyType... types) {

			Assert.notNull(types, "Types must not be null!");

			return allBut(Arrays.asList(types));
		}
	}
}
