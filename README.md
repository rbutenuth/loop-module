# Mule Loop-Module Extension

## Introduction

This module adds two variant of loops to Mule 4 flows:

1. A repeat until loop: This loops runs at least once and is repeated until the body produces a non empty payload.
2. A for loop, counting from a start to an end value.

Caused by the way how scopes are implemented in the Mule SDK (asynchronous), it is possible to transfer variables 
into the scope, but there is no way to transfer variables out of the scope. So any changes to variables within the
loop are only visible in the the loop. There is one way to transfer data out of the loop: The payload after the 
last iteration is the payload returned by the loop.

## Maven Dependency

Add this dependency to your application pom.xml (check for newer version):

```
<dependency>
	<groupId>de.codecentric.mule.modules</groupId>
	<artifactId>loop-module</artifactId>
	<version>0.1.0</version>
	<classifier>mule-plugin</classifier>
</dependency>
```

The module is available on [Maven Central](https://mvnrepository.com/), so you don't need it to compile/install it yourself.

## Repeat Until Payload Not Empty

This loop executes the loop body at least once and repeats until the body returns a non empty payload. There is nothing to configure.

Here an example which calls an HTTP end point with one query parameter until the GET request returns a non empty payload:

```
<loop:repeat-until-payload-not-empty doc:name="Repeat until payload not empty">
	<http:request method="GET" doc:name="/ping" config-ref="HTTP_Request_configuration" path="/ping">
		<http:query-params ><![CDATA[#[%dw 2.0
output application/java
---
{
	start: vars.start
}]]]></http:query-params>
	</http:request>
	<logger level="INFO" doc:name="payload" message="#[payload]" category="loop-test"/>
</loop:repeat-until-payload-not-empty>
```

## For

The for loop executes the body `end - start` times, with a counter starting at `start` and ending
just before `end`. Thus `start` is included, `end` is excluded. You can choose if the payload is 
set to the counter (as integer) or if the payload from the flow is passed to the loop body. 

When the payload is passed into the body, the payload at the end of one iteration is the payload for
the next iteration. So you can use it to collect data within the loop.

### Payload Is Set To Counter

Here a simple loop which logs the numbers 0 to 41:

```
<loop:for doc:name="For" start="0" end="42">
	<logger level="INFO" doc:name="payload" message="#[payload]" category="for-counter"/>
</loop:for>
```

### Payload Passed Through

Another example, where the payload is collected and you end with a string of 42 `x`. The first
set in front of the loop, followed by 41 `x` added within the loop:

```
<set-payload value='#["x"]' doc:name="x" />
<loop:for doc:name="For" start="0" end="42" counterAsPayload="false">
	<logger level="INFO" doc:name="payload" message="#[payload]" category="for"/>
	<set-payload value='#[payload ++ "x"]' doc:name='payload ++ "x"' />
</loop:for>
```

