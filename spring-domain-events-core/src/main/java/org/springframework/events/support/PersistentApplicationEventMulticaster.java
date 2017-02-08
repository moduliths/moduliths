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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.AbstractApplicationEventMulticaster;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.events.EventPublicationRegistry;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class PersistentApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

	private final @NonNull ObjectFactory<EventPublicationRegistry> registry;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.event.ApplicationEventMulticaster#multicastEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void multicastEvent(ApplicationEvent event) {
		multicastEvent(event, ResolvableType.forInstance(event));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.event.ApplicationEventMulticaster#multicastEvent(org.springframework.context.ApplicationEvent, org.springframework.core.ResolvableType)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {

		ResolvableType type = eventType == null ? ResolvableType.forInstance(event) : eventType;
		Collection<ApplicationListener<?>> listeners = getApplicationListeners(event, type);

		if (listeners.isEmpty()) {
			return;
		}

		List<ApplicationListener<?>> transactionalListeners = listeners.stream()//
				.filter(PersistentApplicationEventMulticaster::isTransactionalApplicationEventListener)//
				.collect(Collectors.toList());

		if (!transactionalListeners.isEmpty()) {

			Object eventToPersist = getEventToPersist(event);

			registry.getObject().store(eventToPersist, transactionalListeners);
			// EventStore.persist(eventThis)
			// SpringMVC Controller Atom Feed
		}

		for (ApplicationListener listener : listeners) {
			listener.onApplicationEvent(event);
		}
	}

	private static boolean isTransactionalApplicationEventListener(ApplicationListener<?> listener) {

		Class<?> targetClass = AopUtils.getTargetClass(listener);

		if (!ApplicationListenerMethodAdapter.class.isAssignableFrom(targetClass)) {
			return false;
		}

		Field field = ReflectionUtils.findField(ApplicationListenerMethodAdapter.class, "method");
		ReflectionUtils.makeAccessible(field);
		Method method = (Method) ReflectionUtils.getField(field, listener);

		return AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class);
	}

	private static Object getEventToPersist(ApplicationEvent event) {
		return PayloadApplicationEvent.class.isInstance(event) ? ((PayloadApplicationEvent<?>) event).getPayload() : event;
	}
}
