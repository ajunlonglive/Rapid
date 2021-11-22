REM run cmd.exe as admin and run update.jar - make sure the folder below is correct! - also not the replace parameter to only replace existing folders/files

%windir%\system32\cmd.exe /C "cd \Tomcat\webapps\rapid\WEB-INF\update & java -jar update.jar replace"
