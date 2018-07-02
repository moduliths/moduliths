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
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test cases to verify the validity of the overall modulith rules
 *
 * @author Oliver Gierke
 */
public class ModulithTest {

	@Test
	public void verifyModules() {
		assertThatThrownBy(() -> Modules.of(Application.class).verify())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(
						String.format("Module 'moduleC' depends on non-exposed type %s within module 'moduleB'",
								InternalComponentB.class.getName()))
				.hasMessageContaining(
						String.format("%s.<init>(%s) declares parameter %s",
								InvalidComponent.class.getName(), InternalComponentB.class.getName(), InternalComponentB.class.getName()));
	}

	@Test
	public void verifyModulesWithoutInvalid() {
		Modules.of(Application.class, resideInAPackage("..moduleC..")).verify();
	}
}
