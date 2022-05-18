package de.codecentric.mule.loop.api;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;

public class LoopOperations implements Stoppable, Startable {
	private static Logger logger = LoggerFactory.getLogger(LoopOperations.class);

	@Inject
	private SchedulerService schedulerService;

	private ScheduledExecutorService scheduledExecutor;

	@Override
	public void start() {
		SchedulerConfig config = SchedulerConfig.config().withMaxConcurrentTasks(10)
				.withShutdownTimeout(1, TimeUnit.SECONDS).withPrefix("loop-module")
				.withName("operations");
		scheduledExecutor = schedulerService.customScheduler(config);
	}

	@Override
	public void stop() {
		scheduledExecutor.shutdown();
	}

	@SuppressWarnings("unchecked")
	public void repeatUntilPayloadNotEmpty(Chain operations, CompletionCallback<Object, Object> callback) {
		operations.process(result -> {

			if (isEmpty(result)) {
				repeatUntilPayloadNotEmpty(operations, callback);
			} else {
				callback.success(result);
			}
		}, (error, previous) -> {
			callback.error(error);
		});
	}

	private boolean isEmpty(Result<?, ?> result) {
		Object output = result.getOutput();
		if (output == null) {
			logger.debug("output is null");
			return true;
		}
		if (output instanceof String && ((String) output).trim().isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("output: \"{}\"", output);
			}
			return true;
		}
		if (result.getByteLength().orElse(-1) == 0) {
			logger.debug("output length is 0");
			return true;
		}
		logger.debug("output is not empty");

		return false;
	}

	@Alias("for")
	public void forLoop(Chain operations, CompletionCallback<Object, Object> callback, //
			@DisplayName("start (inclusive)") @Optional(defaultValue = "0") int start, //
			@DisplayName("end (exclusive)") int end, @Optional(defaultValue = "true") boolean counterAsPayload) {

		if (start < end) {
			if (counterAsPayload) {
				new ForWithCounterRunner(operations, callback, start, end).run();
			} else {
				new ForWithPayloadRunner(operations, callback, start, end).run();
			}
		}
	}

	private class ForWithCounterRunner implements Runnable {
		private Chain operations;
		private CompletionCallback<Object, Object> callback;
		private int start;
		private int end;
		
		public ForWithCounterRunner(Chain operations, CompletionCallback<Object, Object> callback, int start, int end) {
			this.operations = operations;
			this.callback = callback;
			this.start = start;
			this.end = end;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void run() {
			operations.process(start, Collections.EMPTY_MAP, result -> {
				if (start + 1 < end) {
					start++;
					scheduledExecutor.submit(this);
				} else {
					callback.success(result);
				}
			}, (error, previous) -> {
				callback.error(error);
			});
		}
	}
	
	private class ForWithPayloadRunner implements Runnable {
		private Chain operations;
		private CompletionCallback<Object, Object> callback;
		private int start;
		private int end;
		private boolean first;
		private Object payload;
		
		public ForWithPayloadRunner(Chain operations, CompletionCallback<Object, Object> callback, int start, int end) {
			this.operations = operations;
			this.callback = callback;
			this.start = start;
			this.end = end;
			first = true;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void run() {
			if (first) {
				operations.process(result -> {
					if (start + 1 < end) {
						start++;
						payload = result.getOutput();
						first = false;
						scheduledExecutor.submit(this);
					} else {
						callback.success(result);
					}
				}, (error, previous) -> {
					callback.error(error);
				});
			} else {
				operations.process(payload, Collections.EMPTY_MAP, result -> {
					if (start + 1 < end) {
						start++;
						payload = result.getOutput();
						scheduledExecutor.submit(this);
					} else {
						callback.success(result);
					}
				}, (error, previous) -> {
					callback.error(error);
				});
			}
		}
	}
}
