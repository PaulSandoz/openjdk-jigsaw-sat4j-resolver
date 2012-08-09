openjdk-jigsaw-sat4j-resolver
=============================

Experiments applying a SAT solver for dependency resolution in OpenJDK Jigsaw

This requires a recent build of Jigsaw.

This requires a local build of Sat4j, current version 2.3.3-SNAPSHOT. The reason
being the Sat4j jars distributed on maven are compiled such that the class 
major version is 48 (i.e. Java 1.4) even though the class files contain generic
signature types. Javac >= 7 will comply with the class file version and ignore
such signature types.