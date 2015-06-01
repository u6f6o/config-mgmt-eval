#!/bin/bash


# build config wrcsapper csapp
../config-wrapper/gradlew -p ../config-wrapper installDist
cp -rf ../config-wrapper/build/install/config-wrapper dist/

# stop and remove consul server and csclient containers
docker rm -f cssrv1 cssrv2 cssrv3 cscli1 cscli2 csapp1 csapp2 >> /dev/null 2>&1

# cleanup logfile
cat /dev/null > /var/tmp/consul_cluster.log 

# recreate local container
docker build -t csapp -f AppFile . 
docker build -t consul -f ConsulFile . 

# 1st consul server node
nohup docker run --name cssrv1 -h cssrv1 -p 8500:8500 consul -server -ui-dir /ui -config-dir /config -bootstrap-expect 3 2>&1 | sed -u 's/^/cs-srv1] /' >> /var/tmp/consul_cluster.log &
sleep 2
JOIN_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' cssrv1)"

# 2nd consul server node
nohup docker run --name cssrv2 -h cssrv2 consul -server -join $JOIN_IP 2>&1 | sed -u 's/^/[cs-srv2] /' >> /var/tmp/consul_cluster.log &
sleep 2

# 3rd consul server node
nohup docker run --name cssrv3 -h cssrv3 consul -server -join $JOIN_IP 2>&1 | sed -u 's/^/[cs-srv3] /' >> /var/tmp/consul_cluster.log &
sleep 2

# 2 csclient nodes 
nohup docker run --name cscli1 -h cscli1 -p 8501:8500 consul -join $JOIN_IP 2>&1 | sed -u 's/^/[cs-cli1] /' >> /var/tmp/consul_cluster.log &
nohup docker run --name cscli2 -h cscli2 -p 8502:8500 consul -join $JOIN_IP 2>&1 | sed -u 's/^/[cs-cli2] /' >> /var/tmp/consul_cluster.log &
sleep 2

CLIENT1_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' cscli1)"
CLIENT2_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' cscli2)"

# put fake data
curl -X PUT -d 'bar' http://localhost:8500/v1/kv/app/ps/foo
sleep 2

nohup docker run -e "CONSUL_CLIENT_IP=$CLIENT1_IP" --name csapp1 -h csapp1 csapp 2>&1 | sed -u 's/^/[cs-app1] /' >> /var/tmp/consul_cluster.log &
nohup docker run -e "CONSUL_CLIENT_IP=$CLIENT2_IP" --name csapp2 -h csapp2 csapp 2>&1 | sed -u 's/^/[cs-app2] /' >> /var/tmp/consul_cluster.log &
sleep 2