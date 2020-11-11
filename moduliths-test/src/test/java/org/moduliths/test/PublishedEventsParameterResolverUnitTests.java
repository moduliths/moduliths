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
package org.moduliths.test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for PublishedEventsParameterResolver.
 *
 * @author Oliver Drotbohm
 */
class PublishedEventsParameterResolverUnitTests {

	AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test // #98
	void supportsPublishedEventsType() throws Exception {

		PublishedEventsParameterResolver resolver = new PublishedEventsParameterResolver();

		assertThat(resolver.supportsParameter(getParameterContext(PublishedEvents.class), null)).isTrue();
		assertThat(resolver.supportsParameter(getParameterContext(Object.class), null)).isFalse();
	}

	private static ParameterContext getParameterContext(Class<?> type) {

		Method method = ReflectionUtils.findMethod(Methods.class, "with", type);

		ParameterContext context = mock(ParameterContext.class);
		doReturn(method.getParameters()[0]).when(context).getParameter();

		return context;
	}

	interface Methods {

		void with(PublishedEvents events);

		void with(Object object);
	}
}
