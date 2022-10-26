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
package org.moduliths.events.jpa;

import org.junit.jupiter.api.Test;
import org.moduliths.events.EventPublication;
import org.moduliths.events.EventSerializer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JpaEventPublicationRegistry}.
 *
 * @author OGAWA, Takeshi
 */
class JpaEventPublicationRegistryUnitTests {

    JpaEventPublicationRepository repository = mock(JpaEventPublicationRepository.class);
    EventSerializer serializer = mock(EventSerializer.class);
    JpaEventPublicationRegistry registry = new JpaEventPublicationRegistry(repository, serializer);

    @Test
    void findIncompletePublications() {
        String listenerId = "listener";
        LocalDateTime date = LocalDateTime.now();
        doReturn(Arrays.asList(//
                JpaEventPublication.of(date.withHour(3).toInstant(ZoneOffset.UTC), listenerId, "", Object.class), //
                JpaEventPublication.of(date.withHour(0).toInstant(ZoneOffset.UTC), listenerId, "", Object.class), //
                JpaEventPublication.of(date.withHour(2).toInstant(ZoneOffset.UTC), listenerId, "", Object.class) //
        )).when(repository).findByCompletionDateIsNull();
        Iterable<EventPublication> publications = registry.findIncompletePublications();
        assertThat(publications).extracting("publicationDate").isSorted();
    }
}
