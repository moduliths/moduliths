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
package com.acme.myproject.moduleC;

import static org.assertj.core.api.Assertions.*;

import de.olivergierke.moduliths.test.TestUtils;
import de.olivergierke.moduliths.test.ModuleTest.BootstrapMode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.acme.myproject.NonVerifyingModuleTest;
import com.acme.myproject.moduleA.ServiceComponentA;
import com.acme.myproject.moduleB.ServiceComponentB;
import com.acme.myproject.moduleC.ModuleCTest.FailsStandalone;
import com.acme.myproject.moduleC.ModuleCTest.FailsWithDirectDependency;
import com.acme.myproject.moduleC.ModuleCTest.SucceedsWithAllDependencies;
import com.acme.myproject.moduleC.ModuleCTest.SucceedsWithDirectDependencyPlusItsDependenciesMocks;

/**
 * @author Oliver Gierke
 */
@RunWith(Suite.class)
@SuiteClasses({ //
		FailsStandalone.class, //
		FailsWithDirectDependency.class, //
		SucceedsWithDirectDependencyPlusItsDependenciesMocks.class, //
		SucceedsWithAllDependencies.class //
})
public class ModuleCTest {

	@NonVerifyingModuleTest
	public static class FailsStandalone {

		@Test
		public void failsStandalone() {
			TestUtils.assertDependencyMissing(FailsStandalone.class, ServiceComponentB.class);
		}
	}

	@NonVerifyingModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)
	public static class FailsWithDirectDependency {

		@Test
		public void failsWithDirectDependency() {
			TestUtils.assertDependencyMissing(FailsWithDirectDependency.class, ServiceComponentA.class);
		}
	}

	@RunWith(SpringRunner.class)
	@NonVerifyingModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)
	public static class SucceedsWithDirectDependencyPlusItsDependenciesMocks {

		@MockBean ServiceComponentA serviceComponentA;

		@Test
		public void bootstrapsContext() {
			assertThat(serviceComponentA).isNotNull();
		}
	}

	@RunWith(SpringRunner.class)
	@NonVerifyingModuleTest(BootstrapMode.ALL_DEPENDENCIES)
	public static class SucceedsWithAllDependencies {

		@Autowired ServiceComponentA serviceComponentA;
		@Autowired ServiceComponentB serviceComponentB;

		@Test
		public void bootstrapsContext() {
			assertThat(serviceComponentA).isNotNull();
			assertThat(serviceComponentB).isNotNull();
		}
	}
}
