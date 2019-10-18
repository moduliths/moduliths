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
package example.events;

import static org.assertj.core.api.Assertions.*;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.events.EventPublication;
import org.springframework.events.EventPublicationRegistry;
import org.springframework.events.PublicationTargetIdentifier;
import org.springframework.events.config.EnablePersistentDomainEvents;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oliver Gierke
 */
class PersistentDomainEventIntegrationTest {

	@Test
	void exposesEventPublicationForFailedListener() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ApplicationConfiguration.class, InfrastructureConfiguration.class);
		context.refresh();

		Method method = ReflectionUtils.findMethod(SecondTxEventListener.class, "on", DomainEvent.class);

		try {

			context.getBean(Client.class).method();

		} catch (Throwable e) {

			System.out.println(e);

		} finally {

			Iterable<EventPublication> eventsToBePublished = context.getBean(EventPublicationRegistry.class)
					.findIncompletePublications();

			assertThat(eventsToBePublished).hasSize(1);
			assertThat(eventsToBePublished).allSatisfy(it -> {
				assertThat(it.getTargetIdentifier()).isEqualTo(PublicationTargetIdentifier.forMethod(method));
			});

			context.close();
		}
	}

	@Configuration
	@EnablePersistentDomainEvents
	static class ApplicationConfiguration {

		@Bean
		FirstTxEventListener first() {
			return new FirstTxEventListener();
		}

		@Bean
		SecondTxEventListener second() {
			return new SecondTxEventListener();
		}

		@Bean
		ThirdTxEventListener third() {
			return new ThirdTxEventListener();
		}

		@Bean
		Client client(ApplicationEventPublisher publisher) {
			return new Client(publisher);
		}
	}

	@RequiredArgsConstructor
	static class Client {

		private final ApplicationEventPublisher publisher;

		@Transactional
		public void method() {
			publisher.publishEvent(new DomainEvent());
		}
	}

	static class DomainEvent {}

	static class FirstTxEventListener {

		boolean invoked = false;

		@TransactionalEventListener
		public void on(DomainEvent event) {
			invoked = true;
		}
	}

	static class SecondTxEventListener {

		@TransactionalEventListener
		public void on(DomainEvent event) {
			throw new IllegalStateException();
		}
	}

	static class ThirdTxEventListener {

		boolean invoked = false;

		@TransactionalEventListener
		public void on(DomainEvent event) {

			invoked = true;
		}
	}
}
