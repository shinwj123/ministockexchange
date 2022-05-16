PROJECT_ROOT=/vagrant
cd $PROJECT_ROOT
sudo yum group install -y "Development Tools"
sudo yum install -y nodejs npm
wget https://www.python.org/ftp/python/3.9.11/Python-3.9.11.tgz
tar -xf Python-3.9.11.tgz
sudo yum install -y python3-devel
cd Python-3.9.11
./configure
make -j 2
sudo make altinstall
cd $PROJECT_ROOT
pip3.9 install wheel
pip3.9 install quickfix
python3.9 -m pip install perspective-python websocket-client