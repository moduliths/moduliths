/*
 * Copyright 2019 the original author or authors.
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
package com.acme.myproject.fieldinjected;

import static org.assertj.core.api.Assertions.*;

import de.olivergierke.moduliths.model.Modules;
import de.olivergierke.moduliths.test.ModuleTestExecution;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.acme.myproject.NonVerifyingModuleTest;
import com.acme.myproject.moduleA.ServiceComponentA;

/**
 * Integration tests to verify field injection is rejected.
 *
 * @author Oliver Drotbohm
 */
@RunWith(SpringRunner.class)
@NonVerifyingModuleTest
public class FieldInjectedIntegrationTest {

	@Autowired ModuleTestExecution execution;

	@MockBean ServiceComponentA dependency;

	@Test // #52
	public void rejectsFieldInjection() {

		Modules modules = execution.getModules();

		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> execution.getModule().verifyDependencies(modules)) //
				.withMessageContaining("field injection") //
				.withMessageContaining("WithFieldInjection.a"); // offending field
	}
}
