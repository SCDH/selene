#!/bin/sh

JAVAOPTS="-Ddebug=true -Dorg.slf4j.simpleLogger.defaultLogLevel=info"

java $JAVAOPTS -cp ${project.build.directory}/${project.artifactId}-${project.version}.jar:${project.build.directory}/lib/* de.wwu.scdh.annotation.selection.cli.SelectionEngine $@
