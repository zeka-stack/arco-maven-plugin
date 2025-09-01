#!/bin/bash

chown -R zekastack:zekastack ./

if [[ ! "$1" ]] ;then
    su zekastack -c "bin/launcher -r prod -t"
else
    su zekastack -c "bin/launcher -r $1 -t"
fi
