AIGUI Installation Notes Version 1.2
=======================================

Requirements
------------
AIGUI requires Mathematica 5 or 6, and AnalogInsydes 2 installed on your computer.
Further you must have installed the Mathematica JLink interface.
It is part of the Mathematica installation: <Mathematica_installation_path>\SystemFiles\Links\JLink\JLink.jar

Windows
-------
Unpack the installation files from aigui*.zip into a directory of your choice, e.g. C:\aigui
You can it also install on a network drive, users needs read acces only.
But this directory must be reached over a network drive letter, UNC paths like \\myserver\aigui are not supported.

Edit the file start_aigui.bat 
* replace "your_aigui_install_dir" with your aigui dircetory 
* replace "jlink_directory" with the directory contains JLink.jar

SET AIGUI_INSTALL_PATH=your_aigui_install_dir
java -Xmx512m -cp %AIGUI_INSTALL_PATH%\aigui.jar;jlink_directory\JLink.jar aidc.aigui.Gui

The files in AIGUI_INSTALL_PATH can set to read only for the users.
The user-specific configurations are saved in the user's home directory, subdirectory aigui.
For Windows it is usually C:\Users\<username>\aigui.

Linux / Unix
------------
Unpack the installation files from aigui*.zip into a directory of your choice, e.g. /progs/aigui

Edit the file start_aigui.csh or write your own startup script.
* replace "your_aigui_install_dir" with your aigui dircetory 
* replace "mathematica_dir" with the directory installed Mathematica, which contains AddOns/JLink/JLink.jar 

#!/bin/csh -f
# start script for unix shells
# TO DO: setup your current install path  for aigui and correct path for Mathematica's JLink
set AIGUI_INSTALL_PATH=your_aigui_install_dir
java -Xmx512m -cp $AIGUI_INSTALL_PATH/aigui.jar:/mathematica_dir/AddOns/JLink/JLink.jar aidc.aigui.Gui

The files in AIGUI_INSTALL_PATH can set to read only for the users.
The user-specific configurations are saved in your home directory, subdirectory aigui ( ~/aigui ).


Installation Files
--------------------
aigui.jar               - Executable Java Archive
aigui-changelog.txt     - change log
readme.txt              - this file
start_aigui.bat         - startup file for Windows (you must edit!)
start_aigui.csh         - startup file for UNIX and Linux (you must edit!)
[conf]                  - directory for configuration data
  aigui.dtd             - Data type description for aiguiconf.xml
  aiguiconf.xml         - options for AnalogInsydes functions
  AIStyles.nb           - Mathematica notebook template for AIGUI
  defaultProperties.xml - Default properties 


Feedback
--------
For errors, comments and requests, please contact author via GitHub.
