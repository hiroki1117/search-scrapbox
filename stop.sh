#!/bin/sh

pid=`lsof -i:8080 | tail -n 1 |  awk '{print $2}'`
kill "$pid"