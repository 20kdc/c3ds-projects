#!/bin/sh
cd ..
mvn clean
mvn install
cd natsue
mvn package
