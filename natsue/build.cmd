@echo off
cd ..
call umvn install
cd natsue\cradle
call ..\..\umvn package
