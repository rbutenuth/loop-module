package de.codecentric.mule.loop.api;

import static de.codecentric.mule.loop.api.PayloadAfterLoop.COLLECTION_OF_ALL_PAYLOADS_WITHIN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.sdk.api.annotation.param.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopOperations {
	private static Logger logger = LoggerFactory.getLogger(LoopOperations.class);

	@SuppressWarnings("unchecked")
	@MediaType("*/*")
	public void repeatUntilPayloadNotEmpty(Chain operations, CompletionCallback<Object, Object> callback)
			throws InterruptedException {
		ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<>(1);
		boolean continueLoop;
		do {
			operations.process(result -> {
				if (!isEmpty(result.getOutput())) {
					queue.offer(Boolean.FALSE);
					callback.success(result);
				} else {
					queue.offer(Boolean.TRUE);
				}
			}, (error, previous) -> {
				callback.error(error);
				queue.offer(Boolean.FALSE);
			});
			continueLoop = queue.take();
		} while (continueLoop);
	}

	private static boolean isEmpty(Object output) {
		if (output instanceof CursorProvider) {
			logger.warn("isEmpty check on CursorProvider not possible, will always return false" );
			return false;
		}
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
		if (output instanceof Map && ((Map<?, ?>) output).isEmpty()) {
			logger.debug("output is empty map");
			return true;
		}
		logger.debug("output is not empty");

		return false;
	}

	@Alias("while")
	@MediaType("*/*")
	@Throws(value = OperationErrorTypeProvider.class)
	public void whileLoop(Chain operations, CompletionCallback<Object, Object> callback,
			@org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "true") boolean condition, //
			@org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "#[payload]") Object initialPayload, //
			@org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "PAYLOAD_OF_LAST_ITERATION") PayloadAfterLoop resultPayload)
			throws InterruptedException {
		if (resultPayload == PayloadAfterLoop.ITERATOR_OF_ALL_PAYLOADS_WITHIN) {
			whileLoopStreaming(operations, callback, condition, initialPayload);
		} else {
			whileLoopInMemory(operations, callback, condition, initialPayload, resultPayload);
		}
	}

	private void whileLoopInMemory(Chain operations, CompletionCallback<Object, Object> callback, boolean condition, Object initialPayload, PayloadAfterLoop resultPayload) throws InterruptedException {
		ArrayBlockingQueue<WhileQueueEntry> queue = new ArrayBlockingQueue<>(1);
		List<Object> resultCollection = resultPayload == COLLECTION_OF_ALL_PAYLOADS_WITHIN ? new ArrayList<>() : null;
		boolean firstIteration = true;
		WhileQueueEntry entry = new WhileQueueEntry(condition, initialPayload, null);

		while (entry.condition) {
			Object nextPayload = firstIteration ? initialPayload : entry.payload;
			operations.process(nextPayload, Collections.EMPTY_MAP, result -> {
				Map<String, Object> payload = payloadAsMap(result);
				queue.offer(new WhileQueueEntry(evaluateCondition(payload.get("condition")), payload.get("nextPayload"), payload.get("addToCollection")));
			}, (error, previous) -> {
				callback.error(error);
				// Make sure while() does not hang in case of error
				queue.offer(new WhileQueueEntry(error));
			});
			entry = queue.take();
			if (resultPayload == COLLECTION_OF_ALL_PAYLOADS_WITHIN) {
				resultCollection.add(entry.addToCollection);
			}
			firstIteration = false;
		}
		if (entry.error == null) {
			if (resultPayload == PayloadAfterLoop.COLLECTION_OF_ALL_PAYLOADS_WITHIN) {
				callback.success(Result.<Object, Object>builder().output(resultCollection).build());
			} else if (resultPayload == PayloadAfterLoop.PAYLOAD_BEFORE_LOOP) {
				callback.success(Result.<Object, Object>builder().output(initialPayload).build());
			} else { // PAYLOAD_OF_LAST_ITERATION
				callback.success(Result.<Object, Object>builder().output(entry.payload).build());
			}
		}
	}
	
	private void whileLoopStreaming(Chain operations, CompletionCallback<Object, Object> callback, boolean condition, Object initialPayload) throws InterruptedException {
		Iterator<Object> result = new Iterator<Object>() {
			boolean firstIteration = true;
			WhileQueueEntry entry = new WhileQueueEntry(condition, initialPayload, null);

			@Override
			public boolean hasNext() {
				return entry.condition;
			}

			@Override
			public Object next() {
				Object nextPayload = firstIteration ? initialPayload : entry.payload;
				ArrayBlockingQueue<WhileQueueEntry> queue = new ArrayBlockingQueue<>(1);
				operations.process(nextPayload, Collections.EMPTY_MAP, result -> {
					Map<String, Object> payload = payloadAsMap(result);
					queue.offer(new WhileQueueEntry(evaluateCondition(payload.get("condition")), payload.get("nextPayload"), payload.get("addToCollection")));
				}, (error, previous) -> {
					queue.offer(new WhileQueueEntry(error));
				});
				try {
					firstIteration = false;
					entry = queue.take();
					if (entry.error != null) {
						throw new RuntimeException(entry.error);
					}
					return entry.addToCollection;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};
		callback.success(Result.<Object, Object>builder().output(result).build());
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> payloadAsMap(Result<?, ?> result) {
		Object rawPayload = result.getOutput();
		if (!(rawPayload instanceof Map)) {
			throw new ModuleException(
					"Payload should be Map, but is: " + (rawPayload == null ? "null" : rawPayload.getClass()),
					LoopError.PAYLOAD_IS_NOT_MAP);
		}
		return (Map<String, Object>) rawPayload;
	}

	private static class WhileQueueEntry {
		private final boolean condition;
		private final Throwable error;
		private final Object payload;
		private final Object addToCollection;

		public WhileQueueEntry(boolean condition, Object payload, Object addToCollection) {
			this.condition = condition;
			this.error = null;
			this.payload = payload;
			this.addToCollection = addToCollection;
		}
		
		public WhileQueueEntry(Throwable error) {
			this.condition = false;
			this.error = error;
			this.payload = null;
			this.addToCollection = null;
		}
	}

	/**
	 * @param condition Any object...
	 * @return <code>null</code>: false, {@link Boolean}: value, otherwise:
	 *         !{@link #isEmpty(Object)}
	 */
	static boolean evaluateCondition(Object condition) {
		if (condition == null) {
			return false;
		} else if (condition instanceof Boolean) {
			return ((Boolean) condition).booleanValue();
		} else {
			return !isEmpty(condition);
		}
	}

	@Alias("for")
	@MediaType("*/*")
	public void forLoop(Chain operations, CompletionCallback<Object, Object> callback, //
			@DisplayName("start (inclusive)") @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "0") int start, //
			@DisplayName("end (exclusive)") int end,
			@org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "true") boolean counterAsPayload)
			throws InterruptedException {

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
	private void forWithCounter(Chain operations, CompletionCallback<Object, Object> callback, int start, int end)
			throws InterruptedException {
		ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<>(1);
		boolean continueLoop = true;
		for (int i = start; i < end && continueLoop; i++) {
			final int counter = i;
			operations.process(counter, Collections.EMPTY_MAP, result -> {
				if (counter + 1 == end) {
					callback.success(result);
				}
				queue.offer(Boolean.TRUE);
			}, (error, previous) -> {
				callback.error(error);
				queue.offer(Boolean.FALSE);
			});
			continueLoop = queue.take();
		}
	}

	@SuppressWarnings("unchecked")
	private void forWithPayload(Chain operations, CompletionCallback<Object, Object> callback, int start, int end)
			throws InterruptedException {
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
					callback.error(error);
					continueLoop.set(false);
					queue.offer(Optional.empty());
				});
			} else {
				// For all other iterations, use the payload we have transported through the
				// queue:
				operations.process(payload, Collections.EMPTY_MAP, result -> {
					if (counter + 1 == end) {
						callback.success(result);
					}
					queue.offer(Optional.ofNullable(result.getOutput()));
				}, (error, previous) -> {
					callback.error(error);
					continueLoop.set(false);
					queue.offer(Optional.empty());
				});
			}
			payload = queue.take().orElse(null);
		}
	}

	@Alias("for-each")
	@MediaType("*/*")
	public void forEachLoop(Chain operations, CompletionCallback<Object, Object> callback, //
			@org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "#[payload]") Collection<Object> values,
			@org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false") boolean streaming)
			throws InterruptedException {
		if (streaming) {
			forEachLoopStreaming(operations, callback, values);
		} else {
			forEachLoopInMemory(operations, callback, values);
		}
	}

	private void forEachLoopInMemory(Chain operations, CompletionCallback<Object, Object> callback, Collection<Object> values) throws InterruptedException {
		AtomicBoolean errorOccured = new AtomicBoolean(false);
		Collection<Object> resultCollection = new ArrayList<>(values.size());
		ArrayBlockingQueue<Optional<Object>> queue = new ArrayBlockingQueue<>(1);

		for (Object value : values) {
			if (errorOccured.get()) {
				break;
			}
			operations.process(value, Collections.EMPTY_MAP, result -> {
				queue.offer(Optional.ofNullable(result.getOutput()));
			}, (error, previous) -> {
				callback.error(error);
				errorOccured.set(true);
				queue.offer(Optional.empty());
			});
			resultCollection.add(queue.take().orElse(null));
		}
		if (!errorOccured.get()) {
			callback.success(Result.<Object, Object>builder().output(resultCollection).build());
		}
	}

	private void forEachLoopStreaming(Chain operations, CompletionCallback<Object, Object> callback, Collection<Object> values) {
		Iterator<Object> inputIterator = values.iterator();
		Iterator<Object> result = new Iterator<Object>() {

			@Override
			public boolean hasNext() {
				return inputIterator.hasNext();
			}

			@Override
			public Object next() {
				Object value = inputIterator.next();
				ArrayBlockingQueue<ForQueueEntry> queue = new ArrayBlockingQueue<>(1);
				operations.process(value, Collections.EMPTY_MAP, result -> {
					queue.offer(new ForQueueEntry(result.getOutput()));
				}, (error, previous) -> {
					queue.offer(new ForQueueEntry(error));
				});
				try {
					ForQueueEntry entry = queue.take();
					if (entry.error == null) {
						return entry.value;
					} else {
						throw new RuntimeException(entry.error);
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};
		callback.success(Result.<Object, Object>builder().output(result).build());
	}
	
	private static class ForQueueEntry {
		private final Object value;
		private final Throwable error;
		
		public ForQueueEntry(Object value) {
			this.value = value;
			error = null;
		}

		public ForQueueEntry(Throwable error) {
			value = null;
			this.error = error;
		}
	}
}
