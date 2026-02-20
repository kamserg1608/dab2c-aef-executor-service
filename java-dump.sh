#!/bin/bash

# Настройки
NAMESPACE="ci07909419-dab2c-ivr900humanagent-b1"
POD_NAME="ivr900humanagent-executor-unver-67477d74f4-h86sv"
CONTAINER_NAME="executor-unver"
JAVA_PID="7"
LOCAL_DIR="./dumps/load_ongoing_2_cpu_hight"

mkdir -p $LOCAL_DIR

echo "🔍 Проверка jcmd..."
if ! kubectl exec -n $NAMESPACE $POD_NAME -c $CONTAINER_NAME -- which jcmd > /dev/null 2>&1; then
    echo "❌ Ошибка: jcmd не найден в контейнере. Проверьте образ."
    exit 1
fi

echo "🧵 Снятие ThreadDump..."
kubectl exec -n $NAMESPACE $POD_NAME -c $CONTAINER_NAME -- \
  jcmd $JAVA_PID Thread.print > "$LOCAL_DIR/thread_dump_$(date +%F_%H-%M).tdump"

echo "🧠 Снятие HeapDump (это может занять время)..."
# 1. Создаем дамп
kubectl exec -n $NAMESPACE $POD_NAME -c $CONTAINER_NAME -- \
  jcmd $JAVA_PID GC.heap_dump /tmp/dump.hprof

# 2. Копируем локально
kubectl exec -n $NAMESPACE $POD_NAME -c $CONTAINER_NAME -- \
  cat /tmp/dump.hprof > "$LOCAL_DIR/heap_dump_$(date +%F_%H-%M).hprof"

# 3. Чистим за собой
kubectl exec -n $NAMESPACE $POD_NAME -c $CONTAINER_NAME -- \
  rm /tmp/dump.hprof

echo "✅ Готово! Файлы в папке $LOCAL_DIR"