FROM docker-dev.registry-ci.delta.sbrf.ru/ci04675739/ci04675739/sberjdk-21:java-21.0.11_001-sberlinux-ubi-10.1.2-se

USER root

#region Копирование файлов из дистрибутива >>>
RUN mkdir -p /var
COPY bh/@deploymentUnit1@-main/*.jar /var/app/
COPY bh/@deploymentUnit1@-dependencies/*.jar /var/app/dependencies/
COPY conf/k8s/base/@fpi-name@/docker-entrypoint.sh /var/
#endregion

RUN chmod +x /var/docker-entrypoint.sh
RUN chmod g+rwX /var/

RUN dnf update -y && dnf install -y openssl

# Приложение работает только на Java.
# Удаляем неиспользуемый Python 3.12 вместе с уязвимыми
# urllib3 1.26.19 и setuptools 69.0.3.
RUN rm -rf \
        /usr/lib/python3.12 \
        /usr/lib64/python3.12 \
        /usr/local/lib/python3.12 \
        /usr/local/lib64/python3.12 \
    && rm -f \
        /usr/bin/python3 \
        /usr/bin/python3.12 \
        /usr/bin/pip3 \
        /usr/bin/pip3.12

EXPOSE 8080

ENTRYPOINT [ "/var/docker-entrypoint.sh", "ru.sbrf.dab2c.executor.application.ApplicationEntryPointKt" ]
