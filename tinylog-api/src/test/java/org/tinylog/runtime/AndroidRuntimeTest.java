/*
 * Copyright 2016 Martin Winandy
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

package org.tinylog.runtime;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Locale;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;

import android.os.Process;

import dalvik.system.VMStack;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.tinylog.Logger;
import org.tinylog.rules.SystemStreamCollector;
import org.tinylog.util.SimpleTimestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.tinylog.assertions.Assertions.assertThat;

/**
 * Tests for {@link AndroidRuntime}.
 */
@RunWith(PowerMockRunner.class)
public final class AndroidRuntimeTest {

	/**
	 * Redirects and collects system output streams.
	 */
	@Rule
	public final SystemStreamCollector systemStream = new SystemStreamCollector(true);

	/**
	 * Prepares {@link VMStack} for testing. The method {@code fillStackTraceElements()} is only at runtime on Android
	 * available, but not part of the provided API. Therefore this method has to be added dynamically for testing.
	 *
	 * @throws Exception
	 *             Failed to add {@code fillStackTraceElements()} method
	 */
	@BeforeClass
	public static void prepareVmStack() throws Exception {
		ClassPool classPool = ClassPool.getDefault();
		classPool.appendSystemPath();

		CtClass vmStack = classPool.get("dalvik.system.VMStack"); // Raw name required to prevent preloading of class
		CtClass androidRuntimeTest = classPool.get(AndroidRuntimeTest.class.getName());

		vmStack.addMethod(CtNewMethod.copy(androidRuntimeTest.getDeclaredMethod("fillStackTraceElements"), vmStack, null));
		vmStack.toClass();
	}

	/**
	 * Verifies that Android's logcat writer will be returned as default writer.
	 */
	@Test
	public void defaultWriter() {
		assertThat(new AndroidRuntime().getDefaultWriter()).isEqualTo("logcat");
	}

	/**
	 * Verifies that the process ID will be returned.
	 */
	@Test
	@PrepareForTest(Process.class)
	public void processId() {
		mockStatic(Process.class);
		when(Process.myPid()).thenReturn(123);

		assertThat(new AndroidRuntime().getProcessId()).isEqualTo(123);
	}

	/**
	 * Verifies that the fully-qualified class name of a caller will be returned correctly, if depth in stack trace is
	 * defined as index.
	 */
	@Test
	public void callerClassNameByIndex() {
		AndroidRuntime runtime = new AndroidRuntime();

		Method method = Whitebox.getMethod(AndroidRuntimeTest.class, "fillStackTraceElements", Thread.class, StackTraceElement[].class);
		Whitebox.setInternalState(runtime, Method.class, method);
		Whitebox.setInternalState(runtime, int.class, 5);

		assertThat(runtime.getCallerClassName(1)).isEqualTo(AndroidRuntimeTest.class.getName());
	}

	/**
	 * Verifies that the fully-qualified class name of a caller will be returned correctly, if successor in stack trace
	 * is defined.
	 */
	@Test
	public void callerClassNameBySuccessor() {
		AndroidRuntime runtime = new AndroidRuntime();

		Method method = Whitebox.getMethod(AndroidRuntimeTest.class, "fillStackTraceElements", Thread.class, StackTraceElement[].class);
		Whitebox.setInternalState(runtime, Method.class, method);
		Whitebox.setInternalState(runtime, int.class, 5);

		assertThat(runtime.getCallerClassName(AndroidRuntime.class.getName())).isEqualTo(AndroidRuntimeTest.class.getName());
	}

	/**
	 * Verifies that the complete stack trace element of a caller will be returned correctly, if depth in stack trace is
	 * defined as index.
	 */
	@Test
	public void callerStackTraceElementByIndex() {
		AndroidRuntime runtime = new AndroidRuntime();

		Method method = Whitebox.getMethod(AndroidRuntimeTest.class, "fillStackTraceElements", Thread.class, StackTraceElement[].class);
		Whitebox.setInternalState(runtime, Method.class, method);
		Whitebox.setInternalState(runtime, int.class, 5);

		assertThat(runtime.getCallerStackTraceElement(1)).isEqualTo(new Throwable().getStackTrace()[0]);
	}

	/**
	 * Verifies that the complete stack trace element of a caller will be returned correctly, if successor in stack
	 * trace is defined.
	 */
	@Test
	public void callerStackTraceElementBySuccessor() {
		AndroidRuntime runtime = new AndroidRuntime();

		Method method = Whitebox.getMethod(AndroidRuntimeTest.class, "fillStackTraceElements", Thread.class, StackTraceElement[].class);
		Whitebox.setInternalState(runtime, Method.class, method);
		Whitebox.setInternalState(runtime, int.class, 5);

		assertThat(runtime.getCallerStackTraceElement(AndroidRuntime.class.getName())).isEqualTo(new Throwable().getStackTrace()[0]);
	}

