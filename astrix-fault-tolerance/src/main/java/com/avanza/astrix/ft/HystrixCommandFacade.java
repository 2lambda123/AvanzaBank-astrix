/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.ft;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.core.AstrixCallStackTrace;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;

/**
 * @author Elias Lindholm (elilin)
 * @author Kristoffer Erlandsson (krierl)
 */
class HystrixCommandFacade<T> {

	private static final Logger log = LoggerFactory.getLogger(HystrixCommandFacade.class);

	private final CheckedCommand<T> command;
	private final Setter hystrixConfiguration;
	
	private HystrixCommandFacade(CheckedCommand<T> command, Setter hystrixConfiguration) {
		this.command = command;
		this.hystrixConfiguration = hystrixConfiguration;
	}

	public static <T> T execute(CheckedCommand<T> command, Setter settings) throws Throwable {
		return new HystrixCommandFacade<>(command, settings).execute();
	}

	protected T execute() throws Throwable {
		HystrixCommand<HystrixResult<T>> command = createHystrixCommand();
		HystrixResult<T> result = command.execute();
		throwExceptionIfExecutionFailed(result);
		return result.getResult();
	}

	private void throwExceptionIfExecutionFailed(HystrixResult<T> result) throws Throwable {
		if (result.getException() != null) {
			AstrixCallStackTrace trace = new AstrixCallStackTrace();
			appendStackTrace(result.getException(), trace);
			throw result.getException();
		}
	}

	private void appendStackTrace(Throwable exception, AstrixCallStackTrace trace) {
		Throwable lastThowableInChain = exception;
		while (lastThowableInChain.getCause() != null) {
			lastThowableInChain = lastThowableInChain.getCause();
		}
		lastThowableInChain.initCause(trace);
	}

	private HystrixCommand<HystrixResult<T>> createHystrixCommand() {
		return new HystrixCommand<HystrixResult<T>>(hystrixConfiguration) {

			@Override
			protected HystrixResult<T> run() throws Exception {
				try {
					return HystrixResult.success(command.call());
				} catch (Throwable e) {
					return handleException(e);
				} 
			}

			private HystrixResult<T> handleException(Throwable cause) {
				if (cause instanceof ServiceUnavailableException) {
					// Only ServiceUnavailableExceptions are propagated and counted as failures for the circuit breaker
					throw (ServiceUnavailableException) cause;
				}
				// Any other exception is treated as a service exception and does not count as failures for the circuit breaker
				return HystrixResult.exception(cause);
			}

			@Override
			protected HystrixResult<T> getFallback() {
				// getFallback is only invoked when the underlying api threw an ServiceUnavailableException, or the
				// when the invocation reached timeout. In any case, treat this as service unavailable.
				ServiceUnavailableCause cause = resolveUnavailableCause();
				log.info(String.format("Aborted command execution: cause=%s circuit=%s", cause, this.getCommandKey().name()));
				if (isFailedExecution()) {
					// Underlying service threw ServiceUnavailableException
					return HystrixResult.exception(getFailedExecutionException());
				}
				// Timeout or rejected in queue
				return HystrixResult.exception(new ServiceUnavailableException(String.format("cause=%s service=%s executionTime=%s", 
																				Objects.toString(cause), getCommandKey().name(), getExecutionTimeInMilliseconds())));
			}
			
			private ServiceUnavailableCause resolveUnavailableCause() {
				if (isResponseRejected()) {
					return ServiceUnavailableCause.REJECTED_EXECUTION;
				}
				if (isResponseTimedOut()) {
					return ServiceUnavailableCause.TIMEOUT;
				}
				if (isResponseShortCircuited()) {
					return ServiceUnavailableCause.SHORT_CIRCUITED;
				}
				if (isFailedExecution()) {
					return ServiceUnavailableCause.UNAVAILABLE;
				}
				return ServiceUnavailableCause.UNKNOWN;
			}

		};
	}

	private static class HystrixResult<T> {

		private T result;
		private Throwable exception;

		public static <T> HystrixResult<T> success(T result) {
			return new HystrixResult<>(result, null);
		}

		public static <T> HystrixResult<T> exception(Throwable exception) {
			return new HystrixResult<>(null, exception);
		}

		public HystrixResult(T result, Throwable exception) {
			this.result = result;
			this.exception = exception;
		}

		public T getResult() {
			return result;
		}

		public Throwable getException() {
			return exception;
		}

	}
}
