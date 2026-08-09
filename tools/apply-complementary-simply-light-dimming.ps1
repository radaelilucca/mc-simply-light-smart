<#
Applies Simply Light - Smart!'s dynamic-emission material mapping to a stock
Complementary Reimagined or Complementary Unbound shader pack directory or ZIP.

The addon adds smart_power=0..15 to the original Simply Light blocks. The
shader pack normally assigns an emissive material only by block ID, so a lamp
can continue to glow after the addon has lowered its vanilla light emission.
This patch assigns one material ID per smart_power value and scales the shader
emission by that same value.

The script is idempotent: it saves an untouched backup next to the target and
always regenerates the patch from that copy. Euphoria Patches is deliberately
excluded: its generated block.properties owns Simply Light's colored-light
material IDs, and replacing those IDs breaks placed-light coloration.
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [ValidateNotNullOrEmpty()]
    [string[]]$ShaderPackPath,

    [switch]$Restore
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$materialIdStart = 12000
$markerStart = '# Simply Light - Smart! dynamic emission start'
$markerEnd = '# Simply Light - Smart! dynamic emission end'
$blockPropertyAnchor = 'block.10028='
$ipbrTerrainAnchor = "    #if IPBR_EMISSIVE_MODE != 1 && !defined VOXY_PATCH`n        emission = GetCustomEmissionForIPBR(color, emission);`n    #endif"
$terrainAnchor = "    } else if (mat == 10028) { // Modded Light Sources`n        noSmoothLighting = true; noDirectionalShading = true;`n        emission = GetLuminance(color.rgb) * 2.5;`n    }"
$euphoriaAutomaticEmissionAnchor = '    } else if (mat > 20999 && mat < 21025){'
$euphoriaSmartLightMaterials = @(
    @{ Color = ''; MaterialId = 21100 }, @{ Color = 'brown'; MaterialId = 21102 },
    @{ Color = 'red'; MaterialId = 21104 }, @{ Color = 'orange'; MaterialId = 21106 },
    @{ Color = 'yellow'; MaterialId = 21108 }, @{ Color = 'lime'; MaterialId = 21110 },
    @{ Color = 'green'; MaterialId = 21112 }, @{ Color = 'cyan'; MaterialId = 21114 },
    @{ Color = 'light_blue'; MaterialId = 21116 }, @{ Color = 'blue'; MaterialId = 21118 },
    @{ Color = 'purple'; MaterialId = 21120 }, @{ Color = 'magenta'; MaterialId = 21122 },
    @{ Color = 'pink'; MaterialId = 21124 }, @{ Color = 'gray'; MaterialId = 21100 },
    @{ Color = 'light_gray'; MaterialId = 21100 }, @{ Color = 'black'; MaterialId = 21100 }
)

function Get-SimplyLightBlockIds {
    $colors = @(
        '', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
        'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black'
    )
    $families = @(
        'lightbulb',
        'illuminant_slab',
        'illuminant_panel',
        'wall_lamp',
        'rodlamp',
        'edge_light',
        'edge_light_top',
        'lamp_post'
    )

    $blocks = [System.Collections.Generic.List[string]]::new()
    foreach ($family in $families) {
        foreach ($color in $colors) {
            $suffix = if ($color) { "_$color" } else { '' }
            $blocks.Add("simplylight:$family$suffix")
        }
    }

    foreach ($color in $colors) {
        $base = if ($color) { "illuminant_${color}_block" } else { 'illuminant_block' }
        $blocks.Add("simplylight:$base")
        $blocks.Add("simplylight:${base}_on")
    }

    return $blocks
}

function New-BlockPropertiesSection {
    $blocks = Get-SimplyLightBlockIds
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add($markerStart)
    $lines.Add('# Maps the original Simply Light blocks by smart_power so their shader emission')
    $lines.Add('# follows the vanilla light level applied by Simply Light - Smart!.')

    for ($power = 0; $power -le 15; $power++) {
        $materialId = $materialIdStart + $power
        $states = $blocks | ForEach-Object { "${_}:smart_power=$power" }
        $lines.Add("block.$materialId=$($states -join ' ')")
    }

    $lines.Add($markerEnd)
    return $lines -join "`n"
}

