#!/bin/bash

PROPS_FILE=$1

cat $PROPS_FILE | dos2unix | grep -P '^\w' |  perl -lpe 's|^(\w.*?)=(.*)$|curl -X PUT -d '\''\2\'\'' http://localhost:8500/v1/kv/app/ps/\1|' | source /dev/stdin