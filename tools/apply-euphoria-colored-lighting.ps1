<#
Applies the Complementary Reimagined + Euphoria Patches colored-lighting
profiles used by Simply Light to this addon's dimmable lamp variants, including
items held in either hand.

The shader pack, rather than Minecraft or NeoForge, assigns colored-lighting
profiles by block and item ID. This script is deliberately idempotent and
creates backups beside the edited shader property files before changing them.
#>
[CmdletBinding()]
param(
    # Exact shaderpack directory. Takes precedence over -InstancePath.
    [string]$ShaderPackPath,
    # CurseForge/Minecraft instance directory. This avoids accidentally editing
    # the development run/ copy when testing features such as Sable sublevels.
    [string]$InstancePath,
    [string]$ShaderPackName = "ComplementaryReimagined_r5.8.1 + EuphoriaPatches_1.9.3"
)

if ([string]::IsNullOrWhiteSpace($ShaderPackPath)) {
    $shaderRoot = if ([string]::IsNullOrWhiteSpace($InstancePath)) {
        Join-Path $PSScriptRoot "..\run\shaderpacks"
    }
    else {
        Join-Path $InstancePath "shaderpacks"
    }
    $ShaderPackPath = Join-Path $shaderRoot $ShaderPackName
}

$blockPropertiesPath = Join-Path $ShaderPackPath "shaders\block.properties"
$itemPropertiesPath = Join-Path $ShaderPackPath "shaders\item.properties"
$backupPath = "$blockPropertiesPath.simply-light-smart-backup"
$legacyBackupPath = "$blockPropertiesPath.simply-lights-toggle-backup"
$itemBackupPath = "$itemPropertiesPath.simply-light-smart-backup"

foreach ($propertiesPath in @($blockPropertiesPath, $itemPropertiesPath)) {
    if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) {
        throw "Shader pack property file not found: $propertiesPath"
    }
}

# Always start from the untouched copy so running the script again cannot add duplicates.
if (-not (Test-Path -LiteralPath $backupPath -PathType Leaf)) {
    if (Test-Path -LiteralPath $legacyBackupPath -PathType Leaf) {
        Copy-Item -LiteralPath $legacyBackupPath -Destination $backupPath
    }
    else {
        Copy-Item -LiteralPath $blockPropertiesPath -Destination $backupPath
    }
}

if (-not (Test-Path -LiteralPath $itemBackupPath -PathType Leaf)) {
    Copy-Item -LiteralPath $itemPropertiesPath -Destination $itemBackupPath
}

$blockContents = [System.IO.File]::ReadAllText($backupPath)
$itemContents = [System.IO.File]::ReadAllText($itemBackupPath)
$families = @(
    "dimmer_lightbulb",
    "dimmer_illuminant_slab",
    "dimmer_illuminant_panel",
    "dimmer_fixture",
    "dimmer_illuminant_block",
    "dimmer_illuminant_rod",
    "dimmer_illuminant_edge",
    "dimmer_illuminant_edge_top",
    "dimmer_illuminant_column"
)
$colors = @(
    "", "black", "blue", "brown", "cyan", "gray", "green", "light_blue",
    "light_gray", "lime", "magenta", "orange", "pink", "purple", "red", "yellow"
)

foreach ($color in $colors) {
    $colorSuffix = if ($color) { "_$color" } else { "" }
    $sourceId = "simplylight:wall_lamp$colorSuffix"
    $pattern = [regex]::Escape($sourceId) + "(?!_)"
    $matches = [regex]::Matches($blockContents, $pattern)

    if ($matches.Count -ne 1) {
        throw "Expected exactly one $sourceId profile in the shader pack, found $($matches.Count)."
    }

    # Iris applies a profile without a property predicate to every block state
    # of that block. Listing power=1 through power=15 creates 4,320 exact
    # entries, which makes Euphoria's material-map build stall on world join.
    # The block's vanilla light emission still controls the visible dimming;
    # this profile only supplies its color.
    $dimmerBlocks = foreach ($family in $families) {
        foreach ($invertedSuffix in @("", "_inverted")) {
            "simply_light_smart:${family}${colorSuffix}${invertedSuffix}"
        }
    }

    # Keep the generated property readable and avoid an exceptionally long physical
    # line; the trailing backslash continues the same block.<id> property.
    $formattedBlocks = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $dimmerBlocks.Count; $index++) {
        if ($index -gt 0 -and $index % 10 -eq 0) {
            $formattedBlocks.Add(' \' + [Environment]::NewLine + '        ')
        }
        $formattedBlocks.Add($dimmerBlocks[$index])
    }

    $blockContents = $blockContents -replace $pattern, ('$0 ' + ($formattedBlocks -join ' '))

    $itemMatches = [regex]::Matches($itemContents, $pattern)
    if ($itemMatches.Count -ne 1) {
        throw "Expected exactly one $sourceId item profile in the shader pack, found $($itemMatches.Count)."
    }

    $dimmerItems = foreach ($family in $families) {
        foreach ($invertedSuffix in @("", "_inverted")) {
            "simply_light_smart:${family}${colorSuffix}${invertedSuffix}"
        }
    }

    $formattedItems = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $dimmerItems.Count; $index++) {
        if ($index -gt 0 -and $index % 8 -eq 0) {
            $formattedItems.Add(' \' + [Environment]::NewLine + '        ')
        }
        $formattedItems.Add($dimmerItems[$index])
    }

    $itemContents = $itemContents -replace $pattern, ('$0 ' + ($formattedItems -join ' '))
}

[System.IO.File]::WriteAllText($blockPropertiesPath, $blockContents, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText($itemPropertiesPath, $itemContents, [System.Text.UTF8Encoding]::new($false))
Write-Host "Colored-lighting profiles applied to $blockPropertiesPath"
Write-Host "Colored-lighting profiles applied to $itemPropertiesPath"
Write-Host "Backup retained at $backupPath"
Write-Host "Backup retained at $itemBackupPath"
