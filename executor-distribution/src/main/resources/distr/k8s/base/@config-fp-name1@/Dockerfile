FROM docker-dev.registry-ci.delta.sbrf.ru/ci04675739/ci04675739/sberjdk-21:java-21.0.9_001-sberlinux-ubi-9.6.2-se

USER root

#region Копирование файлов из дистрибутива >>>
RUN mkdir -p /var
COPY bh/@deploymentUnit1@-main/*.jar /var/app/
COPY bh/@deploymentUnit1@-dependencies/*.jar /var/app/dependencies/
COPY conf/k8s/base/@fpi-name@/docker-entrypoint.sh /var/
#endregion

RUN chmod +x /var/docker-entrypoint.sh
RUN chmod g+rwX /var/
RUN dnf update -y
RUN dnf install -y openssl

ENTRYPOINT [ "/var/docker-entrypoint.sh", "ru.sbrf.dab2c.executor.application.ApplicationEntryPointKt" ]

RUN  rm -rf /usr/lib/node_modules \
    && rm -rf /usr/bin/sqlite3 \
    && rm -rf /usr/include/sqlite3.h \
    && rm -rf /usr/include/sqlite3ext.h \
    && rm -rf /usr/lib64/pkgconfig/sqlite3.pc \
    && rm -rf /usr/lib64/libsqlite3.so.0 \
    && rm -rf /usr/lib64/libsqlite3.so.0.8.6 \
    && rm -rf /usr/lib64/libsqlite3.so \
    && rm -rf /usr/lib64/python3.9 \
    && rm -rf /usr/lib/python3.9 \
    && rm -rf /var/lib/rpm/rpmdb.sqlite \
    && rm -rf /var/lib/rpm/rpmdb.sqlite-shm \
    && rm -rf /var/lib/rpm/rpmdb.sqlite-wal \
    && rm -rf /var/lib/dnf/history.sqlite-shm \
    && rm -rf /var/lib/dnf/history.sqlite \
    && rm -rf /var/lib/dnf/history.sqlite-wal

EXPOSE 8080
