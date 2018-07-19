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
package de.olivergierke.moduliths.test;

import de.olivergierke.moduliths.model.JavaPackage;
import de.olivergierke.moduliths.model.Module;
import de.olivergierke.moduliths.model.Modules;
import de.olivergierke.moduliths.test.ModuleTest.BootstrapMode;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotatedElementUtils;

import com.tngtech.archunit.thirdparty.com.google.common.base.Supplier;
import com.tngtech.archunit.thirdparty.com.google.common.base.Suppliers;

/**
 * @author Oliver Gierke
 */
@Slf4j
@EqualsAndHashCode(of = "key")
public class ModuleTestExecution implements Iterable<Module> {

	private static Map<Class<?>, Class<?>> MODULITH_TYPES = new HashMap<>();
	private static Map<Key, ModuleTestExecution> EXECUTIONS = new HashMap<>();

	private final Key key;

	private final @Getter BootstrapMode bootstrapMode;
	private final @Getter Module module;
	private final @Getter Modules modules;

	private final Supplier<List<JavaPackage>> basePackages;
	private final Supplier<List<Module>> dependencies;

	private ModuleTestExecution(ModuleTest annotation, Modules modules, Module module) {

		this.key = Key.of(module.getBasePackage().getName(), annotation);
		this.modules = modules;
		this.bootstrapMode = annotation.mode();
		this.module = module;

		this.basePackages = Suppliers.memoize(() -> {

			Stream<JavaPackage> moduleBasePackages = module.getBasePackages(modules, bootstrapMode.getDepth());
			Stream<JavaPackage> extraPackages = Arrays.stream(annotation.extraIncludes()) //
					.map(modules::getModuleByName).flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty)) //
					.map(Module::getBasePackage);

			return Stream.concat(moduleBasePackages, extraPackages).collect(Collectors.toList());
		});

		this.dependencies = Suppliers.memoize(() -> module.getBootstrapDependencies(modules, bootstrapMode.getDepth()));

		if (annotation.verifyAutomatically()) {
			verify();
		}
	}

	public static ModuleTestExecution of(Class<?> type) {

		ModuleTest annotation = AnnotatedElementUtils.findMergedAnnotation(type, ModuleTest.class);
		String packageName = type.getPackage().getName();

		Class<?> modulithType = MODULITH_TYPES.computeIfAbsent(type,
				it -> new ModulithConfigurationFinder().findFromPackage(packageName));
		Modules modules = Modules.of(modulithType);
		Module module = modules.getModuleByBasePackage(packageName) //
				.orElseThrow(
						() -> new IllegalStateException(String.format("Package %s is not part of any module!", packageName)));

		return EXECUTIONS.computeIfAbsent(Key.of(module.getBasePackage().getName(), annotation),
				it -> new ModuleTestExecution(annotation, modules, module));
	}

	/**
	 * Returns all base packages the current execution needs to use for component scanning, auto-configuration etc.
	 * 
	 * @return
	 */
	public Stream<String> getBasePackages() {
		return basePackages.get().stream().map(JavaPackage::getName);
	}

	public boolean includes(String className) {

		boolean result = modules.withinRootPackages(className) //
				|| basePackages.get().stream().anyMatch(it -> it.contains(className));

		if (result) {
			LOG.debug("Including class {}.", className);
		}

		return !result;
	}

	/**
	 * Returns all module dependencies, based on the current {@link BootstrapMode}.
	 * 
	 * @return
	 */
	public List<Module> getDependencies() {
		return dependencies.get();
	}

	/**
	 * Explicitly trigger the module structure verification.
	 */
	public void verify() {
		modules.verify();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Module> iterator() {
		return modules.iterator();
	}

	@Value
	@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
	private static class Key {

		String moduleBasePackage;
		ModuleTest annotation;
	}
}
