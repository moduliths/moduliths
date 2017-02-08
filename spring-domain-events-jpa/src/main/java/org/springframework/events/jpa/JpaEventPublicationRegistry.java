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
package org.springframework.events.jpa;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationListener;
import org.springframework.events.CompletableEventPublication;
import org.springframework.events.EventPublication;
import org.springframework.events.EventPublicationRegistry;
import org.springframework.events.EventSerializer;
import org.springframework.events.PublicationTargetIdentifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * JPA based {@link EventPublicationRegistry}.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
class JpaEventPublicationRegistry implements EventPublicationRegistry {

	private final @NonNull JpaEventPublicationRepository events;
	private final @NonNull EventSerializer serializer;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.events.EventPublicationRegistry#store(java.lang.Object, java.util.Collection)
	 */
	@Override
	public void store(Object event, Collection<ApplicationListener<?>> listeners) {

		listeners.stream()//
				.map(it -> PublicationTargetIdentifier.forListener(it))//
				.map(it -> CompletableEventPublication.of(event, it))//
				.map(this::map)//
				.forEach(it -> events.save(it));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.events.EventPublicationRegistry#findIncompletePublications()
	 */
	@Override
	public Iterable<EventPublication> findIncompletePublications() {

		List<EventPublication> result = events.findByCompletionDateIsNull().stream()//
				.map(it -> JpaEventPublicationAdapter.of(it, serializer))//
				.collect(Collectors.toList());

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.events.EventPublicationRegistry#markCompleted(java.lang.Object, org.springframework.events.ListenerId)
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markCompleted(Object event, PublicationTargetIdentifier listener) {

		Assert.notNull(event, "Domain event must not be null!");
		Assert.notNull(listener, "Listener identifier must not be null!");

		events.findBySerializedEventAndListenerId(serializer.serialize(event), listener.toString())//
				.ifPresent(it -> events.save(it.markCompleted()));
	}

	private JpaEventPublication map(EventPublication publication) {

		return JpaEventPublication.builder()//
				.serializedEvent(serializer.serialize(publication.getEvent()).toString())//
				.publicationDate(publication.getPublicationDate())//
				.listenerId(publication.getTargetIdentifier().toString())//
				.build();
	}

	@EqualsAndHashCode
	@RequiredArgsConstructor(staticName = "of")
	static class JpaEventPublicationAdapter implements EventPublication {

		private final JpaEventPublication publication;
		private final EventSerializer serializer;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.events.EventPublication#getEvent()
		 */
		@Override
		public Object getEvent() {
			return serializer.deserialize(publication.getSerializedEvent(), publication.getEventType());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.events.EventPublication#getListenerId()
		 */
		@Override
		public PublicationTargetIdentifier getTargetIdentifier() {
			return PublicationTargetIdentifier.of(publication.getListenerId());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.events.EventPublication#getPublicationDate()
		 */
		@Override
		public LocalDateTime getPublicationDate() {
			return publication.getPublicationDate();
		}
	}
}
