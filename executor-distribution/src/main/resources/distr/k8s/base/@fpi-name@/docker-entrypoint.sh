#!/bin/bash

export LANG=en_US.UTF-8

#region Запуск приложения
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Addresses=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Xverify:none"
APP_OPTS="-Dspring.profiles.active=PROM -Dlog4j2.format.MsgNoLookups=true -Duser.timezone=Europe/Moscow"

java $JAVA_OPTS $APP_OPTS -cp "/var/app/*:/var/app/dependencies/*" $1

#endregion
