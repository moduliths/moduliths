/*
 * Copyright 2019 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;

/**
 * Default implementation of {@link PublishedEvents}.
 *
 * @author Oliver Drotbohm
 */
class DefaultPublishedEvents implements PublishedEvents, ApplicationListener<ApplicationEvent> {

	private List<Object> events = new ArrayList<>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		this.events.add(unwrapPayloadEvent(event));
	}

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.test.PublishedEvents#ofType(java.lang.Class)
	 */
	@Override
	public <T> TypedPublishedEvents<T> ofType(Class<T> type) {

		return SimpleTypedPublishedEvents.of(events.stream()//
				.filter(type::isInstance) //
				.map(type::cast));
	}

	private static Object unwrapPayloadEvent(Object source) {

		return PayloadApplicationEvent.class.isInstance(source) //
				? ((PayloadApplicationEvent<?>) source).getPayload() //
				: source;
	}

	@RequiredArgsConstructor(staticName = "of")
	private static class SimpleTypedPublishedEvents<T> implements TypedPublishedEvents<T> {

		private final List<T> events;

		private static <T> SimpleTypedPublishedEvents<T> of(Stream<T> stream) {
			return new SimpleTypedPublishedEvents<>(stream.collect(Collectors.toList()));
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.test.PublishedEvents.TypedPublishedEvents#ofSubType(java.lang.Class)
		 */
		@Override
		public <S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType) {

			return SimpleTypedPublishedEvents.of(getFilteredEvents(subType::isInstance) //
					.map(subType::cast));

		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.test.PublishedEvents.TypedPublishedEvents#matching(java.util.function.Predicate)
		 */
		@Override
		public TypedPublishedEvents<T> matching(Predicate<? super T> predicate) {
			return SimpleTypedPublishedEvents.of(getFilteredEvents(predicate));
		}

		/**
		 * Returns a {@link Stream} of events filtered by the given {@link Predicate}.
		 *
		 * @param predicate must not be {@literal null}.
		 * @return
		 */
		private Stream<T> getFilteredEvents(Predicate<? super T> predicate) {
			return events.stream().filter(predicate);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<T> iterator() {
			return events.iterator();
		}
	}
}