function Get-SimplyLightBlockIdsForColor {
    param([string]$Color)

    $families = @('lightbulb', 'illuminant_slab', 'illuminant_panel', 'wall_lamp', 'rodlamp', 'edge_light', 'edge_light_top', 'lamp_post')
    $suffix = if ($Color) { "_$Color" } else { '' }
    $blocks = [System.Collections.Generic.List[string]]::new()
    foreach ($family in $families) {
        $blocks.Add("simplylight:$family$suffix")
    }
    $illuminant = if ($Color) { "illuminant_${Color}_block" } else { 'illuminant_block' }
    $blocks.Add("simplylight:$illuminant")
    $blocks.Add("simplylight:${illuminant}_on")
    return $blocks
}

function New-EuphoriaBlockPropertiesSection {
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add($markerStart)
    $lines.Add("# Keeps Euphoria's colored-light material categories while dimming only the visible texture.")
    foreach ($entry in $euphoriaSmartLightMaterials | Where-Object { $_.Color -ne '' -and $_.Color -ne 'gray' -and $_.Color -ne 'light_gray' -and $_.Color -ne 'black' }) {
        $blocks = Get-SimplyLightBlockIdsForColor -Color $entry.Color
        $lines.Add("block.$($entry.MaterialId)=$($blocks -join ' ')")
    }
    $whiteBlocks = @('', 'gray', 'light_gray', 'black') | ForEach-Object { Get-SimplyLightBlockIdsForColor -Color $_ }
    $lines.Add("block.21100=$($whiteBlocks -join ' ')")
    $lines.Add($markerEnd)
    return $lines -join "`n"
}

function New-EuphoriaPhotonicsLights {
    param([string]$PhotonicsLights, [string]$SourceDescription)

    $patched = $PhotonicsLights -replace "`r`n", "`n"
    foreach ($entry in $euphoriaSmartLightMaterials | Where-Object { $_.Color -ne 'gray' -and $_.Color -ne 'light_gray' -and $_.Color -ne 'black' }) {
        $sourceMaterialId = $entry.MaterialId - 100
        $anchor = '"blocks": ["*' + $sourceMaterialId + '"]'
        $replacement = '"blocks": ["*' + $sourceMaterialId + '", "*' + $entry.MaterialId + '"]'
        $patched = Replace-UniqueAnchor -Content $patched -Anchor $anchor -Replacement $replacement -FileDescription $SourceDescription
    }
    return $patched
}

function New-TerrainMaterialSection {
    $materialIdEnd = $materialIdStart + 15
    return @"
    else if (mat >= $materialIdStart && mat <= $materialIdEnd) { // Simply Light - Smart! dynamic emission
        noSmoothLighting = true; noDirectionalShading = true;
        float simplyLightSmartPower = float(mat - $materialIdStart) / 15.0;
        emission = GetLuminance(color.rgb) * 2.5 * simplyLightSmartPower;
        color.rgb *= simplyLightSmartPower;
    }
"@.TrimEnd("`r", "`n")
}

function New-IpbrEmissionSection {
    $materialIdEnd = $materialIdStart + 15
    return @"
    if (mat >= $materialIdStart && mat <= $materialIdEnd) { // Simply Light - Smart! dynamic emission
        float simplyLightSmartPower = float(mat - $materialIdStart) / 15.0;
        color.rgb *= simplyLightSmartPower;
        emission *= simplyLightSmartPower;
    }
"@.TrimEnd("`r", "`n")
}

function New-EuphoriaTerrainMaterialSection {
    return @"
    } else if (mat >= 21100 && mat <= 21124) { // Simply Light - Smart! texture dimming
        emission = DoAutomaticEmission(noSmoothLighting, noDirectionalShading, color.rgb, lmCoord.x, blockLightEmission, 1.0);
        float simplyLightSmartTexturePower = max(float(blockLightEmission) / 15.0, 2.0 / 15.0);
        color.rgb *= simplyLightSmartTexturePower;
    }
"@.TrimEnd("`r", "`n")
}

function Get-EuphoriaIpbrEmissionAnchor {
    return @"
    } else if (mat > 20999 && mat < 21025) {
    #ifdef GBUFFERS_TERRAIN
        emission = DoAutomaticEmission(noSmoothLighting, noDirectionalShading, color.rgb, lmCoord.x, blockLightEmission, 1.0);
    #else
        bool doesNothing;
        emission = DoAutomaticEmission(noSmoothLighting, doesNothing, color.rgb, 0.0, 15, 1.0);
    #endif
}
"@.TrimEnd("`r", "`n")
}

