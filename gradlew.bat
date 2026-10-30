@echo off
setlocal
set "APP_HOME=%~dp0"
set "JAVA_EXE=java.exe"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"
if exist "%WRAPPER_JAR%" goto run
"%JAVA_EXE%" "%APP_HOME%gradle\wrapper\Bootstrap.java" "%WRAPPER_JAR%"
if errorlevel 1 exit /b 1
:run
"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
