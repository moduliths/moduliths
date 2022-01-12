/*
 * Copyright 2022 the original author or authors.
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
package org.moduliths.observability.autoconfigure;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.moduliths.observability.ApplicationRuntime;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
class SpringBootApplicationRuntime implements ApplicationRuntime {

	private static final Map<String, Boolean> APPLICATION_CLASSES = new ConcurrentHashMap<>();

	private final ApplicationContext context;

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.observability.ApplicationRuntime#getId()
	 */
	@Override
	public String getId() {
		return context.getId();
	}

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.observability.ApplicationRuntime#getApplicationClass()
	 */
	@Override
	public Class<?> getMainApplicationClass() {

		String[] mainBeanNames = context.getBeanNamesForAnnotation(SpringBootApplication.class);

		return context.getType(mainBeanNames[0]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.observability.ApplicationRuntime#getApplicationPackages()
	 */
	@Override
	public List<String> getApplicationPackages() {
		return AutoConfigurationPackages.get(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.observability.ApplicationRuntime#getBeanUserClass(java.lang.Object, java.lang.String)
	 */
	@Override
	public Class<?> getUserClass(Object bean, String beanName) {

		Class<?> targetClass = ClassUtils.getUserClass(bean);

		return context.containsBean(beanName) ? context.getType(beanName) : targetClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.observability.ApplicationRuntime#isApplicationClass(java.lang.Class)
	 */
	@Override
	public boolean isApplicationClass(Class<?> type) {

		return APPLICATION_CLASSES.computeIfAbsent(type.getName(),
				it -> {
					return it.startsWith(getMainApplicationClass().getPackage().getName())
							|| getApplicationPackages().stream().anyMatch(pkg -> it.startsWith(pkg));
				});
	}
}
