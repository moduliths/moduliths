/*
 * Copyright 2019-2020 the original author or authors.
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link PublishedEvents}.
 *
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DefaultPublishedEvents implements PublishedEvents {

	private final Supplier<ApplicationEvents> delegate;

	/**
	 * Creates a new {@link DefaultPublishedEvents} instance with the given events.
	 *
	 * @param events must not be {@literal null}.
	 */
	DefaultPublishedEvents(Collection<? extends Object> events) {

		Assert.notNull(events, "Events must not be null!");

		this.delegate = () -> CollectionApplicationEvents.of(events);
	}

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.test.PublishedEvents#ofType(java.lang.Class)
	 */
	@Override
	public <T> TypedPublishedEvents<T> ofType(Class<T> type) {
		return SimpleTypedPublishedEvents.of(getEvents(type));
	}

	private <T> Stream<T> getEvents(Class<T> type) {

		ApplicationEvents events = delegate.get();

		return events == null ? Stream.empty() : events.stream(type);
	}

	@RequiredArgsConstructor(staticName = "of")
	private static class CollectionApplicationEvents implements ApplicationEvents {

		private final Collection<? extends Object> events;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.test.context.junit.jupiter.ApplicationEvents#stream()
		 */
		@Override
		public Stream<ApplicationEvent> stream() {

			return events.stream()
					.map(it -> ApplicationEvent.class.isInstance(it) //
							? ApplicationEvent.class.cast(it) //
							: new PayloadApplicationEvent<>(this, it));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.test.context.junit.jupiter.ApplicationEvents#stream(java.lang.Class)
		 */
		@Override
		public <T> Stream<T> stream(Class<T> type) {

			return events.stream() //
					.map(it -> unwrapPayloadEvent(it)) //
					.filter(type::isInstance) //
					.map(type::cast);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.test.context.event.ApplicationEvents#clear()
		 */
		@Override
		public void clear() {
			this.events.clear();
		}

		private static Object unwrapPayloadEvent(Object source) {

			return PayloadApplicationEvent.class.isInstance(source) //
					? ((PayloadApplicationEvent<?>) source).getPayload() //
					: source;
		}
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

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.test.PublishedEvents.TypedPublishedEvents#matchingMapped(java.util.function.Function, java.util.function.Predicate)
		 */
		@Override
		public <S> TypedPublishedEvents<T> matchingMapped(Function<T, S> mapper, Predicate<? super S> predicate) {

			return SimpleTypedPublishedEvents.of(events.stream().flatMap(it -> {

				S mapped = mapper.apply(it);

				return predicate.test(mapped) ? Stream.of(it) : Stream.empty();

			}));
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

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return events.toString();
		}
	}

}
