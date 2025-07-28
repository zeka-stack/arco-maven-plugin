#!/bin/bash
# 本地测试使用, 因为没有 zekastack 用户

if [[ ! "$1" ]] ;then
    bin/launcher -T -r local &
else
    bin/launcher -T -r $1 &
fi

