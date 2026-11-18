param([string]$Jdk = $env:JAVA_HOME, [switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force build | Out-Null
    $sources = @(Get-ChildItem src -Filter *.java -Recurse | ForEach-Object FullName)
    $compiler = if ($Jdk) { Join-Path $Jdk 'bin/javac.exe' } else { 'javac' }
    $runtime = if ($Jdk) { Join-Path $Jdk 'bin/java.exe' } else { 'java' }
    if ($Test) { $sources += 'tests/E245549_SystemTest.java' }
    & $compiler -encoding UTF-8 --release 11 -d build @sources
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($Test) { & $runtime -cp build E245549_SystemTest }
    else { & $runtime -cp build smartstay.ui.E245549_Main }
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
