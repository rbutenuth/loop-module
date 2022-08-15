package de.codecentric.mule.loop.api;

import java.util.HashSet;
import java.util.Set;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public class OperationErrorTypeProvider implements ErrorTypeProvider {

	@Override
	@SuppressWarnings("rawtypes")
	public Set<ErrorTypeDefinition> getErrorTypes() {
		Set<ErrorTypeDefinition> errors = new HashSet<>();
		for (LoopError error : LoopError.values()) {
			errors.add(error);
		}
		return errors;
	}
}
