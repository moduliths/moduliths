/*
 * Copyright 2020 the original author or authors.
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
package com.acme.withatbean;

import org.moduliths.Event;

/**
 * @author Oliver Drotbohm
 */
@SuppressWarnings("deprecation")
public class TestEvents {

	@Event
	public static class DomainEvent {}

	@org.jddd.event.annotation.DomainEvent
	public static class JDddAnnotated {}

	public static class JDddImplementing implements org.jddd.event.types.DomainEvent {}
}
