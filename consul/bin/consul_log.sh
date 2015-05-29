#!/bin/bash 

tail -f /var/tmp/consul_cluster.log | awk '
	/consul-srv1/ {print "\033[31m" $0 "\033[39m"}
	/consul-srv2/ {print "\033[32m" $0 "\033[39m"}
	/consul-srv3/ {print "\033[33m" $0 "\033[39m"}
	/consul-cli1/ {print "\033[34m" $0 "\033[39m"}
	/consul-cli2/ {print "\033[35m" $0 "\033[39m"}
	/consul-app1/ {print "\033[36m" $0 "\033[39m"}
	/consul-app2/ {print "\033[36m" $0 "\033[39m"}
'