package de.codecentric.mule.loop.api;

import java.util.List;

public enum PayloadAfterLoop {
	PAYLOAD_BEFORE_LOOP {
		@Override
		Object result(Object initialPayload, List<Object> resultCollection, Object lastPayload) {
			return initialPayload;
		}
	},
	COLLECTION_OF_ALL_PAYLOADS_WITHIN {
		@Override
		Object result(Object initialPayload, List<Object> resultCollection, Object lastPayload) {
			return resultCollection;
		}
	},
	PAYLOAD_OF_LAST_ITERATION {
		@Override
		Object result(Object initialPayload, List<Object> resultCollection, Object lastPayload) {
			return lastPayload;
		}
	}
	;

	abstract Object result(Object initialPayload, List<Object> resultCollection, Object lastPayload);
}
