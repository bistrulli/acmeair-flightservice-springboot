#!/bin/bash

# getrewardmiles
kubectl apply -f deployment-getrewardmiles.yaml
sleep 1
kubectl apply -f service-getrewardmiles.yaml
sleep 1

# queryflights
kubectl apply -f deployment-queryflights.yaml
sleep 1
kubectl apply -f service-queryflights.yaml
sleep 1