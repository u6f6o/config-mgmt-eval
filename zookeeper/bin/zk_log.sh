#!/bin/bash 

tail -f /var/tmp/zookeeper_cluster.log | awk '
	/zk-srv1/ {print "\033[31m" $0 "\033[39m"}
	/zk-srv2/ {print "\033[32m" $0 "\033[39m"}
	/zk-srv3/ {print "\033[33m" $0 "\033[39m"}
	/zk-cli1/ {print "\033[34m" $0 "\033[39m"}
	/zk-cli2/ {print "\033[35m" $0 "\033[39m"}
	/zk-app1/ {print "\033[36m" $0 "\033[39m"}
	/zk-app2/ {print "\033[36m" $0 "\033[39m"}
'