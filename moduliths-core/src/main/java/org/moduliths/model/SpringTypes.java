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
package org.moduliths.model;

import lombok.experimental.UtilityClass;

/**
 * @author Oliver Drotbohm
 */
@UtilityClass
class SpringTypes {

	private static String BASE_PACKAGE = "org.springframework";

	static String AT_AUTOWIRED = BASE_PACKAGE + ".beans.factory.annotation.Autowired";
	static String AT_BEAN = BASE_PACKAGE + ".context.annotation.Bean";
	static String AT_COMPONENT = BASE_PACKAGE + ".stereotype.Component";
	static String AT_CONFIGURATION = BASE_PACKAGE + ".context.annotation.Configuration";
	static String AT_EVENT_LISTENER = BASE_PACKAGE + ".context.event.EventListener";

	static String AT_SPRING_BOOT_APPLICATION = BASE_PACKAGE + ".boot.autoconfigure.SpringBootApplication";
}
