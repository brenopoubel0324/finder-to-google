#!/bin/sh
# Lançador do Gradle Wrapper. O pequeno JAR oficial é obtido e verificado no primeiro uso.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
if [ -n "${JAVA_HOME:-}" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo 'JDK 17 ou 21 necessário. Configure JAVA_HOME.' >&2
    exit 1
fi
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    "$JAVACMD" "$APP_HOME/gradle/wrapper/Bootstrap.java" "$WRAPPER_JAR"
fi
# Divisão intencional: opções da JVM em JAVA_OPTS e GRADLE_OPTS seguem a convenção Gradle.
# shellcheck disable=SC2086
exec "$JAVACMD" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
    -Dorg.gradle.appname=gradlew -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
