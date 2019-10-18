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
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.AbstractApplicationEventMulticaster;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.events.CompletableEventPublication;
import org.springframework.events.EventPublication;
import org.springframework.events.EventPublicationRegistry;
import org.springframework.events.PublicationTargetIdentifier;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * An {@link ApplicationEventMulticaster} to register {@link EventPublication}s in an {@link EventPublicationRegistry}
 * so that potentially failing transactional event listeners can get re-invoked upon application restart or via a
 * schedule.
 * <p>
 * Republication is handled in {@link #afterSingletonsInstantiated()} inspecting the {@link EventPublicationRegistry}
 * for incomplete publications and
 *
 * @author Oliver Drotbohm
 * @see CompletionRegisteringBeanPostProcessor
 */
@Slf4j
@RequiredArgsConstructor
public class PersistentApplicationEventMulticaster extends AbstractApplicationEventMulticaster
		implements SmartInitializingSingleton {

	private final @NonNull Supplier<EventPublicationRegistry> registry;

	private static final Map<Class<?>, Boolean> TX_EVENT_LISTENERS = new ConcurrentReferenceHashMap<>();
	private static final Field LISTENER_METHOD_FIELD;

	static {

		LISTENER_METHOD_FIELD = ReflectionUtils.findField(ApplicationListenerMethodAdapter.class, "method");
		ReflectionUtils.makeAccessible(LISTENER_METHOD_FIELD);
	}

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

		List<ApplicationListener<?>> transactionalListeners = listeners.stream() //
				.filter(PersistentApplicationEventMulticaster::isTransactionalApplicationEventListener) //
				.collect(Collectors.toList());

		if (!transactionalListeners.isEmpty()) {

			Object eventToPersist = getEventToPersist(event);

			registry.get().store(eventToPersist, transactionalListeners);
		}

		for (ApplicationListener listener : listeners) {

			EventPublication publication = CompletableEventPublication.of(event,
					PublicationTargetIdentifier.forListener(listener));

			executeListenerWithCompletion(publication, listener);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.SmartInitializingSingleton#afterSingletonsInstantiated()
	 */
	@Override
	public void afterSingletonsInstantiated() {

		for (EventPublication publication : registry.get().findIncompletePublications()) {
			invokeTargetListener(publication);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void invokeTargetListener(EventPublication publication) {

		for (ApplicationListener listener : getApplicationListeners()) {

			if (publication.isIdentifiedBy(PublicationTargetIdentifier.forListener(listener))) {

				executeListenerWithCompletion(publication, listener);
				return;
			}
		}

		log.debug("Listener {} not found!", publication.getTargetIdentifier());
	}

	private void executeListenerWithCompletion(EventPublication publication,
			ApplicationListener<ApplicationEvent> listener) {

		try {

			listener.onApplicationEvent(publication.getApplicationEvent());
			registry.get().markCompleted(publication);

		} catch (Exception e) {
			// Log
		}
	}

	private static boolean isTransactionalApplicationEventListener(ApplicationListener<?> listener) {

		Class<?> targetClass = AopUtils.getTargetClass(listener);

		return TX_EVENT_LISTENERS.computeIfAbsent(targetClass, it -> {

			if (!ApplicationListenerMethodAdapter.class.isAssignableFrom(targetClass)) {
				return false;
			}

			Method method = (Method) ReflectionUtils.getField(LISTENER_METHOD_FIELD, listener);

			return AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class);
		});
	}

	private static Object getEventToPersist(ApplicationEvent event) {

		return PayloadApplicationEvent.class.isInstance(event) //
				? ((PayloadApplicationEvent<?>) event).getPayload() //
				: event;
	}
}
