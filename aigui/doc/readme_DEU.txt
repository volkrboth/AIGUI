Installationshinweise aigui Version 1.2
=======================================
Voraussetzungen
---------------
Die Installationsdateien können in einem beliebigen Verzeichnis gespeichert werden.
Für die Benutzer ist Lesezugriff ausreichend. Für den Start unter Windows ist eine 
Netzlaufwerkverbindung zum aigui-Verzeichnis notwendig, da der Start vom aigui-Verzeichnis 
erfolgen muss und der Befehl cd bei Windows für UNC-Pfade in der Form "//server/verzeichnis" 
nicht unterstuetzt wird.
Die benutzerspezifischen Konfigurationen werden im Home-Verzeichnis, Unterverzeichnis aigui, 
gespeichert. Bei Windows normalerweise unter C:\Dokumente und Einstellungen\<username>\aigui,
bei UNIX und Linux $HOME\aigui.

Weiterhin ist vom Mathematica das Java-Link Paket JLink erforderlich (JLink.jar, JLinkNativeLibrary.dll).

Installationsdateien
--------------------
aigui.jar              -  Ausführbares Java-Archiv
aigui-changelog.txt    -  Änderungslog
readme_DEU.txt         -  diese Datei
startWinJar.bat        -  Startdatei (siehe Anmerkung)
[conf]                 -  Verzeichnis für Konfigurationsdaten
    defaultProperties  -  Standard Properties
	functionOptions    -  Optionen für AnalogInsydes Funktionen (z.Z. nicht benutzt)
	functionOptions1   -  Optionen für AnalogInsydes Funktionen
	AIStyles.nb        -  Vorlage für Notebooks
[JLink-Windows]	       -  Java Link zu Mathematica (*)

(*) nicht erforderlich, wenn JLink bereits installiert ist.
	
Startdatei startWinJar.bat
--------------------------
Die Startdatei enthält den Befehl

java -Xmx512m -cp .\aigui.jar;JLink-Windows\JLink.jar aidc.aigui.Gui

Falls JLink bereits in einem anderen Verzeichnis installiert ist, 
muss der Pfad zu JLink.jar gegebenenfalls angepasst werden.

Sonstiges
---------
Fehler, Anmerkungen und Wünsche bitte an volker.boos@imms.de senden.
