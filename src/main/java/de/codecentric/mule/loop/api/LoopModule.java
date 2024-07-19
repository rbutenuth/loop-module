package de.codecentric.mule.loop.api;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.sdk.api.annotation.JavaVersionSupport;

@Extension(name = "Loop")
@Operations(LoopOperations.class)
@ErrorTypes(value = LoopError.class)
@JavaVersionSupport({ JAVA_8, JAVA_11, JAVA_17})
public class LoopModule {

}
