$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = 'C:\Program Files\ojdkbuild\java-17-openjdk-17.0.3.0.6-1'
$mavenBin = Join-Path $repoRoot 'tools\apache-maven-3.9.14\bin'
$backendDir = Join-Path $repoRoot 'backend-java'

if (-not (Test-Path $javaHome)) {
    throw "Java 17 was not found at '$javaHome'. Update run-backend.ps1 to match your installed JDK path."
}

if (-not (Test-Path (Join-Path $mavenBin 'mvn.cmd'))) {
    throw "Local Maven was not found at '$mavenBin'."
}

$env:JAVA_HOME = $javaHome
$env:Path = "$mavenBin;$javaHome\bin;$env:Path"

Set-Location $backendDir
mvn.cmd "-Dmaven.repo.local=../tools/m2repo" spring-boot:run
