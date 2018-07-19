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
package com.acme.myproject.moduleB;

import static org.assertj.core.api.Assertions.*;

import de.olivergierke.moduliths.test.TestUtils;
import de.olivergierke.moduliths.test.ModuleTest.BootstrapMode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.internal.creation.bytebuddy.MockAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.acme.myproject.NonVerifyingModuleTest;
import com.acme.myproject.moduleA.ServiceComponentA;
import com.acme.myproject.moduleB.ModuleBTest.TestWithMocks;
import com.acme.myproject.moduleB.ModuleBTest.TestWithoutMocks;
import com.acme.myproject.moduleB.internal.InternalComponentB;

/**
 * @author Oliver Gierke
 */
@RunWith(Suite.class)
@SuiteClasses({ TestWithoutMocks.class, TestWithMocks.class })
public class ModuleBTest {

	@NonVerifyingModuleTest
	public static class TestWithoutMocks {

		@Autowired ServiceComponentB serviceComponentB;

		@Test
		public void failsToStartBecauseServiceComponentAIsMissing() throws Exception {
			TestUtils.assertDependencyMissing(TestWithoutMocks.class, ServiceComponentA.class);
		}
	}

	@NonVerifyingModuleTest
	@RunWith(SpringRunner.class)
	public static class TestWithMocks {

		@Autowired ApplicationContext context;
		@MockBean ServiceComponentA serviceComponentA;

		@Test
		public void bootstrapsModuleB() {

			context.getBean(ServiceComponentB.class);

			assertThat(context.getBean(ServiceComponentA.class)).isInstanceOf(MockAccess.class);
		}

		@Test
		public void considersNestedPackagePartOfTheModuleByDefault() {
			context.getBean(InternalComponentB.class);
		}

		@Test // #4
		public void tweaksAutoConfigurationPackageToModulePackage() {

			assertThat(AutoConfigurationPackages.get(context)) //
					.containsExactly(getClass().getPackage().getName());
		}
	}

	@RunWith(SpringRunner.class)
	@NonVerifyingModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)
	public static class TestWithUpstreamModule {

		@Autowired ServiceComponentA componentA;
		@Autowired ServiceComponentB componentB;

		@Test
		public void bootstrapsContext() {}
	}
}
