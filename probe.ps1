# Royal Kennel API probe (round 2)
# Dumps the exact 1.21.11 API signatures from the remapped Minecraft jar in
# the Gradle cache, so the mod source can be fixed against ground truth.
#
# Run from the mod5 folder:  powershell -ExecutionPolicy Bypass -File probe.ps1 > probe.txt
$ErrorActionPreference = "SilentlyContinue"

$javaBin = Split-Path (Get-Command java).Source
$jarTool = Join-Path $javaBin "jar.exe"
$javap   = Join-Path $javaBin "javap.exe"

$roots = @("$env:USERPROFILE\.gradle\caches\fabric-loom", "$PWD\.gradle\loom-cache")

# Only consider jars belonging to 1.21.11 (the old 1.21.4 leftovers fooled round 1).
$cand = Get-ChildItem -Recurse -Path $roots -Filter "*.jar" |
        Where-Object { $_.FullName -match "1\.21\.11" -and $_.Name -notmatch "sources" }

"### CANDIDATE 1.21.11 JARS (largest 15)"
$cand | Sort-Object Length -Descending | Select-Object -First 15 |
    ForEach-Object { "$([math]::Round($_.Length/1MB,1)) MB  $($_.FullName)" }

# The named (Mojang-mapped) jar lives under minecraftMaven with "loom.mappings"
# in its path; the big plain minecraft-server.jar is still obfuscated.
$mc = $cand | Where-Object { $_.Name -match "minecraft-merged" -and $_.FullName -match "loom\.mappings" } |
      Sort-Object Length -Descending | Select-Object -First 1
""
"### MC JAR: $($mc.FullName) [$([math]::Round($mc.Length/1MB,1)) MB]"

$list = & $jarTool tf $mc.FullName

""
"### CLASS LOCATIONS"
$list | Select-String -Pattern "(Identifier|ResourceLocation|RenderTypes?|DataComponents|DataComponentGetter|MouseButtonEvent)\.class$" |
    ForEach-Object { $_.Line }

function DumpFull($jar, $cls) {
    ""
    "### javap $cls"
    & $javap -classpath $jar $cls 2>&1
}
function DumpGrep($jar, $cls, $pat) {
    ""
    "### javap $cls (filtered: $pat)"
    & $javap -classpath $jar $cls 2>&1 | Select-String -Pattern $pat | ForEach-Object { $_.Line }
}

$idClass = ($list | Select-String -Pattern "Identifier\.class$" | Select-Object -First 1).Line -replace "/", "." -replace "\.class$", ""
if ($idClass) { DumpFull $mc.FullName $idClass }

DumpFull $mc.FullName "net.minecraft.resources.ResourceKey"
DumpFull $mc.FullName "net.minecraft.core.component.DataComponentGetter"
DumpGrep $mc.FullName "net.minecraft.world.entity.animal.wolf.Wolf" "ariant|ollar|omponent"
DumpGrep $mc.FullName "net.minecraft.world.entity.Entity" "omponent"
DumpGrep $mc.FullName "net.minecraft.world.entity.LivingEntity" "omponent"
DumpGrep $mc.FullName "net.minecraft.world.entity.Mob" "omponent"
DumpFull $mc.FullName "net.minecraft.client.gui.components.AbstractButton"
DumpGrep $mc.FullName "net.minecraft.client.gui.components.AbstractWidget" "render|onClick|onPress|arration|ound|ressed"
DumpGrep $mc.FullName "net.minecraft.core.component.DataComponents" "WOLF"
DumpGrep $mc.FullName "net.minecraft.core.Holder" "is|unwrapKey|getRegisteredName"
DumpGrep $mc.FullName "net.minecraft.client.renderer.MultiBufferSource" "getBuffer"
DumpGrep $mc.FullName "net.minecraft.world.level.Level" "lient"

DumpGrep $mc.FullName "net.minecraft.client.renderer.rendertype.RenderTypes" "ntity"
DumpFull $mc.FullName "net.minecraft.network.protocol.common.custom.CustomPacketPayload"

DumpGrep $mc.FullName "net.minecraft.client.renderer.GameRenderer" "amera"

# Fabric's reworked world render events: exact context/event signatures.
$fj = Get-ChildItem -Recurse -Path $roots -Filter "*.jar" |
      Where-Object { $_.Name -match "fabric-rendering-v1" -and $_.Name -notmatch "sources" } |
      Select-Object -First 1
""
"### FABRIC RENDERING JAR: $($fj.FullName)"
DumpFull $fj.FullName "net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents"
DumpFull $fj.FullName 'net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents$AfterEntities'
DumpFull $fj.FullName "net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext"
DumpFull $fj.FullName "net.fabricmc.fabric.api.client.rendering.v1.world.AbstractWorldRenderContext"
DumpFull $fj.FullName "net.fabricmc.fabric.api.client.rendering.v1.world.WorldTerrainRenderContext"
