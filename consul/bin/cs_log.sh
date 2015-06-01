#!/bin/bash 

tail -f /var/tmp/consul_cluster.log | awk '
	/cs-srv1/ {print "\033[31m" $0 "\033[39m"}
	/cs-srv2/ {print "\033[32m" $0 "\033[39m"}
	/cs-srv3/ {print "\033[33m" $0 "\033[39m"}
	/cs-cli1/ {print "\033[34m" $0 "\033[39m"}
	/cs-cli2/ {print "\033[35m" $0 "\033[39m"}
	/cs-app1/ {print "\033[36m" $0 "\033[39m"}
	/cs-app2/ {print "\033[36m" $0 "\033[39m"}
'