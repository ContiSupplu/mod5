# Royal Kennel API probe (round 5 - mouse input, for user camera control)
# Run from the mod5 folder:  powershell -ExecutionPolicy Bypass -File probe.ps1 > probe.txt
$ErrorActionPreference = "SilentlyContinue"

$javaBin = Split-Path (Get-Command java).Source
$javap   = Join-Path $javaBin "javap.exe"

$mc = Get-ChildItem -Recurse -Path @("$env:USERPROFILE\.gradle\caches\fabric-loom", "$PWD\.gradle\loom-cache") -Filter "*.jar" |
      Where-Object { $_.FullName -match "1\.21\.11" -and $_.Name -match "minecraft-merged" -and
                     $_.FullName -match "loom\.mappings" -and $_.Name -notmatch "sources|intermediary" } |
      Sort-Object Length -Descending | Select-Object -First 1
"### MC JAR: $($mc.FullName)"

function DumpFull($cls) {
    ""
    "### javap $cls"
    & $javap -classpath $mc.FullName $cls 2>&1
}

DumpFull "net.minecraft.client.gui.components.events.GuiEventListener"
DumpFull "net.minecraft.client.input.MouseButtonEvent"
""
"### javap net.minecraft.client.gui.screens.Screen (filtered: mouse|Mouse|scroll|Scroll|drag|Drag)"
& $javap -classpath $mc.FullName "net.minecraft.client.gui.screens.Screen" 2>&1 |
    Select-String -Pattern "mouse|Mouse|scroll|Scroll|drag|Drag" | ForEach-Object { $_.Line }
