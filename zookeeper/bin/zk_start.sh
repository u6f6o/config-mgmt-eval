#!/bin/bash


# build config wrapper app
#../config-wrapper/gradlew -p ../config-wrapper installDist
#cp -rf ../config-wrapper/build/install/config-wrapper dist/

# stop and remove zookeeper server and zkclient containers
docker rm -f zksrv1 zksrv2 zksrv3 zkcli1 zkcli2 zkapp1 zkapp2 >> /dev/null 2>&1

# cleanup logfile
cat /dev/null > /var/tmp/zookeeper_cluster.log 

# recreate local container
#docker build -t zkapp -f AppFile . 
docker build -t zookeeper -f ExhibitorFile . 

# 1st zookeeper server node
nohup docker run -name zksrv1 -h zksrv1 zookeeper 2>&1 | sed -u 's/^/[zk-srv1] /' >> /var/tmp/zookeeper_cluster.log &
sleep 2
JOIN_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' zksrv1)"

# 2nd zookeeper server node
nohup docker run -name zksrv2 -h zksrv2 zookeeper 2>&1 | sed -u 's/^/[zk-srv2] /' >> /var/tmp/zookeeper_cluster.log &
sleep 2

# 3rd zookeeper server node
nohup docker run -name zksrv3 -h zksrv3 -p 8080:8080 --link zksrv1:zksrv1 --link zksrv2:zksrv2 zookeeper 2>&1 | sed -u 's/^/[zk-srv3] /' >> /var/tmp/zookeeper_cluster.log &
sleep 2

# 2 client nodes 
# nohup docker run --name zkcli1 -h zkcli1 -p 8501:8500 zookeeper -join $JOIN_IP 2>&1 | sed -u 's/^/[zk-cli1] /' >> /var/tmp/zookeeper_cluster.log &
# nohup docker run --name zkcli2 -h zkcli2 -p 8502:8500 zookeeper -join $JOIN_IP 2>&1 | sed -u 's/^/[zk-cli2] /' >> /var/tmp/zookeeper_cluster.log &
# sleep 2
#
# CLIENT1_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' zkcli1)"
# CLIENT2_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' zkcli2)"

# put fake data
# curl -X PUT -d 'bar' http://localhost:8500/v1/kv/zkapp/ps/foo
# sleep 2

# nohup docker run -e "ZOOKEEPER_CLIENT_IP=$CLIENT1_IP" --name zkapp1 -h zkapp1 zkapp 2>&1 | sed -u 's/^/[zk-app1] /' >> /var/tmp/zookeeper_cluster.log &
# nohup docker run -e "ZOOKEEPER_CLIENT_IP=$CLIENT2_IP" --name zkapp2 -h zkapp2 zkapp 2>&1 | sed -u 's/^/[zk-app2] /' >> /var/tmp/zookeeper_cluster.log &
# sleep 2