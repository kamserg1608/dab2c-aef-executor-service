FROM docker-dev.registry-ci.delta.sbrf.ru/ci04675739/ci04675739/openjdk-21-runtime:8.10.6-se

USER root
RUN adduser -U spring

RUN dnf install -y openssl

#region Копирование файлов из дистрибутива >>>
RUN mkdir -p /var
COPY conf/k8s/base/@config-fp-name1@/importCertificates.sh /var/
COPY conf/k8s/base/@config-fp-name1@/waitingSecrets.sh /var/
COPY conf/k8s/base/@config-fp-name1@/docker-entrypoint.sh /var/
COPY bh/@deploymentUnit1@-main/*.jar /var/app/
COPY bh/@deploymentUnit1@-dependencies/*.jar /var/app/dependencies/
#endregion

RUN chown -R spring:spring /var
RUN chmod +x /var/docker-entrypoint.sh /var/waitingSecrets.sh /var/importCertificates.sh
RUN chmod g+rwX /var/

USER spring:spring
ENTRYPOINT [ "/var/docker-entrypoint.sh", "ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app.RouterExecutorApplicationEntryPointKt" ]

EXPOSE 8080