function New-EuphoriaIpbrSection {
    return @"
 else if (mat >= 21100 && mat <= 21124) { // Simply Light - Smart! texture dimming
    #ifdef GBUFFERS_TERRAIN
        emission = DoAutomaticEmission(noSmoothLighting, noDirectionalShading, color.rgb, lmCoord.x, blockLightEmission, 1.0);
        float simplyLightSmartTexturePower = max(float(blockLightEmission) / 15.0, 2.0 / 15.0);
        color.rgb *= simplyLightSmartTexturePower;
    #else
        bool doesNothing;
        emission = DoAutomaticEmission(noSmoothLighting, doesNothing, color.rgb, 0.0, 15, 1.0);
    #endif
}
"@.TrimEnd("`r", "`n")
}

function Get-PristineContent {
    param([string]$Path)

    $backupPath = "$Path.simply-light-smart-backup"
    if (-not (Test-Path -LiteralPath $backupPath -PathType Leaf)) {
        if ($WhatIfPreference) {
            return [System.IO.File]::ReadAllText($Path) -replace "`r`n", "`n"
        }
        Copy-Item -LiteralPath $Path -Destination $backupPath
    }
    return [System.IO.File]::ReadAllText($backupPath) -replace "`r`n", "`n"
}

function Add-AfterUniqueAnchor {
    param(
        [string]$Content,
        [string]$Anchor,
        [string]$Addition,
        [string]$FileDescription
    )

    $matches = [regex]::Matches($Content, [regex]::Escape($Anchor))
    if ($matches.Count -ne 1) {
        throw "Expected exactly one '$Anchor' anchor in $FileDescription, found $($matches.Count)."
    }

    $position = $matches[0].Index + $Anchor.Length
    return $Content.Insert($position, "`n" + $Addition)
}

function Replace-UniqueAnchor {
    param(
        [string]$Content,
        [string]$Anchor,
        [string]$Replacement,
        [string]$FileDescription
    )

    $matches = [regex]::Matches($Content, [regex]::Escape($Anchor))
    if ($matches.Count -ne 1) {
        throw "Expected exactly one '$Anchor' anchor in $FileDescription, found $($matches.Count)."
    }

    return $Content.Remove($matches[0].Index, $Anchor.Length).Insert($matches[0].Index, $Replacement)
}

function Remove-SimplyLightProfiles {
    param([string]$BlockProperties)

    # Euphoria's existing profiles are static and reserve a minimum emission.
    # Remove only Simply Light tokens, retaining every other mod's profile.
    return $BlockProperties -replace '(?<!\S)simplylight:[^\s\\]+', ''
}

function Upgrade-ExistingShaderPatch {
    param(
        [string]$BlockProperties,
        [string]$TerrainMaterials,
        [string]$SourceDescription
    )

    if (-not $BlockProperties.Contains($markerStart) -or -not $BlockProperties.Contains($markerEnd) -or -not $TerrainMaterials.Contains('Simply Light - Smart! dynamic emission')) {
        throw "Existing Simply Light - Smart! patch in $SourceDescription is incomplete and cannot be upgraded safely."
    }

    $oldTerrainRule = "float simplyLightSmartPower = float(mat - $materialIdStart) / 15.0;`n        emission = GetLuminance(color.rgb) * 2.5 * simplyLightSmartPower;"
    $newTerrainRule = "$oldTerrainRule`n        color.rgb *= simplyLightSmartPower;"
    $oldIpbrRule = "emission *= float(mat - $materialIdStart) / 15.0;"
    $newIpbrRule = "float simplyLightSmartPower = float(mat - $materialIdStart) / 15.0;`n        color.rgb *= simplyLightSmartPower;`n        emission *= simplyLightSmartPower;"
    $upgradedTerrainMaterials = $TerrainMaterials.Replace($oldTerrainRule, $newTerrainRule).Replace($oldIpbrRule, $newIpbrRule)

    return [PSCustomObject]@{
        BlockProperties = $BlockProperties
        TerrainMaterials = $upgradedTerrainMaterials
        TerrainIpbr = $null
        PhotonicsLights = $null
    }
}

