package de.codecentric.mule.loop.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopOperations {
	private static Logger logger = LoggerFactory.getLogger(LoopOperations.class);

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
			logger.debug("output empty string");
			return true;
		}
		if (output instanceof Collection && ((Collection<?>) output).isEmpty()) {
			logger.debug("output is empty collection");
			return true;
		}
		if (output instanceof Map && ((Map<?, ?>)output).isEmpty()) {
			logger.debug("output is empty map");
			return true;
		}
		logger.debug("output is not empty");

		return false;
	}

	@Alias("for")
	public void forLoop(Chain operations, CompletionCallback<Object, Object> callback, //
			@DisplayName("start (inclusive)") @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "0") int start, //
			@DisplayName("end (exclusive)") int end, @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "true") boolean counterAsPayload) throws InterruptedException {

		if (start < end) {
			if (counterAsPayload) {
				forWithCounter(operations, callback, start, end);
			} else {
				forWithPayload(operations, callback, start, end);
			}
		} else {
			callback.success(Result.<Object, Object>builder().build()); 
		}
	}

	@SuppressWarnings("unchecked")
	private void forWithCounter(Chain operations, CompletionCallback<Object, Object> callback, int start, int end) throws InterruptedException {
		AtomicBoolean continueLoop = new AtomicBoolean(true);
		Semaphore sem = new Semaphore(0);
		for (int i = start; i < end && continueLoop.get(); i++) {
			final int counter = i;
			operations.process(counter, Collections.EMPTY_MAP, result -> {
				if (counter + 1 == end) {
					callback.success(result);
				}
				sem.release();
			}, (error, previous) -> {
				callback.error(error);
				continueLoop.set(false);
				sem.release();
			});
			sem.acquire();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void forWithPayload(Chain operations, CompletionCallback<Object, Object> callback, int start, int end) throws InterruptedException {
		AtomicBoolean continueLoop = new AtomicBoolean(true);
		ArrayBlockingQueue<Optional<Object>> queue = new ArrayBlockingQueue<>(1);
		
		Object payload = null;
		for (int i = start; i < end && continueLoop.get(); i++) {
			final int counter = i;
			if (counter == start) {
				// In first iteration, process with the payload present when scope starts:
				operations.process(result -> {
					if (counter + 1 == end) {
						callback.success(result);
					}
					queue.offer(Optional.ofNullable(result.getOutput()));
				}, (error, previous) -> {
					logger.info("xxx fail in first iteration");
					callback.error(error);
					continueLoop.set(false);
					queue.offer(Optional.empty());
				});
			} else {
				// For all other iterations, use the payload we have transported through the queue:
				operations.process(payload, Collections.EMPTY_MAP, result -> {
					if (counter + 1 == end) {
						callback.success(result);
					}
					queue.offer(Optional.ofNullable(result.getOutput()));
				}, (error, previous) -> {
					logger.info("xxx fail in other iteration");
					callback.error(error);
					continueLoop.set(false);
					queue.offer(Optional.empty());
				});
			}
			payload = queue.take().orElse(null);
		}
	}
	
	@Alias("for-each")
	public void forLoop(Chain operations, CompletionCallback<Object, Object> callback, //
		@org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "#[payload]") Collection<Object> values) throws InterruptedException {
		AtomicBoolean continueLoop = new AtomicBoolean(true);
		Collection<Object> resultCollection = new ArrayList<>(values.size());
		ArrayBlockingQueue<Optional<Object>> queue = new ArrayBlockingQueue<>(1);
		
		for (Object value: values) {
			if (!continueLoop.get()) {
				break;
			}
			operations.process(value, Collections.EMPTY_MAP, result -> {
				queue.offer(Optional.ofNullable(result.getOutput()));
			}, (error, previous) -> {
				callback.error(error);
				continueLoop.set(false);
				queue.offer(Optional.empty());
			});
			resultCollection.add(queue.take().orElse(null));
		}
		callback.success(Result.<Object, Object>builder().output(resultCollection).build()); 
	}
}
