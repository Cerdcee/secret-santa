#!/bin/bash

if [ -z $1 ]
then
    echo "Please provide someone's id"
else
    grep "$1 ->" "secret_santa.backup"
fi