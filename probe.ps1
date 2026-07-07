# Royal Kennel API probe
# Dumps the exact 1.21.11 API signatures from the remapped Minecraft jar in
# the Gradle cache, so the mod source can be fixed against ground truth.
#
# Run from the mod5 folder:  powershell -ExecutionPolicy Bypass -File probe.ps1 > probe.txt
$ErrorActionPreference = "SilentlyContinue"

$javaBin = Split-Path (Get-Command java).Source
$jarTool = Join-Path $javaBin "jar.exe"
$javap   = Join-Path $javaBin "javap.exe"

$roots = @("$env:USERPROFILE\.gradle\caches\fabric-loom", "$PWD\.gradle\loom-cache")

$mc = Get-ChildItem -Recurse -Path $roots -Filter "*.jar" |
      Where-Object { $_.Name -match "minecraft" -and $_.Name -notmatch "sources|intermediary|raw|original" } |
      Sort-Object Length -Descending | Select-Object -First 1
"### MC JAR: $($mc.FullName) [$([math]::Round($mc.Length/1MB,1)) MB]"

$list = & $jarTool tf $mc.FullName

"### CLASS LOCATIONS"
$list | Select-String -Pattern "(Identifier|ResourceLocation|RenderTypes?|DataComponents|DataComponentGetter|MouseButtonEvent)\.class$" |
    ForEach-Object { $_.Line }

function DumpFull($cls) {
    ""
    "### javap $cls"
    & $javap -classpath $mc.FullName $cls
}
function DumpGrep($cls, $pat) {
    ""
    "### javap $cls (filtered: $pat)"
    & $javap -classpath $mc.FullName $cls | Select-String -Pattern $pat | ForEach-Object { $_.Line }
}

$idClass = ($list | Select-String -Pattern "Identifier\.class$" | Select-Object -First 1).Line -replace "/", "." -replace "\.class$", ""
if ($idClass) { DumpFull $idClass }

DumpFull "net.minecraft.resources.ResourceKey"
DumpFull "net.minecraft.core.component.DataComponentGetter"
DumpGrep "net.minecraft.world.entity.animal.wolf.Wolf" "ariant|ollar|omponent"
DumpGrep "net.minecraft.world.entity.Entity" "omponent"
DumpGrep "net.minecraft.world.entity.LivingEntity" "omponent"
DumpGrep "net.minecraft.world.entity.Mob" "omponent"
DumpFull "net.minecraft.client.gui.components.AbstractButton"
DumpGrep "net.minecraft.client.gui.components.AbstractWidget" "render|onClick|onPress|arration|ound|ressed"
DumpGrep "net.minecraft.core.component.DataComponents" "WOLF"
DumpGrep "net.minecraft.core.Holder" "is|unwrapKey|getRegisteredName"
DumpGrep "net.minecraft.client.renderer.MultiBufferSource" "getBuffer"
DumpGrep "net.minecraft.world.level.Level" "lient"

$rtClass = ($list | Select-String -Pattern "RenderTypes\.class$" | Select-Object -First 1).Line -replace "/", "." -replace "\.class$", ""
if ($rtClass) { DumpGrep $rtClass "ntity" }

""
"### FABRIC RENDERING JARS"
$fjars = Get-ChildItem -Recurse -Path $roots -Filter "*.jar" | Where-Object { $_.Name -match "rendering|fabric-api" }
foreach ($fj in $fjars) {
    "## $($fj.Name)"
    & $jarTool tf $fj.FullName | Select-String -Pattern "WorldRender" | ForEach-Object { $_.Line } | Select-Object -First 40
}
