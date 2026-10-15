$ErrorActionPreference = 'Stop'

$projectDir = $PSScriptRoot
$serverDir = Join-Path $projectDir 'run\dedicated-server'
$modsDir = Join-Path $serverDir 'mods'
$neoForgeVersion = '21.1.234'
$installerName = "neoforge-$neoForgeVersion-installer.jar"
$installerPath = Join-Path $serverDir $installerName
$installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoForgeVersion/$installerName"

$javaHomes = @()
if ($env:JAVA_HOME) {
    $javaHomes += $env:JAVA_HOME
}
$toolchainHome = Join-Path $env:USERPROFILE '.gradle\jdks'
if (Test-Path -LiteralPath $toolchainHome) {
    $javaHomes += Get-ChildItem -LiteralPath $toolchainHome -Directory | Select-Object -ExpandProperty FullName
}
$adoptiumHome = 'C:\Program Files\Eclipse Adoptium'
if (Test-Path -LiteralPath $adoptiumHome) {
    $javaHomes += Get-ChildItem -LiteralPath $adoptiumHome -Directory -Filter 'jdk-21*' | Select-Object -ExpandProperty FullName
}
$java21Home = $javaHomes |
    Where-Object { Test-Path -LiteralPath (Join-Path $_ 'release') } |
    Where-Object { (Get-Content -LiteralPath (Join-Path $_ 'release') -Raw) -match 'JAVA_VERSION="21\.' } |
    Select-Object -First 1
if (-not $java21Home) {
    throw 'Java 21 was not found. Install a Java 21 JDK before running the dedicated server.'
}
$env:JAVA_HOME = $java21Home
$env:PATH = "$(Join-Path $java21Home 'bin');$env:PATH"

Push-Location $projectDir
try {
    Write-Host 'Building Simply Light Smart addon JAR...'
    & .\gradlew.bat --no-daemon jar
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle jar task failed with exit code $LASTEXITCODE."
    }

    $addonJar = Get-ChildItem (Join-Path $projectDir 'build\libs') -Filter 'simply_light_smart-*.jar' |
        Where-Object { $_.Name -notmatch 'sources|javadoc' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $addonJar) {
        throw 'Gradle completed, but no Simply Light Smart addon JAR was found in build\libs.'
    }

    New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
    Get-ChildItem -LiteralPath $modsDir -Filter 'simply_light_smart-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Remove-Item -Force
    Copy-Item -LiteralPath $addonJar.FullName -Destination (Join-Path $modsDir $addonJar.Name) -Force

    $baseModJar = Join-Path $projectDir 'libs\simplylight-1.5.3+1.21.1-b4.jar'
    if (-not (Test-Path -LiteralPath $baseModJar)) {
        throw "Required Simply Light dependency was not found: $baseModJar"
    }
    Copy-Item -LiteralPath $baseModJar -Destination (Join-Path $modsDir (Split-Path $baseModJar -Leaf)) -Force

    if (-not (Test-Path -LiteralPath (Join-Path $serverDir 'run.bat'))) {
        New-Item -ItemType Directory -Path $serverDir -Force | Out-Null
        if (-not (Test-Path -LiteralPath $installerPath)) {
            Write-Host "Downloading NeoForge $neoForgeVersion installer..."
            Invoke-WebRequest -Uri $installerUrl -OutFile $installerPath
        }

        Write-Host "Installing NeoForge $neoForgeVersion dedicated server..."
        Push-Location $serverDir
        try {
            & java -jar $installerPath --installServer
        }
        finally {
            Pop-Location
        }
        if ($LASTEXITCODE -ne 0) {
            throw "NeoForge server installation failed with exit code $LASTEXITCODE."
        }
    }

    $eulaPath = Join-Path $serverDir 'eula.txt'
    if (-not (Test-Path -LiteralPath $eulaPath)) {
        Set-Content -LiteralPath $eulaPath -Value 'eula=false' -Encoding ascii
        Write-Host 'Server files and both mod JARs are ready.'
        Write-Host "Review and accept the Minecraft EULA in $eulaPath, then run this task again."
        exit 0
    }
    if (-not (Select-String -LiteralPath $eulaPath -Pattern '^\s*eula\s*=\s*true\s*$' -Quiet)) {
        Write-Host "Accept the Minecraft EULA in $eulaPath, then run this task again."
        exit 0
    }

    Write-Host "Starting NeoForge dedicated server with $($addonJar.Name)..."
    Push-Location $serverDir
    & .\run.bat nogui
    $serverExitCode = $LASTEXITCODE
    Pop-Location
    exit $serverExitCode
}
finally {
    Pop-Location
}
