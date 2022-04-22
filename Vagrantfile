# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure("2") do |config|
# The most common configuration options are documented and commented below.
# For a complete reference, please see the online documentation at
# https://docs.vagrantup.com.

# Every Vagrant development environment requires a box. You can search for
# boxes at https://vagrantcloud.com/search.

    config.vm.box = "centos7_mini"
    config.vm.box_url = "https://davidl.web.engr.illinois.edu/vms/centos7_mini.box"
    config.vm.box_download_checksum = "4897db055b26323d6d8d4a3f14b9c7d5a5e770e5d4c0185c0d2915c832719a1f"
    config.vm.box_download_checksum_type = "sha256"
    config.ssh.insert_key = false

    (1..2).each do |i|
        config.vm.define "client#{i}" do |client|
            client.vm.hostname = "client#{i}"

            client.vm.provider :virtualbox do |vb|
                vb.customize ["modifyvm", :id, "--memory", "1024"]
                vb.customize ["modifyvm", :id, "--cpus", "2"]
            end

            client.vm.network "private_network", ip: "192.168.0.2#{i}", virtualbox__intnet: "exchange_network", nic_type: "virtio"
            client.vm.provision "shell", path: "scripts/install.sh"
        end
    end

    (1..3).each do |i|
        config.vm.define "matching-engine#{i}" do |me|
            me.vm.hostname = "matching-engine#{i}"

            me.vm.provider :virtualbox do |vb|
                vb.customize ["modifyvm", :id, "--memory", "1024"]
                vb.customize ["modifyvm", :id, "--cpus", "2"]
            end

            me.vm.network "private_network", ip: "192.168.0.5#{i}", virtualbox__intnet: "exchange_network", nic_type: "virtio"
            me.vm.provision "shell", path: "scripts/install.sh"
            me.vm.provision "shell", path: "scripts/me_setup.sh"
        end
    end

    (1..3).each do |i|

        config.vm.define "gateway#{i}" do |gateway|
            gateway.vm.hostname = "gateway#{i}"

            gateway.vm.provider :virtualbox do |vb|
                vb.customize ["modifyvm", :id, "--memory", "1024"]
                vb.customize ["modifyvm", :id, "--cpus", "2"]
            end

            gateway.vm.network "private_network", ip: "192.168.0.10#{i}", virtualbox__intnet: "exchange_network", nic_type: "virtio"
            gateway.vm.provision "shell", path: "scripts/install.sh"
            gateway.vm.provision "shell", path: "scripts/gateway_setup.sh"
        end
    end

    config.vm.define "tickerplant" do |tp|
        tp.vm.hostname = "tickerplant"

        tp.vm.provider :virtualbox do |vb|
            vb.customize ["modifyvm", :id, "--memory", "1024"]
            vb.customize ["modifyvm", :id, "--cpus", "2"]
        end

        tp.vm.network "private_network", ip: "192.168.0.201", virtualbox__intnet: "exchange_network", nic_type: "virtio"
        tp.vm.provision "shell", path: "scripts/install.sh"
    end

end