#!/bin/bash

# Скрипт для прежатия .jar файлов с нулевой комперссией
#
# Аргументы:
# 1) абсолютный путь к каталогу с исходными .jar файлами;
# 2) абсолютный путь для сохранения сжатых с нулевой компрессией .jar файлов, так же к файлу будет добален суффикс '-zero-compress'
#
# Пример использования:
# jar_compressor.sh /opt/app/dependencies/ /opt/app/uncompressed.dependencies/
#
# Пути не должны совпадать.
#

set -e

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Error: Please provide both the source and destination paths with .jar."
  echo "Usage: jar_compressor.sh /path/to/source /path/to/destination"
  return 1
fi

if [ "$1" = "$2" ]; then
  echo "Error: $1 and $2 are the same. Please provide different paths."
  echo "Usage: jar_compressor.sh /path/to/source /path/to/destination"
  return 1
fi

jar_suffix='-zero-compress'

mkdir -p $2
cd $1
rm -rf tmp/

for f in ./*.jar; do
  echo "Processing $f file with zero compression ..."
  mkdir $1/tmp/

  {
    unzip -qq $1/$f -d $1/tmp
  } || {
    echo "Unzipping $f failed."
    exit 1
  }

  cd $1/tmp/

  {
    zip -0 -q -r $2/${f%.*}${jar_suffix}.jar . *
  } || {
    echo "Zipping $f failed."
    exit 1
  }

  cd $1
  rm -rf tmp/
done