# Mule Loop-Module Extension

## Introduction

This module adds four variants of loops to Mule 4 flows:

1. A repeat until loop: This loops runs at least once and is repeated until the body produces a non empty payload.
2. A while loop, where the payload controls the loop continuation and the payload for the next iteration.
   Controlled by a flag, the loop allows to collect results from the iterations. 
3. A for loop, counting from a start to an end value.
4. A for-each loop, similar to the for-each available in Mule 4, but collecting the payloads.


Caused by the way how scopes are implemented in the Mule SDK (asynchronous), it is possible to transfer variables 
into the scope, but there is no way to transfer variables out of the scope. So any changes to variables within the
loop are only visible within the current loop iteration.

## Maven Dependency

Add this dependency to your application pom.xml (check for newer version):

```
<dependency>
	<groupId>de.codecentric.mule.modules</groupId>
	<artifactId>loop-module</artifactId>
	<version>1.1.0</version>
	<classifier>mule-plugin</classifier>
</dependency>
```

The module is available on [Maven Central](https://mvnrepository.com/), so you don't need it to compile/install it yourself.

## Repeat Until Payload Not Empty

This loop executes the loop body at least once and repeats until the body returns a non empty payload. There is nothing to configure. Empty are null, empty String, empty array, and empty map.

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

## For-Each

The loop iterates over payload, or the optional collection given in the parameter `values`.
In contrast to the builtin for-each loop, the payloads of the iterations are collected and returned as an array.
So this example squares all numbers of a collection and returns a collection with the squares:

```
<loop:for-each>
	<set-payload value="#[payload * payload]"/>
</loop:for-each>
```

## While

Loop over scope while a condition is `true`. For the first iteration, the condition is given as parameter `condition`.
For the following iterations it is taken from the `payload`. Therefore, the payload at the end of the scope has to be
a map with two keys:
* `nextPayload`: The payload for the next iteration (or the result payload of the scope, if this is the last iteration).
* `condition`: Should another iteration follow?

The following example iterates over the numbers 10 to 0 (inclusive). The result is `-1` (the condition fails for `payload == 0`,
but `nextPayload` is set to `payload - 1`):

```
<loop:while initialPayload="#[payload]" condition="true">
    <set-payload value="#[10]" />
	<set-payload value="#[%dw 2.0&#10;output application/java&#10;---&#10;{	condition: payload &gt; 0,	nextPayload: payload - 1 }]"/>
</loop:while>
```

When you set the parameter `collectResults` to `true`, the result of the scope is not taken from the last payload, instead it is a collection
of the values `addToCollection` in the payload returned by the content of the scope.

The following example collects the numbers 10 to 0 (inclusive):  

```
<loop:while initialPayload="#[payload]" condition="true" collectResults="true">
    <set-payload value="#[10]" />
	<set-payload value="#[%dw 2.0&#10;output application/java&#10;---&#10;{	condition: payload &gt; 0,	nextPayload: payload - 1, addToCollection: payload }]"/>
</loop:while>
```

## Release notes

### 1.1.0 2022-08-16

- Refactoring
- Added while

### 1.0.2 2022-08-10 

- Fixed deadlock in repeat-until-payload-not-empty

### 1.0.1 2022-05-19

- Rewrite from recursion to iteration to allow longer loops without stack overflow
- Added for-each with result collection

### 0.1.0 2021-02-20

- Initial release

