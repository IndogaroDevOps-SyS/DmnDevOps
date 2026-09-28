#!/usr/bin/env sh

if [ -n "$DEBUG" ]; then
    echo "$*"
fi

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
    if ! command -v java >/dev/null 2>&1 ; then
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
        exit 1
    fi
fi

APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`

DEFAULT_JVM_OPTS='-Xmx64m -Xms64m'

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
