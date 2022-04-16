#!/bin/bash
PROJECT_ROOT=/home/vagrant/dev
mkdir -p $PROJECT_ROOT
cd $PROJECT_ROOT

sudo yum install -y git
sudo yum install -y tcpdump
sudo yum install -y iperf
sudo yum install -y java-11-openjdk-devel
sudo yum install -y maven
# change default java version
sudo alternatives --set javac /usr/lib/jvm/java-11-openjdk-11.0.14.1.1-1.el7_9.x86_64/bin/javac
sudo alternatives --set java /usr/lib/jvm/java-11-openjdk-11.0.14.1.1-1.el7_9.x86_64/bin/java
echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.14.1.1-1.el7_9.x86_64" >>~/.bash_profile
echo "export PATH=$JAVA_HOME/bin:$PATH" >>~/.bash_profile
source ~/.bash_profile
sudo route add -net 224.0.0.0 netmask 240.0.0.0 eth0
sudo sysctl net.core.rmem_max=2097152
sudo sysctl net.core.wmem_max=2097152

echo "Finished installing dependencies"

TOKEN=YOUR_TOKEN
git clone https://oath2:${TOKEN}@gitlab.engr.illinois.edu/ie598_high_frequency_trading_spring_2022/ie498_hft_spring_2022_group_03/group_03_project.git
cd $PROJECT_ROOT/group_03_project
git checkout daniel

chown -R vagrant:vagrant $PROJECT_ROOT