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

import com.acme.myproject.moduleB.internal.InternalComponentB;
import com.acme.myproject.moduleC.InvalidComponent;
import de.olivergierke.moduliths.model.Modules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;

/**
 * Test cases to verify the validity of the overall modulith rules
 *
 * @author Oliver Gierke
 */
public class ModulithTest {
	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void verifyModules() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage(String.format("Module 'moduleC' depends on non-exposed type %s within module 'moduleB'",
				InternalComponentB.class.getName()));
		thrown.expectMessage(String.format("%s.<init>(%s) declares parameter %s",
				InvalidComponent.class.getName(), InternalComponentB.class.getName(), InternalComponentB.class.getName()));

		Modules.of(Application.class).verify();
	}

	@Test
	public void verifyModulesWithoutInvalid() {
		Modules.of(Application.class, resideInAPackage("..moduleC..")).verify();
	}
}