function New-PatchedShaderContents {
    param(
        [string]$BlockProperties,
        [string]$TerrainMaterials,
        [string]$TerrainIpbr,
        [string]$PhotonicsLights,
        [string]$SourceDescription
    )

    $BlockProperties = $BlockProperties -replace "`r`n", "`n"
    $TerrainMaterials = $TerrainMaterials -replace "`r`n", "`n"
    $blockSection = New-BlockPropertiesSection
    $terrainSection = New-TerrainMaterialSection
    $ipbrEmissionSection = New-IpbrEmissionSection
    $materialIdEnd = $materialIdStart + 15

    if ($BlockProperties.Contains($markerStart) -or $BlockProperties.Contains($markerEnd) -or $TerrainMaterials.Contains('Simply Light - Smart! dynamic emission')) {
        return Upgrade-ExistingShaderPatch -BlockProperties $BlockProperties -TerrainMaterials $TerrainMaterials -SourceDescription $SourceDescription
    }
    $reservedMaterialIds = ($materialIdStart..$materialIdEnd) -join '|'
    if ([regex]::IsMatch($BlockProperties, "(?m)^block\.(?:$reservedMaterialIds)=")) {
        throw "Material IDs $materialIdStart through $materialIdEnd are already used in $SourceDescription. Choose a new range before patching this shader version."
    }

    if ($TerrainMaterials.Contains($euphoriaAutomaticEmissionAnchor)) {
        throw "Euphoria Patches is not supported by this dimming patcher. Its block.properties must retain Euphoria's Simply Light colored-light IDs."
    }
    $patchedBlockProperties = Add-AfterUniqueAnchor -Content $BlockProperties -Anchor $blockPropertyAnchor -Addition $blockSection -FileDescription $SourceDescription
    $patchedTerrainMaterials = Add-AfterUniqueAnchor -Content $TerrainMaterials -Anchor $terrainAnchor -Addition $terrainSection -FileDescription $SourceDescription
    $patchedTerrainIpbr = $null
    $patchedPhotonicsLights = $null

    return [PSCustomObject]@{
        BlockProperties = $patchedBlockProperties
        TerrainMaterials = Add-AfterUniqueAnchor -Content $patchedTerrainMaterials -Anchor $ipbrTerrainAnchor -Addition $ipbrEmissionSection -FileDescription $SourceDescription
        TerrainIpbr = $patchedTerrainIpbr
        PhotonicsLights = $patchedPhotonicsLights
    }
}

function Read-ZipTextEntry {
    param(
        [string]$ZipPath,
        [string]$EntryName
    )

    $zip = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
    try {
        $entry = $zip.GetEntry($EntryName)
        if ($null -eq $entry) {
            throw "Complementary shader file not found in ${ZipPath}: $EntryName"
        }
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try {
            return $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }
    }
    finally {
        $zip.Dispose()
    }
}

function Write-ZipTextEntries {
    param(
        [string]$ZipPath,
        [PSCustomObject]$Contents
    )

    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $entries = @{
            'shaders/block.properties' = $Contents.BlockProperties
            'shaders/lib/materials/materialHandling/terrainMaterials.glsl' = $Contents.TerrainMaterials
        }
        if ($null -ne $Contents.TerrainIpbr) {
            $entries['shaders/lib/materials/materialHandling/terrainIPBR.glsl'] = $Contents.TerrainIpbr
        }
        if ($null -ne $Contents.PhotonicsLights) {
            $entries['shaders/ph_lights.json'] = $Contents.PhotonicsLights
        }
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        foreach ($entryName in $entries.Keys) {
            $existingEntry = $zip.GetEntry($entryName)
            if ($null -ne $existingEntry) {
                $existingEntry.Delete()
            }
            $entry = $zip.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Optimal)
            $writer = [System.IO.StreamWriter]::new($entry.Open(), $utf8NoBom)
            try {
                $writer.Write($entries[$entryName])
            }
            finally {
                $writer.Dispose()
            }
        }
    }
    finally {
        $zip.Dispose()
    }
}

