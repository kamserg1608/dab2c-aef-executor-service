FROM docker-dev.registry-ci.delta.sbrf.ru/ci04675739/ci04675739/openjdk-21-runtime:8.10.6-se

USER root

#region Копирование файлов из дистрибутива >>>
RUN mkdir -p /var
COPY bh/router_executor_java_dab2c_router_executor-main/*.jar /var/app/
COPY bh/router_executor_java_dab2c_router_executor-dependencies/*.jar /var/app/dependencies/
COPY conf/k8s/base/router-executor/waitingSecrets.sh /var/
COPY conf/k8s/base/router-executor/docker-entrypoint.sh /var/
COPY conf/k8s/base/router-executor/importCertificates.sh /var/
#endregion

RUN chmod +x /var/docker-entrypoint.sh /var/waitingSecrets.sh /var/importCertificates.sh
RUN chmod g+rwX /var/
RUN dnf install -y openssl

ENTRYPOINT [
    "/var/docker-entrypoint.sh",
    "ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app.RouterExecutorApplicationEntryPointKt"
]

EXPOSE 8080
