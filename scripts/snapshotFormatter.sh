#!/usr/bin/env bash
. zkEnv.sh
export CLASSPATH="$CLASSPATH"
java org.apache.zookeeper.server.SnapshotFormatter "$@"