function Restore-SimplyLightSmartPatch {
    param([string]$PackPath)

    $resolvedPackPath = (Resolve-Path -LiteralPath $PackPath -ErrorAction Stop).Path
    if (Test-Path -LiteralPath $resolvedPackPath -PathType Leaf) {
        if ([System.IO.Path]::GetExtension($resolvedPackPath) -ne '.zip') {
            throw "Shader pack file must be a ZIP: $resolvedPackPath"
        }
        $backupPath = "$resolvedPackPath.simply-light-smart-backup"
        if (-not (Test-Path -LiteralPath $backupPath -PathType Leaf)) {
            throw "No Simply Light - Smart! backup exists for $resolvedPackPath. Restore requires the adjacent backup file."
        }
        if ($PSCmdlet.ShouldProcess($resolvedPackPath, 'restore pristine shader pack from Simply Light - Smart! backup')) {
            Copy-Item -LiteralPath $backupPath -Destination $resolvedPackPath -Force
            Write-Host "Restored $resolvedPackPath from $backupPath"
        }
        return
    }
    if (-not (Test-Path -LiteralPath $resolvedPackPath -PathType Container)) {
        throw "Shader pack must be a directory or ZIP: $resolvedPackPath"
    }

    $relativePaths = @(
        'shaders\block.properties',
        'shaders\lib\materials\materialHandling\terrainMaterials.glsl',
        'shaders\lib\materials\materialHandling\terrainIPBR.glsl',
        'shaders\ph_lights.json'
    )
    $restorablePaths = @($relativePaths | ForEach-Object {
        $path = Join-Path $resolvedPackPath $_
        $backupPath = "$path.simply-light-smart-backup"
        if (Test-Path -LiteralPath $backupPath -PathType Leaf) {
            [PSCustomObject]@{ Path = $path; BackupPath = $backupPath }
        }
    })
    if ($restorablePaths.Count -eq 0) {
        throw "No Simply Light - Smart! backups exist in $resolvedPackPath. Nothing can be restored safely."
    }
    foreach ($entry in $restorablePaths) {
        if ($PSCmdlet.ShouldProcess($entry.Path, 'restore pristine shader file from Simply Light - Smart! backup')) {
            Copy-Item -LiteralPath $entry.BackupPath -Destination $entry.Path -Force
        }
    }
    Write-Host "Restored $($restorablePaths.Count) shader file(s) in $resolvedPackPath from their Simply Light - Smart! backups."
}

