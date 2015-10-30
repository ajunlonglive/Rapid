#!/bin/bash

ls | grep -v update. | xargs sudo rm -r

sudo cp ~/rapid.war .

sudo java -jar update.jar
