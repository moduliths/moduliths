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

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static org.assertj.core.api.Assertions.*;

import de.olivergierke.moduliths.model.Modules;

import java.util.Arrays;

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

	private static final String INVALID_MODULE_NAME = "invalid";

	@Test
	public void verifyModules() {

		String componentName = InternalComponentB.class.getName();

		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> Modules.of(Application.class, withoutModules("cycleA", "cycleB")).verify()) //
				.withMessageContaining(String.format("Module '%s' depends on non-exposed type %s within module 'moduleB'",
						INVALID_MODULE_NAME, componentName))
				.withMessageContaining(String.format("<%s.<init>(%s)> has parameter of type <%s>",
						InvalidComponent.class.getName(), componentName, componentName));
	}

	@Test
	public void verifyModulesWithoutInvalid() {
		Modules.of(Application.class, withoutModules(INVALID_MODULE_NAME, "cycleA", "cycleB")).verify();
	}

	@Test // #28
	public void detectsCycleBetweenModules() {

		assertThatExceptionOfType(AssertionError.class) //
				.isThrownBy(() -> Modules.of(Application.class, withoutModule(INVALID_MODULE_NAME)).verify()) //

				// mentions modules
				.withMessageContaining("cycleA") //
				.withMessageContaining("cycleB") //

				// mentions offending types
				.withMessageContaining("CycleA") //
				.withMessageContaining("CycleB");
	}

	private static DescribedPredicate<JavaClass> withoutModules(String... names) {

		return Arrays.stream(names) //
				.map(it -> withoutModule(it)) //
				.reduce(DescribedPredicate.alwaysFalse(), DescribedPredicate::or, (__, right) -> right);
	}

	private static DescribedPredicate<JavaClass> withoutModule(String name) {
		return resideInAPackage("..".concat(name).concat(".."));
	}
}
