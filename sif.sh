#! /bin/bash

if [ $# -ne 2 ]
then
  echo "Usage: ./sifa.sh in.apk my_script.java"
  exit
fi

inapk=$1
script=$2

# 0. unzip and do "dalvik --> jvm" conversion

# apktool to get Manifest.xml
# unzip to get log and regenerate the resources.ap_
if [ 0 -eq 1 ]
then

unzip $inapk &> log.unzip
awk -f /home/haos/sifa/scripts/proc_unzip_log.awk log.unzip > log.ok

/home/haos/sifa/scripts/log2cmd.sh log.ok


# dex2jar classes.dex
jarfile=classes_dex2jar.jar
/home/haos/uLens/dex2jar/dex2jar-0.0.9.8/dex2jar.sh classes.dex
mkdir -p classes
cp $jarfile classes
cd classes
jar xf $jarfile
rm $jarfile
cd ..

echo "dalvik --> jvm done..."

fi

# 1. compile my_script.java
#javac -cp sifa.jar:.
#echo "javac $script done..."

# 2. run SIFA runtime to interpret my_script and perform instrumentation

# 3. do "jvm --> dalvik" conversion for updated class files and repack apk
