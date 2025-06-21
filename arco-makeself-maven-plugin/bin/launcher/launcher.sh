#!/bin/bash

chown -R zekastack:zekastack ./

if [[ ! "$1" ]] ;then
    su zekastack -c "bin/server.sh -r prod -t"
else
    su zekastack -c "bin/server.sh -r $1 -t"
fi
