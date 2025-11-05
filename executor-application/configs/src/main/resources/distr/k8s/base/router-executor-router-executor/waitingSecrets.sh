#!/bin/bash
checked_files=(
"/etc/config/ssl/crt/tls.crt"
"/etc/config/ssl/crt/tls.key"
"/etc/config/ssl/ca/root.crt"
"/etc/config/secrets/kv/cbul.dab2c.token"
"/etc/config/secrets/kv/cbul.dab2c.cn"
)

echo "Ожидаемые файлы  ${checked_files[*]}"

files_count=${#checked_files[@]}
exists_files_count=0
time_counter=0
while [ $exists_files_count != $files_count ]; do
  for (( i=0; i<$files_count; i++ )); do
    file=${checked_files[i]}
    if [ ! -z "${file}" ]; then
      if [ -f $file ]; then
        exists_files_count=$(( exists_files_count + 1 ))
        checked_files[i]=""
        echo "Ожидание секретов: готов $file"
      else
        sleep 1
        time_counter=$(( time_counter + 1 ))
        echo "Ожидаем $file ; время $time_counter с."
      fi
    fi
  done
done

echo "Ожидание секретов: Все секреты готовы!"

exit 0;
