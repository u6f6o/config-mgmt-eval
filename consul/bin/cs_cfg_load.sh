#!/bin/bash

cat $1 | dos2unix | grep -P '^\w' |  perl -lpe 's|^(\w.*?)=(.*)$|curl -X PUT -d '\''\2\'\'' http://localhost:8500/v1/kv/app/ps/\1|' | source /dev/stdin