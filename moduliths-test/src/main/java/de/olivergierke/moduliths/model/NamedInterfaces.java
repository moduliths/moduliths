/*
 * Copyright 2018 the original author or authors.
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
package de.olivergierke.moduliths.model;

import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor(staticName = "of")
public class NamedInterfaces implements Iterable<NamedInterface> {

	private final List<NamedInterface> namedInterfaces;

	public boolean hasExplicitInterfaces() {
		return namedInterfaces.size() > 1 || !namedInterfaces.get(0).isUnnamed();
	}

	public Stream<NamedInterface> stream() {
		return namedInterfaces.stream();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<NamedInterface> iterator() {
		return namedInterfaces.iterator();
	}
}
