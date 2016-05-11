#!/usr/bin/env bash
FSName="hdfs://hadoop-master.hobbithole.local"
HDFSDEST="/user/nick/shapefiles/"
ippBzipPath="/usr/bin"
ippBzip="${ippBzipPath}/bzip2"

for file in `ls *.zip`
do
    echo "Decompressing: " ${file}
    unzip ${file}
    rootname=${file%.*}
    echo ${rootname}
#    ${ippBzip} -kc ${rootname}
#    bzipName="${rootname}.bz2"
    echo "Uploading: " ${rootname}
    hdfs dfs -fs ${FSName} -put "${rootname}/" $HDFSDEST
    rm -rf "${rootname}/"
done
