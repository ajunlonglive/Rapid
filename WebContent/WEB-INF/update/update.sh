#!/bin/bash

# delete all files not called update.*
ls | grep -v update. | xargs sudo rm -r

# copy the rapid.war from the home folder
cp ~/rapid.war .

# update
java -jar update.jar
