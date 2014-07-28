#! /bin/bash

mydir=`dirname $1`
cd $mydir
rm -f log.resources resources.ap_

awk '{$1=""; print}' $1 > tmp

while read line
do
  # echo $line
  zip -n .arsc:.dat:.lua:.m4a:.mp3:.ogg:.png:.wav:.zip resources.ap_ "$line" >> log.resources
done < tmp

rm -f tmp
