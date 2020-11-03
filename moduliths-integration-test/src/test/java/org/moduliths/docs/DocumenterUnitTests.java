/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.moduliths.docs;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.moduliths.docs.Documenter.CanvasOptions;
import org.moduliths.model.Modules;
import org.moduliths.model.SpringBean;
import org.springframework.util.MultiValueMap;

import com.acme.myproject.Application;
import com.acme.myproject.stereotypes.Stereotypes;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * @author Oliver Drotbohm
 */
class DocumenterUnitTests {

	Modules modules = Modules.of(Application.class);

	@Test
	void groupsSpringBeansByArchitecturallyEvidentType() {

		MultiValueMap<String, SpringBean> result = CanvasOptions.defaults()
				.grouping("Representations", CanvasOptions.nameMatching(".*Representations"))
				.grouping("Interface implementations", CanvasOptions.subtypeOf(Stereotypes.SomeAppInterface.class))
				.groupBeans(modules.getModuleByName("stereotypes").orElseThrow(RuntimeException::new));

		assertThat(result).containsOnlyKeys("Controllers", "Services", "Repositories", "Event listeners", "Representations",
				"Interface implementations", "Others");

		List<SpringBean> impls = result.get("Interface implementations");

		assertThat(impls).hasSize(1) //
				.extracting(it -> it.getType()) //
				.extracting(JavaClass::getSimpleName) //
				.containsExactly("SomeAppInterfaceImplementation");

		List<SpringBean> listeners = result.get("Event listeners");

		assertThat(listeners).hasSize(2) //
				.extracting(it -> it.getType()) //
				.extracting(JavaClass::getSimpleName) //
				.containsOnly("SomeEventListener", "SomeTxEventListener");
	}

	@Test
	void playWithOutput() {

		Documenter documenter = new Documenter(modules);

		CanvasOptions foos = CanvasOptions.defaults() //
				.grouping("Representations", CanvasOptions.nameMatching(".*Representations"));

		modules.forEach(it -> System.out.println(documenter.toModuleCanvas(it, foos)));
	}
}
