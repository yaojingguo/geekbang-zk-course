#!/usr/bin/env bash
. zkEnv.sh

export CLASSPATH="build/classes/java/main:$CLASSPATH"
mkdir -p data
java org.yao.watchclient.Executor "$@"
