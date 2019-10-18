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
package org.springframework.events;

import java.time.Instant;

/**
 * An event publication.
 *
 * @author Oliver Gierke
 */
public interface EventPublication extends Comparable<EventPublication> {

	/**
	 * Returns the event that is published.
	 *
	 * @return
	 */
	Object getEvent();

	/**
	 * Returns the time the event is published at.
	 *
	 * @return
	 */
	Instant getPublicationDate();

	/**
	 * Returns the identifier of the target that the event is supposed to be published to.
	 *
	 * @return
	 */
	PublicationTargetIdentifier getTargetIdentifier();

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public default int compareTo(EventPublication that) {
		return this.getPublicationDate().compareTo(that.getPublicationDate());
	}
}
