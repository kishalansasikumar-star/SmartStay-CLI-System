param([string]$Jdk = $env:JAVA_HOME, [switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$previousEncoding = [Console]::OutputEncoding
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    New-Item -ItemType Directory -Force build | Out-Null
    $sources = @(Get-ChildItem src -Filter *.java -Recurse | ForEach-Object FullName)
    $compiler = if ($Jdk) { Join-Path $Jdk 'bin/javac.exe' } else { 'javac' }
    $runtime = if ($Jdk) { Join-Path $Jdk 'bin/java.exe' } else { 'java' }
    if ($Test) { $sources += 'tests/SystemTest_2703142.java' }
    & $compiler -encoding UTF-8 --release 11 -d build @sources
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($Test) { & $runtime -cp build SystemTest_2703142 }
    else { & $runtime -cp build smartstay.ui.Main_2703142 }
    exit $LASTEXITCODE
} finally {
    [Console]::OutputEncoding = $previousEncoding
    Pop-Location
}
