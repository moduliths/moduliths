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
package de.olivergierke.moduliths.model.test;

import de.olivergierke.moduliths.model.JavaPackage;
import de.olivergierke.moduliths.model.Module;
import de.olivergierke.moduliths.model.Modules;
import de.olivergierke.moduliths.model.test.ModuleTest.BootstrapMode;
import lombok.Getter;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * @author Oliver Gierke
 */
public class ModuleTestExecution implements Iterable<Module> {

	private final @Getter BootstrapMode bootstrapMode;
	private final @Getter Module module;
	private final Modules modules;

	public ModuleTestExecution(Class<?> type) {

		ModuleTest annotation = AnnotatedElementUtils.findMergedAnnotation(type, ModuleTest.class);
		String packageName = type.getPackage().getName();

		this.bootstrapMode = annotation.mode();

		this.modules = Modules.ofSubpackage(packageName);
		this.module = modules.getModuleByBasePackage(packageName) //
				.orElseThrow(
						() -> new IllegalStateException(String.format("Couldn't find module for package '%s'!", packageName)));

		if (annotation.verifyAutomatically()) {
			verify();
		}
	}

	/**
	 * Returns all base packages the current execution needs to use for component scanning, auto-configuration etc.
	 * 
	 * @return
	 */
	public Stream<String> getBasePackages() {

		return module.getBasePackages(modules, bootstrapMode.getDepth()) //
				.map(JavaPackage::getName);
	}

	/**
	 * Returns all module dependencies, based on the current {@link BootstrapMode}.
	 * 
	 * @return
	 */
	public List<Module> getDependencies() {
		return module.getDependencies(modules, bootstrapMode.getDepth());
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
}
