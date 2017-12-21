#!/bin/bash

while [ 1 ]
do
   echo 11213 move_remain_count
   echo stats migration | nc localhost 11213 | grep -a move_remain_count
   echo 11215 move_remain_count
   echo stats migration | nc localhost 11215 | grep -a move_remain_count
   echo 11216 move_remain_count
   echo stats migration | nc localhost 11216 | grep -a move_remain_count
   sleep 1
done
