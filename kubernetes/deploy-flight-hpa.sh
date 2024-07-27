#!/bin/bash

kubectl apply -f hpa-getrewardmiles.yaml
sleep 1

kubectl apply -f hpa-queryflights.yaml
sleep 1