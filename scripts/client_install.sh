PROJECT_ROOT=/vagrant
cd $PROJECT_ROOT
sudo yum install -y openssl-devel libffi-devel bzip2-devel sqlite-devel
sudo yum group install -y "Development Tools"
sudo yum install -y nodejs npm
curl --silent --location https://dl.yarnpkg.com/rpm/yarn.repo | sudo tee /etc/yum.repos.d/yarn.repo
sudo rpm --import https://dl.yarnpkg.com/rpm/pubkey.gpg
sudo yum install -y yarn
wget https://www.python.org/ftp/python/3.9.11/Python-3.9.11.tgz
tar -xf Python-3.9.11.tgz
sudo yum install -y python3-devel
cd Python-3.9.11
./configure
make clean
make -j 2
sudo make altinstall
echo "export PATH=/usr/local/bin:$PATH" >>~/.bash_profile
source ~/.bash_profile
cd $PROJECT_ROOT
pip3.9 install wheel
pip3.9 install quickfix
python3.9 -m pip install perspective-python websocket-client