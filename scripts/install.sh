#!/bin/bash
PROJECT_ROOT=/vagrant
cd $PROJECT_ROOT

sudo yum install -y git
sudo yum install -y tcpdump
sudo yum install -y iperf
sudo yum install -y java-11-openjdk-devel
# change default java version
sudo alternatives --set javac /usr/lib/jvm/java-11-openjdk-11.0.14.1.1-1.el7_9.x86_64/bin/javac
sudo alternatives --set java /usr/lib/jvm/java-11-openjdk-11.0.14.1.1-1.el7_9.x86_64/bin/java
echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.14.1.1-1.el7_9.x86_64" >>~/.bash_profile
echo "export PATH=$JAVA_HOME/bin:$PATH" >>~/.bash_profile
source ~/.bash_profile
sudo route add -net 224.0.0.0 netmask 240.0.0.0 eth0
sudo sysctl net.core.rmem_max=2097152
sudo sysctl net.core.wmem_max=2097152

# install maven and configure
sudo wget --no-check-certificate https://dlcdn.apache.org/maven/maven-3/3.8.5/binaries/apache-maven-3.8.5-bin.tar.gz -P /tmp
sudo tar xf /tmp/apache-maven-3.8.5-bin.tar.gz -C /opt
sudo mv /opt/apache-maven-3.8.5/ /opt/maven/
echo "export M2_HOME=/opt/maven" | sudo tee -a /etc/profile.d/maven.sh
echo "export MAVEN_HOME=/opt/maven" | sudo tee -a /etc/profile.d/maven.sh
echo "export PATH=/opt/maven/bin:${PATH}" | sudo tee -a /etc/profile.d/maven.sh
sudo chmod +x /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh

echo "Finished installing dependencies"

chown -R vagrant:vagrant $PROJECT_ROOT
