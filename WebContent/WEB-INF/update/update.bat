REM - Make sure the folder below matches the folder this .bat file is in, and that you run this .bat file as a Windows Administrator

%windir%\system32\cmd.exe /C "cd \Tomcat\webapps\rapid\WEB-INF\update & java -jar update.jar"
