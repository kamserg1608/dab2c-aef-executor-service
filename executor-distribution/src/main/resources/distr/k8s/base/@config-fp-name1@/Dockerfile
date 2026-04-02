FROM docker-dev.registry-ci.delta.sbrf.ru/ci04675739/ci04675739/sberjdk-21@sha256:7917291190c92f64a1f49d79b51be39b765d4ca2833a35597c76ab04503c73ff

USER root

#region Копирование файлов из дистрибутива >>>
RUN mkdir -p /var
COPY bh/@deploymentUnit1@-main/*.jar /var/app/
COPY bh/@deploymentUnit1@-dependencies/*.jar /var/app/dependencies/
COPY conf/k8s/base/@fpi-name@/docker-entrypoint.sh /var/
#endregion

RUN chmod +x /var/docker-entrypoint.sh
RUN chmod g+rwX /var/
RUN microdnf install -y openssl procps tar which --nodocs && microdnf clean all

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
