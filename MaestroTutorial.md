# Introduction #

Thanks for trying Maestro! This short tutorial prepares you with the basic knowledge about how to use Maestro as an OpenFlow controller. Because it is an OpenFlow controller, this tutorial heavily relies on the latest [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010). In this tutorial we are only going to shown you the most straight-forward way of setting up one such virtual environment using Ubuntu Linux 32bit, to be able to run Maestro to control the switches inside it. For a complete explanation about setting setup a virtual machine and the emulated network topology, and for all other kinds of operating systems you are using, please check the [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010) to know more details. Of course if you are already very familiar with OpenFlow or you are already running your own OpenFlow network, you can skip this OpenFlowTutorial section below and go directly with Maestro.


# The OpenFlowTutorial - Setup the Virtual OpenFlow Environment #

As a quick summary of the [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010), you basically need to follow these steps below to setup an emulated virtual OpenFlow network environment:

  * Download the VM image file from [here](http://openflowswitch.org/downloads/OpenFlowTutorial-081910.vmware.zip). Then in your host machine, run:

```
  [host-machine]$ unzip OpenFlowTutorial-081910.vmware.zip
```

> and you will get a directory named "OpenFlowVM.vmwarevm", which we call the image directory from now on.

  * Install [VMPlayer](https://www.vmware.com/tryvmware/?p=player&lp=default). You need to register for an evaluation version. For example, for the Ubuntu 32bit version, you will download the file named "VMware-Player-3.1.3-324285.i386.bundle". Then add execute permission to this file by:

```
  [host-machine]$ chmod +x VMware-Player-3.1.3-324285.i386.bundle
```

> Then, execute this file by:

```
  [host-machine]$ sudo ./VMware-Player-3.1.3-324285.i386.bundle
```

> It will pop up the dialog to finish the installation. Just default settings will work.

  * Change directory to the image directory and start the virtual machine by:

```
  [host-machine]$ cd OpenFlowVM.vmwarevm
  [host-machine]$ vmplayer OpenFlowVM.vmx
```

> Both the username and the password are "openflow". After the virtual machine boots, you will get a window for the virtual machine with a shell terminal running. Run:

```
  [virtual-machine]$ ifconfig eth0
```

> to get the virtual machine's IP address. For example, you will get something like "192.168.212.128". This will be the IP address that you use SSH to connect to this virtual machine from the local host machine, by:

```
  [host-machine]$ ssh openflow@192.168.212.128
```

  * Inside such a SSH terminal connected to the virtual machine (or the original windowed virtual machine terminal), start a network topology using mininet by:

```
  [virtual-machine]$ sudo mn --topo single,3 --mac --switch ovsk --controller remote
```

> This creates a network with one OpenFlow switch and three hosts described in the [Start Network](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010#Start_Network) section in the [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010). For more explanations, please refer to the [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010).

  * In the mininet console, you can try to let host h2 ping h3 to see whether they can talk to each other by:

```
  [virtual-machine]mininet> h2 ping -c3 h3
```

> Of course, because now we don't have a working controller, the switch is not functioning, thus the ping will fail.

After done all of these previous steps, you have already successfully created a virtual OpenFlow network environment! Now we are right at before the [Start Controller and view Startup messages in Wireshark](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010#Start_Controller_and_view_Startup_messages_in_Wireshark) section in the [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010). You can definitely continue following that tutorial and try their basic controller and NOX. But since this tutorial is about Maestro, we will use this virtual OpenFlow network environment to try Maestro instead. Let's go~


# Run Maestro #

What we are going to do is to replace all the default controller and NOX part in the [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010) with Maestro.

Maestro is based on Java, so to be able to run Maestro you first need to have a Java environment. What we suggest to do is download the latest JDK from [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to the virtual machine. Choose the right binary according to what operating system you have. For the Ubuntu 32bit, you can first copy the url to the installation file "jdk-6u22-linux-i586.bin", and then do:

```
[virtual-machine]$ wget THE_COPIED_URL
[virtual-machine]$ chmod +x jdk-6u22-linux-i586.bin
[virtual-machine]$ ./jdk-6u22-linux-i586.bin
```

After expanding and installing the JDK binary, you should also set the $JAVA\_HOME environment variable. For bash users, you can add

```
export JAVA_HOME=/the/path/to/where/you/installed/your/JDK
```

into your ~/.bashrc file.

If you do not want to manually install JDK, you can also just run (for Debian OSes)

```
[virtual-machine]$ sudo apt-get update
[virtual-machine]$ sudo apt-get install openjdk-6-jdk
```

or for RPM-compatible OSes, you can run

```
[virtual-machine]$ sudo yum update
[virtual-machine]$ sudo yum install java-1.6.0-openjdk
```

Next, because Maestro relies on Apache Ant to manage compilation of the code, you need to install it before running Maestro. For Debian OSes, you can just run

```
[virtual-machine]$ sudo apt-get update
[virtual-machine]$ sudo apt-get install ant
```

For RPM-compatible OSes, you can run

```
[virtual-machine]$ sudo yum update
[virtual-machine]$ sudo yum install ant
```

For Windows users or you can choose to download and install it manually from [here](http://ant.apache.org/bindownload.cgi).

Yes, that will be all the dependencies! The next step is downloading the Maestro code  [here](http://maestro-platform.googlecode.com/files/Maestro-0.1.0.zip). After done this, unzip it so you get the "Maestro-0.1.0" directory. This will be the Maestro working directory. To build the entire project, just run:

```
[virtual-machine]$ cd Maestro-0.1.0
[virtual-machine]$ ant
```

It will compile all the .java files and generate corresponding .class files in the "build/" directory. You can also build the javadoc webpage files by running

```
[virtual-machine]$ ant javadoc
```

These webpage files will be in the "javadoc/" directory, which you can open with a web browser to see all the documentations of the code. To clean up the build, just run

```
[virtual-machine]$ ant clean
```

To run Maestro, you need to provide two configuration files. First one is a configuration file which contains a bunch of parameter numbers. There is one example file which can also be used as default parameter settings, the "conf/openflow.conf" file. See [Using and Programming in Maestro](http://maestro-platform.googlecode.com/files/programming.pdf) for detailed discussion. The second file is the DAG configuration file. Two examples are provided: "learningswitch.dag" and "routing.dag". As indicated by their names, in them applications related to build a learning-switch like network, and a routing network are composed to fulfill the correct functionality. More details about this can also be found in the design document of Maestro: [Using and Programming in Maestro](http://maestro-platform.googlecode.com/files/programming.pdf)

For example, if you want to run the learning-switch functionality, in the Maestro working directory, you just type

```
[virtual-machine]$ $JAVA_HOME/bin/java -cp build/ sys.Main conf/openflow.conf conf/learningswitch.dag 1
```

to start the system. The last argument, "1" means that we want to run Maestro in interactive mode. Then you will see the Maestro command console, which at this point has very limited functionalities. (If the last argument is 0, then the command console will not be started.) For example, in the console, you can type

```
[virtual-machine]Maestro> print
                          Please input the name of the view instance:
[virtual-machine]Maestro> switches
```

to print out information about all connected OpenFlow switches in the network.

After starting Maestro, you will see the two switches get connected to the controller, and then you can let one end host to ping the other one to verify that it works. Then you probably also want to try the routing version of Maestro. You can use all utilities provided by the [OpenFlowTutorial](http://www.openflowswitch.org/wk/index.php/HOTITutorial2010) to trace all the OpenFlow protocol messages exchanged between the controller and the switches, and use tcpdump to trace data plane packets within the network.

If your operating system in which you would like to run Maestro has IPv6 enabled, run:

`$JAVA_HOME/bin/java -Djava.net.preferIPv4Stack=true -cp build/ sys.Main conf/openflow.conf conf/learningswitch.dag 1`

to ensure that Maestro is listening on an IPv4 addresses. We will fix this issue in further releases to let Maestro be able to listen on IPv6 port addresses.

And that will be all of the tutorial~ Hope you enjoy it. For more detailed information about Maestro, please check out the design document, and all other papers. Please feel free to send us emails about any problems you may encounter. Thanks again for choosing Maestro :)