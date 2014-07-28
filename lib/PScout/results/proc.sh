if [ $# -ne 1 ]
then
  echo "./proc.sh .txt"
  exit
fi

j=`basename $1 .txt`
cp $1 tmp
sed -i 's/^<//g' tmp
sed -i 's/> \(.*\)$//g' tmp

awk -F":| " '{print $1"."$4}' tmp | sort -k1 | uniq > $j.dat
