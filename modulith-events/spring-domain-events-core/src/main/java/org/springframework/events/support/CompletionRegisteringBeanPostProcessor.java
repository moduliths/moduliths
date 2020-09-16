/*
 * Copyright 2017-2019 the original author or authors.
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
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.events.EventPublicationRegistry;
import org.springframework.events.PublicationTargetIdentifier;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * {@link BeanPostProcessor} that will add a
 * {@link org.springframework.events.support.CompletionRegisteringBeanPostProcessor.ProxyCreatingMethodCallback.CompletionRegisteringMethodInterceptor}
 * to the bean in case it carries a {@link TransactionalEventListener} annotation so that the successful invocation of
 * those methods mark the event publication to those listeners as completed.
 *
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
public class CompletionRegisteringBeanPostProcessor implements BeanPostProcessor {

	private final @NonNull Supplier<EventPublicationRegistry> store;

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

		ReflectionUtils.doWithMethods(AopProxyUtils.ultimateTargetClass(bean), callback);

		return callback.methodFound ? callback.getBean() : bean;

	}

	/**
	 * Method callback to find a {@link TransactionalEventListener} method and creating a proxy including an
	 * {@link CompletionRegisteringBeanPostProcessor} for it or adding the latter to the already existing advisor chain.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class ProxyCreatingMethodCallback implements MethodCallback {

		private final CompletionRegisteringMethodInterceptor interceptor;

		private @Getter Object bean;
		private boolean methodFound = false;

		/**
		 * Creates a new {@link ProxyCreatingMethodCallback} for the given {@link EventPublicationRegistry} and bean
		 * instance.
		 *
		 * @param registry must not be {@literal null}.
		 * @param bean must not be {@literal null}.
		 */
		public ProxyCreatingMethodCallback(Supplier<EventPublicationRegistry> registry, Object bean) {

			Assert.notNull(registry, "EventPublicationRegistry must not be null!");
			Assert.notNull(bean, "Bean must not be null!");

			this.bean = bean;
			this.interceptor = new CompletionRegisteringMethodInterceptor(registry);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.util.ReflectionUtils.MethodCallback#doWith(java.lang.reflect.Method)
		 */
		@Override
		public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

			if (methodFound || !CompletionRegisteringMethodInterceptor.isCompletingMethod(method)) {
				return;
			}

			this.methodFound = true;

			bean = createCompletionRegisteringProxy(bean);
		}

		private Object createCompletionRegisteringProxy(Object bean) {

			if (bean instanceof Advised) {

				Advised advised = (Advised) bean;
				advised.addAdvice(advised.getAdvisors().length, interceptor);

				return bean;
			}

			ProxyFactory factory = new ProxyFactory(bean);
			factory.setProxyTargetClass(true);
			factory.addAdvice(interceptor);

			return factory.getProxy();
		}

		/**
		 * {@link MethodInterceptor} to trigger the completion of an event publication after a transaction event listener
		 * method has been completed successfully.
		 *
		 * @author Oliver Drotbohm
		 */
		@Slf4j
		@RequiredArgsConstructor
		private static class CompletionRegisteringMethodInterceptor implements MethodInterceptor, Ordered {

			private static final Map<Method, Boolean> COMPLETING_METHOD = new ConcurrentReferenceHashMap<>();

			private final @NonNull Supplier<EventPublicationRegistry> registry;

			/*
			 * (non-Javadoc)
			 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
			 */
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {

				Object result = null;

				try {
					result = invocation.proceed();
				} catch (Exception o_O) {
					log.debug("Invocation of listener {} failed. Leaving event publication uncompleted.", invocation.getMethod());
					throw o_O;
				}

				Method method = invocation.getMethod();

				// Mark publication complete if the method is a transactional event listener.
				if (!isCompletingMethod(method)) {
					return result;
				}

				PublicationTargetIdentifier identifier = PublicationTargetIdentifier.forMethod(method);
				registry.get().markCompleted(invocation.getArguments()[0], identifier);

				return result;
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.core.Ordered#getOrder()
			 */
			@Override
			public int getOrder() {
				return Ordered.HIGHEST_PRECEDENCE - 10;
			}

			/**
			 * Returns whether the given method is one that requires publication completion.
			 *
			 * @param method must not be {@literal null}.
			 * @return
			 */
			static boolean isCompletingMethod(Method method) {

				Assert.notNull(method, "Method must not be null!");

				return COMPLETING_METHOD.computeIfAbsent(method, it -> {

					TransactionalEventListener annotation = AnnotatedElementUtils.getMergedAnnotation(method,
							TransactionalEventListener.class);

					return annotation == null ? false : annotation.phase().equals(TransactionPhase.AFTER_COMMIT);
				});
			}
		}

	}
}
