#!/bin/bash

export LANG=en_US.UTF-8

#region Запуск приложения
export MALLOC_ARENA_MAX="${MALLOC_ARENA_MAX:-2}"
export JAVA_OPTS="$JAVA_OPTS -XX:MaxRAMPercentage=25.0 -XX:MaxDirectMemorySize=256m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m -Xss512k -XX:NativeMemoryTracking=summary -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -XX:ErrorFile=/tmp/hs_err_pid%p.log -XX:+ExitOnOutOfMemoryError -Djava.net.preferIPv4Addresses=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Xverify:none"
APP_OPTS="-Dspring.profiles.active=PROM -Dlog4j2.format.MsgNoLookups=true -Duser.timezone=Europe/Moscow"

java $JAVA_OPTS $APP_OPTS -cp "/var/app/*:/var/app/dependencies/*" $1

#endregion
