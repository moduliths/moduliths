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
package org.moduliths.events.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository to store {@link JpaEventPublication}s.
 * 
 * @author Oliver Gierke
 */
interface JpaEventPublicationRepository extends JpaRepository<JpaEventPublication, UUID> {

	/**
	 * Returns all {@link JpaEventPublication} that have not been completed yet.
	 */
	List<JpaEventPublication> findByCompletionDateIsNull();

	/**
	 * Return the {@link JpaEventPublication} for the given serialized event and listener identifier.
	 * 
	 * @param event must not be {@literal null}.
	 * @param listenerId must not be {@literal null}.
	 * @return
	 */
	@Query("select p from JpaEventPublication p where p.serializedEvent = ?1 and p.listenerId = ?2")
	Optional<JpaEventPublication> findBySerializedEventAndListenerId(Object event, String listenerId);
}
