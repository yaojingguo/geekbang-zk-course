#!/usr/bin/env bash

quorum_dir=/data/zk/quorum
rm -fr $quorum_dir
mkdir -p $quorum_dir

for no in `seq 3`; do
  node_dir="$quorum_dir/node$no"
  mkdir $node_dir
  echo $no > "$node_dir/myid"
done

