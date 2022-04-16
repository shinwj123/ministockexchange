#!/bin/bash
PROJECT_ROOT=/home/vagrant/dev
mkdir -p $PROJECT_ROOT
cd $PROJECT_ROOT

sudo yum install -y git
sudo yum install -y tcpdump
sudo yum install -y iperf
sudo yum install -y java-11-openjdk-devel
sudo yum install -y maven
sudo route add -net 224.0.0.0 netmask 240.0.0.0 eth0
sudo sysctl net.core.rmem_max=2097152
sudo sysctl net.core.wmem_max=2097152

echo "Finished installing dependencies"

TOKEN=ENTER_YOUR_TOKEN
git clone https://oath2:${TOKEN}@gitlab.engr.illinois.edu/ie598_high_frequency_trading_spring_2022/ie498_hft_spring_2022_group_03/group_03_project.git
cd $PROJECT_ROOT/group_03_project
git checkout daniel

chown -R vagrant:vagrant $PROJECT_ROOT