# Royal Kennel API probe (round 4 - just the Camera class)
# Run from the mod5 folder:  powershell -ExecutionPolicy Bypass -File probe.ps1 > probe.txt
$ErrorActionPreference = "SilentlyContinue"

$javaBin = Split-Path (Get-Command java).Source
$javap   = Join-Path $javaBin "javap.exe"

$mc = Get-ChildItem -Recurse -Path @("$env:USERPROFILE\.gradle\caches\fabric-loom", "$PWD\.gradle\loom-cache") -Filter "*.jar" |
      Where-Object { $_.FullName -match "1\.21\.11" -and $_.Name -match "minecraft-merged" -and
                     $_.FullName -match "loom\.mappings" -and $_.Name -notmatch "sources|intermediary" } |
      Sort-Object Length -Descending | Select-Object -First 1
"### MC JAR: $($mc.FullName)"
""
"### javap -p net.minecraft.client.Camera"
& $javap -p -classpath $mc.FullName "net.minecraft.client.Camera" 2>&1
