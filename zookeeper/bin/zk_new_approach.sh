#!/bin/bash 

# build exhibitor container 
docker rm -f zksrv1 zksrv2 zksrv3  

conf_container=zksrv1

for i in {1..3}; do 
if [ "${i}" == "1" ]; then 
		nohup docker run -v /exhibitor --name "zksrv${i}" -h "zksrv${i}" -e ZOO_ID=${i} -p 8080:8080 netflixoss/exhibitor:1.5.2 2>&1 | sed -u "s/^/[zk-srv${i}] /" >> /var/tmp/zookeeper_cluster.log &
    else 
    	nohup docker run --volumes-from ${conf_container} --name "zksrv${i}" -h "zksrv${i}" -e ZOO_ID=${i} netflixoss/exhibitor:1.5.2 2>&1 | sed -u "s/^/[zk-srv${i}] /" >> /var/tmp/zookeeper_cluster.log &
	fi
done

sleep 10
server_config=`cat config/exhibitor.properties`
server_config="${server_config}\ncom.netflix.exhibitor.servers-spec="

for i in {1..3}; do 
	container_name="zksrv${i}"
	echo $container_name
	container_ip=`docker inspect --format '{{.NetworkSettings.IPAddress}}' ${container_name}`
	echo $container_ip
	server_config="${server_config}${i}\:${container_ip},"	
done; 

echo $server_config
server_config=`echo ${server_config} | sed 's/,$//'`

# Write the configuration to zksrv1
echo $server_config | docker run -i --rm --volumes-from ${conf_container} busybox sh -c 'cat > /exhibitor/exhibitor.properties'

#zoo_links='--link zoo1:zoo1 --link zoo2:zoo2 --link zoo3:zoo3'
#zoo_connect='zoo1:2181,zoo2:2181,zoo3:2181'

