#!/bin/bash

# stop and remove consul server and client containers
docker stop srv1 srv2 srv3 cli1 cli2 app1 app2 >> /dev/null 2>&1
docker rm srv1 srv2 srv3 cli1 cli2 app1 app2 >> /dev/null 2>&1

# cleanup logfile
cat /dev/null > cluster.log 

# recreate local container
docker build -t app . 

# 1st consul server node
nohup docker run --name srv1 -h srv1 -p 8500:8500 progrium/consul -server -ui-dir /ui -bootstrap-expect 3 2>&1 | sed -u 's/^/consul-srv1] /' >> cluster.log &
sleep 2
JOIN_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' srv1)"

# 2nd consul server node
nohup docker run --name srv2 -h srv2 progrium/consul -server -join $JOIN_IP 2>&1 | sed -u 's/^/[consul-srv2] /' >> cluster.log &
sleep 2

# 3rd consul server node
nohup docker run --name srv3 -h srv3 progrium/consul -server -join $JOIN_IP 2>&1 | sed -u 's/^/[consul-srv3] /' >> cluster.log &
sleep 2

# 2 client nodes 
nohup docker run --name cli1 -h cli1 -p 8501:8500 progrium/consul -join $JOIN_IP 2>&1 | sed -u 's/^/[consul-cli1] /' >> cluster.log &
nohup docker run --name cli2 -h cli2 -p 8502:8500 progrium/consul -join $JOIN_IP 2>&1 | sed -u 's/^/[consul-cli2] /' >> cluster.log &
sleep 2

CLIENT1_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' cli1)"
CLIENT2_IP="$(docker inspect -f '{{.NetworkSettings.IPAddress}}' cli2)"

curl -X PUT -d 'bar' http://$CLIENT1_IP:8500/v1/kv/app/ps/foo

nohup docker run -e "CONSUL_CLIENT_IP=$CLIENT1_IP" --name app1 -h app1 app 2>&1 | sed -u 's/^/[consul-app1] /' >> cluster.log &
nohup docker run -e "CONSUL_CLIENT_IP=$CLIENT2_IP" --name app2 -h app2 app 2>&1 | sed -u 's/^/[consul-app2] /' >> cluster.log &
sleep 2


# tail -f cluster.log | awk '
#   /consul-srv1/ {print "\033[31m" $0 "\033[39m"}
#   /consul-srv2/ {print "\033[32m" $0 "\033[39m"}
#   /consul-srv3/ {print "\033[33m" $0 "\033[39m"}
#   /consul-cli1/ {print "\033[34m" $0 "\033[39m"}
#   /consul-cli2/ {print "\033[35m" $0 "\033[39m"}
#   /consul-app1/ {print "\033[36m" $0 "\033[39m"}
#   /consul-app2/ {print "\033[36m" $0 "\033[39m"}
# '