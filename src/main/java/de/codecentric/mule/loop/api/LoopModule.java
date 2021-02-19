package de.codecentric.mule.loop.api;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;

@Extension(name = "Loop")
@Operations(LoopOperations.class)
public class LoopModule {

}
