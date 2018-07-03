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

import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * @author Oliver Gierke
 */
public class ModuleTestClass {

	private final @Getter BootstrapMode bootstrapMode;
	private final @Getter Module module;
	private final Modules modules;

	public ModuleTestClass(Class<?> type) {

		String packageName = type.getPackage().getName();
		ModuleTest annotation = AnnotatedElementUtils.findMergedAnnotation(type, ModuleTest.class);

		this.bootstrapMode = annotation.mode();

		this.modules = Modules.ofSubpackage(packageName);
		this.module = modules.getModuleByBasePackage(packageName).orElseThrow(
				() -> new IllegalStateException(String.format("Couldn't find module for package '%s'!", packageName)));
	}

	public Stream<String> getBasePackages() {

		return module.getBasePackages(modules, bootstrapMode.getDepth()) //
				.map(JavaPackage::getName);
	}

	public List<Module> getDependencies() {
		return module.getDependencies(modules, bootstrapMode.getDepth());
	}
}
