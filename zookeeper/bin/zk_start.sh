#!/bin/bash


# build config wrapper app
../config-wrapper/gradlew -p ../config-wrapper installDist
cp -rf ../config-wrapper/build/install/config-wrapper dist/

# stop and remove consul server and zkclient containers
docker rm -f zksrv1 zksrv2 zksrv3 zkcli1 zkcli2 zkapp1 zkapp2 >> /dev/null 2>&1

# cleanup logfile
cat /dev/null > /var/tmp/zookeeper_cluster.log 

# recreate local container
docker build -t zkapp -f AppFile . 
docker build -t consul -f ConsulFile . 

# 1st consul server node
nohup docker run --name zksrv1 -h zksrv1 -p 8500:8500 consul -server -ui-dir /ui -config-dir /config -bootstrap-expect 3 2>&1 | sed -u 's/^/zksrv1] /' >> /var/tmp/consul_cluster.log &
sleep 2
JOIN_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' zksrv1)"

# 2nd consul server node
nohup docker run --name zksrv2 -h zksrv2 consul -server -join $JOIN_IP 2>&1 | sed -u 's/^/[zksrv2] /' >> /var/tmp/consul_cluster.log &
sleep 2

# 3rd consul server node
nohup docker run --name zksrv3 -h zksrv3 consul -server -join $JOIN_IP 2>&1 | sed -u 's/^/[zksrv3] /' >> /var/tmp/consul_cluster.log &
sleep 2

# 2 client nodes 
nohup docker run --name zkcli1 -h zkcli1 -p 8501:8500 consul -join $JOIN_IP 2>&1 | sed -u 's/^/[zkcli1] /' >> /var/tmp/consul_cluster.log &
nohup docker run --name zkcli2 -h zkcli2 -p 8502:8500 consul -join $JOIN_IP 2>&1 | sed -u 's/^/[zkcli2] /' >> /var/tmp/consul_cluster.log &
sleep 2

CLIENT1_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' zkcli1)"
CLIENT2_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' zkcli2)"

# put fake data
curl -X PUT -d 'bar' http://localhost:8500/v1/kv/zkapp/ps/foo
sleep 2

nohup docker run -e "CONSUL_CLIENT_IP=$CLIENT1_IP" --name zkapp1 -h zkapp1 zkapp 2>&1 | sed -u 's/^/[zkapp1] /' >> /var/tmp/consul_cluster.log &
nohup docker run -e "CONSUL_CLIENT_IP=$CLIENT2_IP" --name zkapp2 -h zkapp2 zkapp 2>&1 | sed -u 's/^/[zkapp2] /' >> /var/tmp/consul_cluster.log &
sleep 2