<#
Applies the Complementary Reimagined + Euphoria Patches colored-lighting
profiles used by Simply Light to this addon's dimmable lamp variants.

The shader pack, rather than Minecraft or NeoForge, assigns colored-lighting
profiles by block ID. This script is deliberately idempotent and creates one
backup beside the edited block.properties file before changing it.
#>
[CmdletBinding()]
param(
    [string]$ShaderPackPath = (Join-Path $PSScriptRoot "..\run\shaderpacks\ComplementaryReimagined_r5.8.1 + EuphoriaPatches_1.9.3")
)

$blockPropertiesPath = Join-Path $ShaderPackPath "shaders\block.properties"
$backupPath = "$blockPropertiesPath.simply-light-smart-backup"
$legacyBackupPath = "$blockPropertiesPath.simply-lights-toggle-backup"

if (-not (Test-Path -LiteralPath $blockPropertiesPath -PathType Leaf)) {
    throw "Shader pack not found: $blockPropertiesPath"
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

$contents = [System.IO.File]::ReadAllText($backupPath)
$families = @(
    "dimmer_lightbulb",
    "dimmer_illuminant_slab",
    "dimmer_illuminant_panel",
    "dimmer_fixture"
)
$colors = @(
    "", "black", "blue", "brown", "cyan", "gray", "green", "light_blue",
    "light_gray", "lime", "magenta", "orange", "pink", "purple", "red", "yellow"
)

foreach ($color in $colors) {
    $colorSuffix = if ($color) { "_$color" } else { "" }
    $sourceId = "simplylight:wall_lamp$colorSuffix"
    $pattern = [regex]::Escape($sourceId) + "(?!_)"
    $matches = [regex]::Matches($contents, $pattern)

    if ($matches.Count -ne 1) {
        throw "Expected exactly one $sourceId profile in the shader pack, found $($matches.Count)."
    }

    $dimmerStates = foreach ($family in $families) {
        foreach ($invertedSuffix in @("", "_inverted")) {
            foreach ($power in 1..15) {
                "simply_light_smart:${family}${colorSuffix}${invertedSuffix}:power=$power"
            }
        }
    }

    # Keep the generated property readable and avoid an exceptionally long physical
    # line; the trailing backslash continues the same block.<id> property.
    $formattedStates = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $dimmerStates.Count; $index++) {
        if ($index -gt 0 -and $index % 10 -eq 0) {
            $formattedStates.Add(' \' + [Environment]::NewLine + '        ')
        }
        $formattedStates.Add($dimmerStates[$index])
    }

    # Do not map power=0: an unpowered normal dimmer (or a fully powered inverted
    # dimmer) must remain completely dark, just like the vanilla light-level logic.
    $contents = $contents -replace $pattern, ('$0 ' + ($formattedStates -join ' '))
}

[System.IO.File]::WriteAllText($blockPropertiesPath, $contents, [System.Text.UTF8Encoding]::new($false))
Write-Host "Colored-lighting profiles applied to $blockPropertiesPath"
Write-Host "Backup retained at $backupPath"
