#!/bin/sh
cd ./initiator
if [[ $1 -eq 1 ]]
then
    python3.9 client.py client1.cfg
elif [[ $1 -eq 2 ]]
then
    # python3 client.py client2.cfg -s order_scripts/test_script1.txt
    python3.9 client.py client2.cfg
else
    echo "client number either 1 or 2"
fi
