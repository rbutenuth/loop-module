package de.codecentric.mule.loop.api;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;

public class LoopOperations {
	private static Log logger = LogFactory.getLog(LoopOperations.class);

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
				logger.debug("output: \"" + output + "\"");
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

		if (counterAsPayload) {
			forLoopWithCounter(operations, callback, start, end);
		} else {
			forLoopWithPayload(operations, callback, start, end, true, null);
		}
	}

	@SuppressWarnings("unchecked")
	private void forLoopWithCounter(Chain operations, CompletionCallback<Object, Object> callback, int start, int end) {
		if (start < end) {
			operations.process(start, Collections.EMPTY_MAP, result -> {
				if (start + 1 < end) {
					forLoopWithCounter(operations, callback, start + 1, end);
				} else {
					callback.success(result);
				}
			}, (error, previous) -> {
				callback.error(error);
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void forLoopWithPayload(Chain operations, CompletionCallback<Object, Object> callback, int start, int end,
			boolean first, Object payload) {

		if (start < end) {
			if (first) {
				operations.process(result -> {
					
					if (start + 1 < end) {
						forLoopWithPayload(operations, callback, start + 1, end, false, result.getOutput());
					} else {
						callback.success(result);
					}
				}, (error, previous) -> {
					callback.error(error);
				});
			} else {
				operations.process(payload, Collections.EMPTY_MAP, result -> {
					
					if (start + 1 < end) {
						forLoopWithPayload(operations, callback, start + 1, end, false, result.getOutput());
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
