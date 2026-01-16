FROM docker-dev.registry-ci.delta.sbrf.ru/ci04675739/ci04675739/sberjdk-21-runtime:9.5.0-se

USER root

#region Копирование файлов из дистрибутива >>>
RUN mkdir -p /var
COPY bh/@deploymentUnit1@-main/*.jar /var/app/
COPY bh/@deploymentUnit1@-dependencies/*.jar /var/app/dependencies/
COPY conf/k8s/base/@fpi-name@/docker-entrypoint.sh /var/
#endregion

RUN chmod +x /var/docker-entrypoint.sh
RUN chmod g+rwX /var/

ENTRYPOINT [ "/var/docker-entrypoint.sh", "ru.sbrf.dab2c.executor.application.ApplicationEntryPoint" ]

EXPOSE 8080