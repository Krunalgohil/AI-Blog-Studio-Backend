@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Maven Wrapper startup batch script
@REM -----------------------------------

@IF "%__MVNW_ARG0_S%"=="" (SET __MVNW_ARG0_S=%0) ELSE (GOTO :mvnw_main)
@SETLOCAL
@SET __MVNW_ARG0=%~f0
@SET __MVNW_ARG0_S=%0

@FOR /F "usebackq tokens=1* delims==" %%A IN ("%__MVNW_ARG0%\..\\.mvn\wrapper\maven-wrapper.properties") DO @(
    IF "%%A"=="distributionUrl" SET "MVNW_DISTRO_URL=%%B"
    IF "%%A"=="wrapperUrl" SET "MVNW_WRAPPER_URL=%%B"
)

@IF NOT EXIST "%__MVNW_ARG0%\..\\.mvn\wrapper\maven-wrapper.jar" (
    @IF NOT "%MVNW_WRAPPER_URL%"=="" (
        @ECHO Downloading Maven Wrapper...
        @powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MVNW_WRAPPER_URL%' -OutFile '%__MVNW_ARG0%\..\\.mvn\wrapper\maven-wrapper.jar'}" >NUL 2>&1
    )
)

@IF NOT EXIST "%__MVNW_ARG0%\..\\.mvn\wrapper\maven-wrapper.jar" (
    @ECHO Maven Wrapper JAR not found. Downloading Maven directly...
    @SET MVNW_SKIP_WRAPPER=true
)

@IF "%MVNW_SKIP_WRAPPER%"=="true" (
    @IF NOT EXIST "%USERPROFILE%\.m2\wrapper\dists" @MKDIR "%USERPROFILE%\.m2\wrapper\dists"
    @SET "MAVEN_HOME_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"
    @IF NOT EXIST "!MAVEN_HOME_DIR!" (
        @powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%MVNW_DISTRO_URL%' -OutFile '%TEMP%\maven.zip'; Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force; Remove-Item '%TEMP%\maven.zip'}"
    )
    @SET "MAVEN_CMD=!MAVEN_HOME_DIR!\bin\mvn.cmd"
) ELSE (
    @SET "MAVEN_CMD=java -jar "%__MVNW_ARG0%\..\\.mvn\wrapper\maven-wrapper.jar""
)

:mvnw_main
@SETLOCAL EnableDelayedExpansion

@SET JAVA_EXE=java
@IF NOT "%JAVA_HOME%"=="" @SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

@REM Find project base dir
@SET "MAVEN_PROJECTBASEDIR=%__MVNW_ARG0%\.."
@SET "MAVEN_PROJECTBASEDIR=%~dp0"

@IF EXIST "%__MVNW_ARG0%\..\\.mvn\wrapper\maven-wrapper.jar" (
    "%JAVA_EXE%" ^
        -classpath "%__MVNW_ARG0%\..\\.mvn\wrapper\maven-wrapper.jar" ^
        "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
        org.apache.maven.wrapper.MavenWrapperMain %*
) ELSE (
    @IF NOT EXIST "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd" (
        @ECHO Downloading Apache Maven 3.9.9...
        @powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%TEMP%\maven.zip'; Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force; Remove-Item '%TEMP%\maven.zip'}"
    )
    "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd" %*
)

@ENDLOCAL
