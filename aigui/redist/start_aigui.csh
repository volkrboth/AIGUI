#!/bin/csh -f
# start script for unix shells
# TO DO: setup your current install path  for aigui and correct path for Mathematica's JLink
set AIGUI_INSTALL_PATH=~/java/aigui
java -Xmx512m -cp $AIGUI_INSTALL_PATH/aigui.jar:/progs/local/mathematica/AddOns/JLink/JLink.jar aidc.aigui.Gui
