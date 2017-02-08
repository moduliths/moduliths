/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.events;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Identifier for a publication target.
 * 
 * @author Oliver Gierke
 */
@Value
@RequiredArgsConstructor(staticName = "of")
public class PublicationTargetIdentifier {

	String value;

	/**
	 * Creates a {@link PublicationTargetIdentifier} for the given {@link Method}.
	 * 
	 * @param method must not be {@literal null}.
	 * @return
	 */
	public static PublicationTargetIdentifier forMethod(Method method) {

		String typeName = ClassUtils.getUserClass(method.getDeclaringClass()).getName();
		String methodName = method.getName();
		String parameterTypes = StringUtils.arrayToDelimitedString(method.getParameterTypes(), ", ");

		return PublicationTargetIdentifier.of(String.format("%s.%s(%s)", typeName, methodName, parameterTypes));
	}

	/**
	 * Creates a {@link PublicationTargetIdentifier} for the given listener instance.
	 * 
	 * @param listener
	 * @return
	 */
	public static PublicationTargetIdentifier forListener(Object listener) {

		if (listener instanceof ApplicationListenerMethodAdapter) {

			Field field = ReflectionUtils.findField(ApplicationListenerMethodAdapter.class, "method");
			ReflectionUtils.makeAccessible(field);
			Method method = (Method) ReflectionUtils.getField(field, listener);

			return PublicationTargetIdentifier.forMethod(method);
		}

		throw new IllegalStateException("Unsupported listener implementation!");
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return value;
	}
}
