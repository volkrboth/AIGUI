ECHO OFF
IF %1" " == " "  (SET target=".\redist") ELSE SET target=%1
ECHO %target%
xcopy /D /Y aigui.jar %target%
xcopy /D /Y start_aigui.bat %target%
xcopy /D /Y start_aigui.csh %target%
xcopy /D /Y doc\readme.txt %target%
xcopy /D /Y doc\aigui_changelog.txt %target%
del /Q %target%\conf\*.*
xcopy /D /Y conf\defaultProperties.xml %target%\conf\
xcopy /D /Y conf\aigui.dtd %target%\conf\
xcopy /D /Y conf\aiguiconf.xml %target%\conf\
xcopy /D /Y conf\AIStyles.nb %target%\conf\
