#!/bin/bash

cd "./vpa/"

kubectl apply -f vpa-getrewardmiles.yaml
sleep 1

kubectl apply -f vpa-queryflights.yaml
sleep 1

cd "../"