#!/bin/bash

docker stop consulsrv1 consulsrv2 consulsrv3 consulcli1 consulcli2 >> /dev/null 2>&1
docker rm consulsrv1 consulsrv2 consulsrv3 consulcli1 consulcli2 >> /dev/null 2>&1

rm cluster.log 
touch cluster.log


# 1st consul server node
nohup docker run --name consulsrv1 -h consulsrv1 progrium/consul -server -bootstrap-expect 3 2>&1 | sed -l 's/^/[consul-srv1] /'  >> cluster.log &
sleep 5
JOIN_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' consulsrv1)"
echo $JOIN_IP 

# 2nd consul server node
nohup docker run --name consulsrv2 -h consulsrv2 progrium/consul -server -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-srv2] /'  >> cluster.log &
sleep 2

# 3rd consul server node
nohup docker run --name consulsrv3 -h consulsrv3 progrium/consul -server -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-srv3] /'  >> cluster.log &
sleep 2

# 2 client nodes 
nohup docker run --name consulcli1 -h consulcli1 progrium/consul -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-cli1] /'  >> cluster.log &
nohup docker run --name consulcli2 -h consulcli2 progrium/consul -join $JOIN_IP 2>&1 | sed -l 's/^/[consul-cli2] /'  >> cluster.log &


