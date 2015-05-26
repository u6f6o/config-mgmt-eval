#!/bin/bash

# stop and remove consul server and client containers
docker stop consulsrv1 consulsrv2 consulsrv3 consulcli1 consulcli2 >> /dev/null 2>&1
docker rm consulsrv1 consulsrv2 consulsrv3 consulcli1 consulcli2 >> /dev/null 2>&1

# rm and recreate final log file
rm cluster.log 
touch cluster.log

# recreate local container
docker build -t consulapp . 

# 1st consul server node
nohup docker run --name consulsrv1 -h consulsrv1 progrium/consul -server -bootstrap-expect 3 2>&1 | sed -l 's/^/[consul-srv1] /' >> cluster.log &
sleep 2
JOIN_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' consulsrv1)"

# 2nd consul server node
nohup docker run --name consulsrv2 -h consulsrv2 progrium/consul -server -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-srv2] /' >> cluster.log &
sleep 2

# 3rd consul server node
nohup docker run --name consulsrv3 -h consulsrv3 progrium/consul -server -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-srv3] /' >> cluster.log &
sleep 2

# 2 client nodes 
nohup docker run --name consulcli1 -h consulcli1 -p 8500:8500 progrium/consul -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-cli1] /' >> cluster.log &
nohup docker run --name consulcli2 -h consulcli2 progrium/consul -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-cli2] /' >> cluster.log &
sleep 2

curl -X PUT -d 'Heute ist ein schöner Tag' http://172.17.0.42:8500/v1/kv/web/key1
curl -X PUT -d 'Morgen nicht!' http://172.17.0.42:8500/v1/kv/web/key2
curl -X PUT -d 'Wetter soll Scheiße sein' http://172.17.0.42:8500/v1/kv/web/sub/key3

CLIENT1_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' consulcli1)"
CLIENT2_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' consulcli2)"

nohup docker run --name consulapp1 -h consulapp1 consulapp -join $CLIENT1_IP 2>&1 | sed -l 's/^/[consul-app1] /' >> cluster.log &
nohup docker run --name consulapp2 -h consulapp2 consulapp -join $CLIENT2_IP 2>&1 | sed -l 's/^/[consul-app2] /' >> cluster.log &
sleep 2
