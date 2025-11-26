#!/bin/bash
checked_files=(
"/etc/config/ssl/crt/tls.crt"
"/etc/config/ssl/crt/tls.key"
"/etc/config/ssl/ca/root.crt"
)

echo "Ожидаемые файлы  ${checked_files[*]}"

files_count=${#checked_files[@]}
time_counter=0
while [ ${#checked_files[@]} != 0 ]; do
  for (( i=0; i < $files_count; i++ )); do
    file=${checked_files[i]}
    if [ ! -z "${file}" ]; then
      if [ -f $file ]; then
        unset checked_files[i]
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
