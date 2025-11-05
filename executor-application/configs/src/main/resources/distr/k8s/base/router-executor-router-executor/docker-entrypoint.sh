#!/bin/bash -e

/var/waitingSecrets.sh

export LANG=en_US.UTF-8

#настройка SSL
CLIENT_CERTS_PATH="/etc/config/ssl/crt/tls.crt"
CLIENT_PRIVATE_KEY_PATH="/etc/config/ssl/crt/tls.key"
SERVER_CERTS_PATH="/etc/config/ssl/ca/root.crt"

P12_STORE_PATH="/tmp/tls.p12"
KEY_STORE_PATH="/tmp/keystore.jks"
KEY_STORE_PASSWORD_PATH="/tmp/keystore.jks.pass"
TRUST_STORE_PATH="/tmp/trustStore.jks"
TRUST_STORE_PASSWORD_PATH="/tmp/trustStore.jks.pass"
SSL_PASSWORD=$(openssl rand -hex 12)

echo "Зачистка кейсторов — они при рестарте пода будут присутствовать и будут мешать, если их не удалить"
rm -f $KEY_STORE_PATH
rm -f $KEY_STORE_PASSWORD_PATH
rm -f $TRUST_STORE_PATH
rm -f $TRUST_STORE_PASSWORD_PATH
rm -f $P12_STORE_PATH

echo "Создание хранилища сертификатов: конвертируем в p12"
openssl pkcs12 -export -in $CLIENT_CERTS_PATH -inkey $CLIENT_PRIVATE_KEY_PATH -passout pass:$SSL_PASSWORD -out $P12_STORE_PATH
echo "Создание хранилища сертификатов: финиш"

echo "Создание keystore: старт"
keytool -importkeystore -srcstorepass $SSL_PASSWORD -deststorepass $SSL_PASSWORD -destkeystore $KEY_STORE_PATH -srckeystore $P12_STORE_PATH -srcstoretype PKCS12
echo "Создание keystore: финиш"

echo "Создание truststore: старт"
/var/importCertificates.sh $SERVER_CERTS_PATH $SSL_PASSWORD $TRUST_STORE_PATH
echo "Создание truststore: финиш"

echo "Создание файла с паролем для keystore: старт"
echo $SSL_PASSWORD > $KEY_STORE_PASSWORD_PATH
echo "Создание файла с паролем для keystore: финиш"

echo "Создание файла с паролем для trustStore: старт"
echo $SSL_PASSWORD > $TRUST_STORE_PASSWORD_PATH
echo "Создание файла с паролем для trustStore: финиш"

KEYSTORE_OPTS="-Djavax.net.ssl.keyStore=$KEY_STORE_PATH -Dpv.javax.net.ssl.keyStorePasswordLocation=$KEY_STORE_PASSWORD_PATH"
TRUSTSTORE_OPTS="-Djavax.net.ssl.trustStore=$TRUST_STORE_PATH -Dpv.javax.net.ssl.trustStorePasswordLocation=$TRUST_STORE_PASSWORD_PATH"

#region Запуск приложения
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Addresses=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Xverify:none"

ENHANCED_JAVA_OPTS=`env | grep '^router-executor-router-executor.ose.configmap.javaArguments=' | cut -d '=' -f1 --complement`
export JAVA_OPTS="$JAVA_OPTS ${ENHANCED_JAVA_OPTS}"


APP_OPTS="-Dspring.profiles.active=PROM -Dlog4j2.format.MsgNoLookups=true -Duser.timezone=Europe/Moscow "


java $JAVA_OPTS $APP_OPTS $KEYSTORE_OPTS $TRUSTSTORE_OPTS -cp "/var/app/*:/var/app/dependencies/*" $1
#endregion
