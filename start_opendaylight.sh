#!/bin/bash

rm -rf distribution/karaf/target/assembly/data/tmp/ClassInfo
rm -rf distribution/karaf/target/assembly/data/tmp/DBPathInfo
rm -rf distribution/karaf/target/assembly/data/tmp/ScouterRule
rm -rf distribution/karaf/target/assembly/data/tmp/TraceData

./distribution/karaf/target/assembly/bin/karaf
