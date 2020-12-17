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
package org.moduliths.docs;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tngtech.archunit.core.domain.JavaMethod;

/**
 * A {@link DocumentationSource} that replaces {@literal {@code …}} or {@literal {@link …}} blocks into inline code
 * references
 *
 * @author Oliver Drotbohm
 * @since 1.1
 */
@RequiredArgsConstructor
class CodeReplacingDocumentationSource implements DocumentationSource {

	private static final Pattern JAVADOC_CODE = Pattern.compile("\\{\\@(?>link|code)\\s(.*)\\}");

	private final DocumentationSource delegate;
	private final InlineCodeSource codeSource;

	/*
	 * (non-Javadoc)
	 * @see org.moduliths.docs.DocumentationSource#getDocumentation(com.tngtech.archunit.core.domain.JavaMethod)
	 */
	@Override
	public Optional<String> getDocumentation(JavaMethod method) {

		return delegate.getDocumentation(method)
				.map(this::replaceJavadocCodeReferences);
	}

	/**
	 * Replaces references to {@literal {@code …}} and {@literal {@link …}} with the inline code representation of the
	 * contained expression.
	 *
	 * @param source must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see CodeReplacingDocumentationSource#getDocumentation(JavaMethod)
	 */
	private String replaceJavadocCodeReferences(String source) {

		Matcher matcher = JAVADOC_CODE.matcher(source);

		while (matcher.find()) {

			String type = matcher.group(1);

			source = source.replace(matcher.group(), codeSource.toInlineCode(type));
		}

		return source;
	}
}