function Apply-SimplyLightSmartPatch {
    param([string]$PackPath)

    $resolvedPackPath = (Resolve-Path -LiteralPath $PackPath -ErrorAction Stop).Path
    if (Test-Path -LiteralPath $resolvedPackPath -PathType Leaf) {
        if ([System.IO.Path]::GetExtension($resolvedPackPath) -ne '.zip') {
            throw "Shader pack file must be a ZIP: $resolvedPackPath"
        }
        $backupPath = "$resolvedPackPath.simply-light-smart-backup"
        $sourceZipPath = $backupPath
        if (-not (Test-Path -LiteralPath $backupPath -PathType Leaf)) {
            if ($WhatIfPreference) {
                $sourceZipPath = $resolvedPackPath
            }
            else {
            Copy-Item -LiteralPath $resolvedPackPath -Destination $backupPath
            }
        }
        $blockProperties = Read-ZipTextEntry -ZipPath $sourceZipPath -EntryName 'shaders/block.properties'
        $terrainMaterials = Read-ZipTextEntry -ZipPath $sourceZipPath -EntryName 'shaders/lib/materials/materialHandling/terrainMaterials.glsl'
        $terrainIpbr = Read-ZipTextEntry -ZipPath $sourceZipPath -EntryName 'shaders/lib/materials/materialHandling/terrainIPBR.glsl'
        if ($terrainMaterials.Contains($euphoriaAutomaticEmissionAnchor)) {
            throw "Euphoria Patches is not supported by this dimming patcher. Its block.properties must retain Euphoria's Simply Light colored-light IDs. Use -Restore to undo an earlier patch."
        }
        $photonicsLights = $null
        $contents = New-PatchedShaderContents `
            -BlockProperties $blockProperties `
            -TerrainMaterials $terrainMaterials `
            -TerrainIpbr $terrainIpbr `
            -PhotonicsLights $photonicsLights `
            -SourceDescription $sourceZipPath
        if ($PSCmdlet.ShouldProcess($resolvedPackPath, 'apply Simply Light - Smart! dynamic-emission patch')) {
            Write-ZipTextEntries -ZipPath $resolvedPackPath -Contents $contents
            Write-Host "Patched $resolvedPackPath"
            Write-Host "Backup retained at $backupPath"
        }
        return
    }
    if (-not (Test-Path -LiteralPath $resolvedPackPath -PathType Container)) {
        throw "Shader pack must be a directory or ZIP: $resolvedPackPath"
    }

    $blockPropertiesPath = Join-Path $resolvedPackPath 'shaders\block.properties'
    $terrainMaterialsPath = Join-Path $resolvedPackPath 'shaders\lib\materials\materialHandling\terrainMaterials.glsl'
    $terrainIpbrPath = Join-Path $resolvedPackPath 'shaders\lib\materials\materialHandling\terrainIPBR.glsl'
    foreach ($path in @($blockPropertiesPath, $terrainMaterialsPath, $terrainIpbrPath)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Complementary shader file not found: $path"
        }
    }

    $sourceTerrainMaterials = [System.IO.File]::ReadAllText($terrainMaterialsPath) -replace "`r`n", "`n"
    if ($sourceTerrainMaterials.Contains($euphoriaAutomaticEmissionAnchor)) {
        throw "Euphoria Patches is not supported by this dimming patcher. Its block.properties must retain Euphoria's Simply Light colored-light IDs. Use -Restore to undo an earlier patch."
    }
    $blockProperties = Get-PristineContent -Path $blockPropertiesPath
    $terrainMaterials = Get-PristineContent -Path $terrainMaterialsPath
    $terrainIpbr = Get-PristineContent -Path $terrainIpbrPath
    $photonicsLights = $null
    $contents = New-PatchedShaderContents -BlockProperties $blockProperties -TerrainMaterials $terrainMaterials -TerrainIpbr $terrainIpbr -PhotonicsLights $photonicsLights -SourceDescription $resolvedPackPath

    if ($PSCmdlet.ShouldProcess($resolvedPackPath, 'apply Simply Light - Smart! dynamic-emission patch')) {
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($blockPropertiesPath, $contents.BlockProperties, $utf8NoBom)
        [System.IO.File]::WriteAllText($terrainMaterialsPath, $contents.TerrainMaterials, $utf8NoBom)
        if ($null -ne $contents.TerrainIpbr) {
            [System.IO.File]::WriteAllText($terrainIpbrPath, $contents.TerrainIpbr, $utf8NoBom)
        }
        if ($null -ne $contents.PhotonicsLights) {
            [System.IO.File]::WriteAllText($photonicsLightsPath, $contents.PhotonicsLights, $utf8NoBom)
        }
        Write-Host "Patched $resolvedPackPath"
        Write-Host "Backups retained beside the patched files with the .simply-light-smart-backup suffix."
    }
}

if ($null -eq $ShaderPackPath -or $ShaderPackPath.Count -eq 0) {
    $shaderpacksPath = Join-Path $PSScriptRoot 'shaderpacks'
    if (-not (Test-Path -LiteralPath $shaderpacksPath -PathType Container)) {
        throw "Shaderpacks folder not found next to this script: $shaderpacksPath"
    }

    $packEntries = Get-ChildItem -LiteralPath $shaderpacksPath | Where-Object {
        $_.PSIsContainer -or $_.Extension -eq '.zip'
    }
    $reimagined = @($packEntries | Where-Object { $_.Name -like 'ComplementaryReimagined_*' })
    $unbound = @($packEntries | Where-Object { $_.Name -like 'ComplementaryUnbound_*' })
    if ($reimagined.Count -ne 1 -or $unbound.Count -ne 1) {
        throw "Expected one ComplementaryReimagined_* and one ComplementaryUnbound_* shader pack in $shaderpacksPath."
    }
    $ShaderPackPath = @($reimagined[0].FullName, $unbound[0].FullName)
}

foreach ($packPath in $ShaderPackPath) {
    if ($Restore) {
        Restore-SimplyLightSmartPatch -PackPath $packPath
    }
    else {
        Apply-SimplyLightSmartPatch -PackPath $packPath
    }
}
