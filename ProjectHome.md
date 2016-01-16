## Overview ##

Riposte is developed to utilize Three Rings Design's Narya ObjectInputStream and ObjectOutputStream over a stateless HTTP connection rather than through the presents TCP always-on connection.

It was also designed to utilize GWT services in a Java server so that all GWT service calls can be shared with any language that implements Object streaming.

## Current Implementation ##

Riposte server code is written in Java and runs in a Servlet environment.  Riposte currently has Actionscript and Java client libs.  A client library could be built for any language that also has an implementation of Narya's streaming libs, which basically amounts to supporting Java object streaming.

Instructions for building are included with the source in README.

## Dependencies ##

Riposte depends on Three Rings's Narya library, the Samskivert utility library, Google Collections and Google Guice.  It can be used in a non-guice environment, but was built with dependency injection in mind.