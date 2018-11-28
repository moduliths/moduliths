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
package com.acme.myproject;

import static org.assertj.core.api.Assertions.*;

import de.olivergierke.moduliths.model.Modules;
import de.olivergierke.moduliths.model.Modules.Filters;

import org.junit.Test;

import com.acme.myproject.invalid.InvalidComponent;
import com.acme.myproject.moduleB.internal.InternalComponentB;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Test cases to verify the validity of the overall modulith rules
 *
 * @author Oliver Gierke
 * @author Peter Gafert
 */
public class ModulithTest {

	static final DescribedPredicate<JavaClass> DEFAULT_EXCLUSIONS = Filters.withoutModules("cycleA", "cycleB",
			"invalid2");

	@Test
	public void verifyModules() {

		String componentName = InternalComponentB.class.getName();

		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> Modules.of(Application.class, DEFAULT_EXCLUSIONS).verify()) //
				.withMessageContaining(String.format("Module '%s' depends on non-exposed type %s within module 'moduleB'",
						"invalid", componentName))
				.withMessageContaining(String.format("<%s.<init>(%s)> has parameter of type <%s>",
						InvalidComponent.class.getName(), componentName, componentName));
	}

	@Test
	public void verifyModulesWithoutInvalid() {
		Modules.of(Application.class, DEFAULT_EXCLUSIONS.or(Filters.withoutModule("invalid"))).verify();
	}

	@Test // #28
	public void detectsCycleBetweenModules() {

		assertThatExceptionOfType(AssertionError.class) //
				.isThrownBy(() -> Modules.of(Application.class, Filters.withoutModules("invalid", "invalid2")).verify()) //

				// mentions modules
				.withMessageContaining("cycleA") //
				.withMessageContaining("cycleB") //

				// mentions offending types
				.withMessageContaining("CycleA") //
				.withMessageContaining("CycleB");
	}
}
