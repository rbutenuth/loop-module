package de.codecentric.mule.loop.api;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

@Extension(name = "Loop")
@Operations(LoopOperations.class)
@ErrorTypes(value = LoopError.class)
public class LoopModule {

}