	/**
	 * Verifies that an exception will be thrown, if stack trace does not contain the expected successor.
	 */
	@Test
	public void missingSuccessorForCallerStackTraceElement() {
		AndroidRuntime runtime = new AndroidRuntime();

		Method method = Whitebox.getMethod(AndroidRuntimeTest.class, "fillStackTraceElements", Thread.class, StackTraceElement[].class);
		Whitebox.setInternalState(runtime, Method.class, method);
		Whitebox.setInternalState(runtime, int.class, 5);

		assertThatThrownBy(() -> runtime.getCallerStackTraceElement(Logger.class.getName()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(Logger.class.getName());
	}

	/**
	 * Verifies that the complete stack trace element of a caller can be returned, if the method
	 * {@code fillStackTraceElements()} of {@link VMStack} is not available.
	 */
	@Test
	public void missingStackTraceElementsFiller() {
		AndroidRuntime runtime = new AndroidRuntime();
		Whitebox.setInternalState(runtime, Method.class, (Object) null);
		Whitebox.setInternalState(runtime, int.class, -1);

		assertThat(new AndroidRuntime().getCallerStackTraceElement(1)).isEqualTo(new Throwable().getStackTrace()[0]);
		assertThat(runtime.getCallerStackTraceElement(AndroidRuntime.class.getName())).isEqualTo(new Throwable().getStackTrace()[0]);
	}

	/**
	 * Verifies that the complete stack trace element of a caller can be returned, if the method
	 * {@code fillStackTraceElements()} of {@link VMStack} is not accessible.
	 */
	@Test
	public void notAccessibleStackTraceElementsFiller() {
		AndroidRuntime runtime = new AndroidRuntime();

		Method method = Whitebox.getMethod(AndroidRuntimeTest.class, "fillStackTraceElements", Thread.class, StackTraceElement[].class);
		method.setAccessible(false);
		Whitebox.setInternalState(runtime, Method.class, method);
		Whitebox.setInternalState(runtime, int.class, 5);

		assertThat(runtime.getCallerStackTraceElement(1)).isEqualTo(new Throwable().getStackTrace()[0]);
		assertThat(systemStream.consumeErrorOutput())
			.containsOnlyOnce("ERROR")
			.containsOnlyOnce(IllegalAccessException.class.getName());
	}

	/**
	 * Verifies that the complete stack trace element of a caller can be returned, if the method
	 * {@code fillStackTraceElements()} of {@link VMStack} throws an exception.
	 */
	@Test
	public void stackTraceElementsFillerThrowsException() {
		AndroidRuntime runtime = new AndroidRuntime();

		Method method = Whitebox.getMethod(AndroidRuntimeTest.class, "throwException", Thread.class, StackTraceElement[].class);
		Whitebox.setInternalState(runtime, Method.class, method);
		Whitebox.setInternalState(runtime, int.class, 5);

		assertThat(runtime.getCallerStackTraceElement(1)).isEqualTo(new Throwable().getStackTrace()[0]);
		assertThat(systemStream.consumeErrorOutput())
			.containsOnlyOnce("ERROR")
			.containsOnlyOnce(UnsupportedOperationException.class.getName());
	}

	/**
	 * Verifies that the Android runtime can find the method {@code fillStackTraceElements()} in {@link VMStack}.
	 *
	 * @throws Exception
	 *             Failed to use reflections
	 */
	@Test
	public void findingFillStackTraceElementsMethod() throws Exception {
		Object result = Whitebox.invokeMethod(AndroidRuntime.class, "getStackTraceElementsFiller");
		assertThat(Whitebox.getInternalState(result, Method.class)).hasParameterTypes(Thread.class, StackTraceElement[].class);
		assertThat(Whitebox.getInternalState(result, int.class)).isNotNegative();
	}

	/**
	 * Verifies that timestamps with millisecond precision will be created.
	 *
	 * @throws InterruptedException
	 *             Interrupted while waiting between creation of both timestamps
	 */
	@Test
	public void createTimestamp() throws InterruptedException {
		AndroidRuntime runtime = new AndroidRuntime();

		Timestamp timestamp = runtime.createTimestamp();
		assertThat(timestamp).isInstanceOf(LegacyTimestamp.class);
		assertThat(timestamp.toInstant()).isBetween(Instant.now().minusSeconds(1), Instant.now());

		Thread.sleep(2);

		assertThat(runtime.createTimestamp().toInstant()).isAfter(timestamp.toInstant());
	}

	/**
	 * Verifies that a legacy timestamp formatter will be created.
	 */
	@Test
	public void createTimestampFormatter() {
		AndroidRuntime runtime = new AndroidRuntime();

		TimestampFormatter formatter = runtime.createTimestampFormatter("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
		assertThat(formatter).isInstanceOf(LegacyTimestampFormatter.class);

		Timestamp timestamp = new SimpleTimestamp(1985, 6, 3, 12, 30, 55, 999_001_002);
		assertThat(formatter.format(timestamp)).isEqualTo("1985-06-03 12:30:55.999");
	}

	/**
	 * Implementation for the method {@code fillStackTraceElements()} of {@link VMStack}.
	 *
	 * @param thread
	 *            Source thread for stack trace
	 * @param elements
	 *            Target array for storing stack trace
	 */
	@SuppressWarnings("unused")
	private static void fillStackTraceElements(final Thread thread, final StackTraceElement[] elements) {
		StackTraceElement[] trace = new Throwable().getStackTrace();
		System.arraycopy(trace, 0, elements, 0, Math.min(trace.length, elements.length));
	}

	/**
	 * Fake implementation for the method {@code fillStackTraceElements()} of {@link VMStack}. This method does nothing
	 * else but throwing an exception.
	 *
	 * @param thread
	 *            Source thread for stack trace
	 * @param elements
	 *            Target array for storing stack trace
	 *
	 * @throws UnsupportedOperationException
	 *            Always thrown on each call
	 */
	@SuppressWarnings("unused")
	private static void throwException(final Thread thread, final StackTraceElement[] elements) {
		throw new UnsupportedOperationException();
	}

}
