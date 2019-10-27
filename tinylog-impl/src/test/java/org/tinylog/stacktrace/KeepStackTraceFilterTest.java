/*
 * Copyright 2019 Martin Winandy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.tinylog.stacktrace;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KeepStackTraceFilter}.
 */
public final class KeepStackTraceFilterTest {

	/**
	 * Verifies that the class name of the source throwable will be looped through.
	 */
	@Test
	public void keepClassName() {
		RuntimeException exception = new RuntimeException();

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.emptyList());
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));
		
		assertThat(data.getClassName()).isEqualTo(RuntimeException.class.getName());
	}

	/**
	 * Verifies that the message of the source throwable will be looped through.
	 */
	@Test
	public void keepMessage() {
		RuntimeException exception = new RuntimeException("Hello World!");

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.emptyList());
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));
		
		assertThat(data.getMessage()).isEqualTo("Hello World!");
	}

	/**
	 * Verifies that a {@code null} cause of the source throwable will be looped through.
	 */
	@Test
	public void loopTroughNullCause() {
		RuntimeException exception = new RuntimeException("Hello World!");

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.emptyList());
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));

		assertThat(data.getCause()).isNull();
	}

	/**
	 * Verifies that an existing cause of the source throwable will be looped through.
	 */
	@Test
	public void keepExistingCause() {
		RuntimeException exception = new RuntimeException("Hello Heaven!", new NullPointerException("Hello Hell!"));

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.emptyList());
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));

		assertThat(data.getCause()).isNotNull();
		assertThat(data.getCause().getClassName()).isEqualTo(NullPointerException.class.getName());
		assertThat(data.getCause().getMessage()).isEqualTo("Hello Hell!");
	}
	
	/**
	 * Verifies that all stack trace elements will be removed, if list of packages and classes is an empty string.
	 */
	@Test
	public void empty() {
		RuntimeException exception = new RuntimeException();

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.emptyList());
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));

		assertThat(data.getStackTrace()).isEmpty();
	}

	/**
	 * Verifies that a single package can be kept on stack trace.
	 */
	@Test
	public void singlePackage() {
		RuntimeException exception = new RuntimeException();

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.singletonList("org.tinylog"));
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));

		assertThat(data.getStackTrace())
			.hasSize(1)
			.allMatch(element -> element.getClassName().startsWith("org.tinylog"));
	}

	/**
	 * Verifies that incomplete package names will be remove all elements from stack trace.
	 */
	@Test
	public void incompletePackage() {
		RuntimeException exception = new RuntimeException();
		
		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.singletonList("o"));
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));

		assertThat(data.getStackTrace()).isEmpty();
	}

	/**
	 * Verifies that multiple packages can be kept on stack trace.
	 */
	@Test
	public void multiplePackages() {
		RuntimeException exception = new RuntimeException();
		int elements = exception.getStackTrace().length;

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Arrays.asList("com", "java", "javax", "jdk", "org", "sun"));
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));

		assertThat(data.getStackTrace()).hasSize(elements);
	}

	/**
	 * Verifies that a single class can be kept on stack trace.
	 */
	@Test
	public void singleClass() {
		RuntimeException exception = new RuntimeException();

		KeepStackTraceFilter filter = new KeepStackTraceFilter(Collections.singletonList(KeepStackTraceFilterTest.class.getName()));
		ThrowableData data = filter.filter(new ThrowableWrapper(exception));

		assertThat(data.getStackTrace())
			.hasSize(1)
			.allMatch(element -> element.getClassName().startsWith(KeepStackTraceFilterTest.class.getName()));
	}

}
