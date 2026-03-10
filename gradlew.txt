#!/usr/bin/env sh

BASEDIR=$(dirname "$0")

java -classpath "$BASEDIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
