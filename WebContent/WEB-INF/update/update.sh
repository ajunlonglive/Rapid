#!/bin/bash

# delete all files not called update.*
ls | grep -v update. | xargs sudo rm -r

# copy the rapid.war from the home folder
sudo cp ~/rapid.war .

# update
sudo java -jar update.jar
