package de.codecentric.mule.loop.api;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum LoopError implements ErrorTypeDefinition<LoopError> {
	PAYLOAD_IS_NOT_MAP
}
