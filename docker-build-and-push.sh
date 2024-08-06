#!/bin/bash

IMAGE_NAME="rpizziol/acmeair-flightservice-springboot"
TAG="0.17"

docker build -t $IMAGE_NAME:$TAG . && docker push $IMAGE_NAME:$TAG
