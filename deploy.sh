#!/bin/bash


# Vue has been compiled, now move these files into the backend so that they can be served
./build_frontend.sh
cd backend
mvn clean package
