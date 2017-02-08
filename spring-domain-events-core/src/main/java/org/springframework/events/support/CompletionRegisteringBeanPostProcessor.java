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
package org.springframework.events.support;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.events.EventPublicationRegistry;
import org.springframework.events.PublicationTargetIdentifier;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class CompletionRegisteringBeanPostProcessor implements BeanPostProcessor {

	private final @NonNull ObjectFactory<EventPublicationRegistry> store;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		ProxyCreatingMethodCallback callback = new ProxyCreatingMethodCallback(store, bean);

		ReflectionUtils.doWithMethods(bean.getClass(), callback);

		return callback.getBean() == null ? bean : callback.getBean();

	}

	private static class ProxyCreatingMethodCallback implements MethodCallback {

		private final ObjectFactory<EventPublicationRegistry> registry;

		private @Getter Object bean;
		private boolean methodFound = false;

		public ProxyCreatingMethodCallback(ObjectFactory<EventPublicationRegistry> registry, Object bean) {

			Assert.notNull(registry, "EventPublicationRegistry must not be null!");

			this.bean = bean;
			this.registry = registry;
		}

		@Override
		public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

			if (methodFound) {
				return;
			}

			TransactionalEventListener listener = AnnotatedElementUtils.findMergedAnnotation(method,
					TransactionalEventListener.class);

			if (listener == null) {
				return;
			}

			bean = createCompletionRegisteringProxy(bean);
		}

		private Object createCompletionRegisteringProxy(Object bean) {

			ProxyFactory factory = new ProxyFactory(bean);
			factory.addAdvice(new CompletionRegisteringMethodInterceptor(registry, bean));

			return factory.getProxy();
		}

		@RequiredArgsConstructor
		private static class CompletionRegisteringMethodInterceptor implements MethodInterceptor {

			private final @NonNull ObjectFactory<EventPublicationRegistry> eventStore;
			private final @NonNull Object listener;

			/* 
			 * (non-Javadoc)
			 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
			 */
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {

				Object result = invocation.proceed();

				eventStore.getObject().markCompleted(invocation.getArguments()[0],
						PublicationTargetIdentifier.forMethod(invocation.getMethod()));

				return result;
			}
		}
	}
}
