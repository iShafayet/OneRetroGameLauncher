package com.sayemshafayet.onereogamelauncher.ui.components

/**
 * Maps ES-DE system folder names to bundled console icons
 * ([app/src/main/assets/system_icons], from
 * [KyleBing/retro-game-console-icons](https://github.com/KyleBing/retro-game-console-icons), GPL-3.0).
 *
 * Source filenames use TrimUI / Miyoo-style short codes (FC, SFC, MD, …).
 */
object SystemIconResolver {
    const val ASSET_DIR = "system_icons"

    /**
     * Asset-relative path suitable for Coil (`file:///android_asset/…`),
     * or null when no icon is bundled for [folderName].
     */
    fun assetPathForFolder(folderName: String): String? {
        val icon = iconFileForFolder(folderName) ?: return null
        return "file:///android_asset/$ASSET_DIR/$icon"
    }

    fun iconFileForFolder(folderName: String): String? {
        val key = folderName.trim().lowercase()
        if (key.isEmpty()) return null
        return FOLDER_TO_ICON[key]
    }

    /** ES-DE folder → icon filename under [ASSET_DIR]. */
    private val FOLDER_TO_ICON: Map<String, String> = mapOf(
        // Nintendo
        "nes" to "FC.png",
        "famicom" to "FC.png",
        "fds" to "FDS.png",
        "snes" to "SFC.png",
        "snesna" to "SFC.png",
        "sfc" to "SFC.png",
        "satellaview" to "SATELLAVIEW.png",
        "sufami" to "SUFAMI.png",
        "sgb" to "SGB.png",
        "gb" to "GB.png",
        "gbc" to "GBC.png",
        "gba" to "GBA.png",
        "n64" to "N64.png",
        "n64dd" to "N64DD.png",
        "gc" to "NGC.png",
        "nds" to "NDS.png",
        "n3ds" to "3DS.png",
        "virtualboy" to "VB.png",
        "pokemini" to "POKEMINI.png",
        "wii" to "WII.png",

        // Sega
        "mastersystem" to "MS.png",
        "megadrive" to "MD.png",
        "megadrivejp" to "MD.png",
        "genesis" to "MD.png",
        "sega32x" to "SEGA32X.png",
        "sega32xjp" to "SEGA32X.png",
        "sega32xna" to "SEGA32X.png",
        "segacd" to "SEGACD.png",
        "megacd" to "SEGACD.png",
        "megacdjp" to "SEGACD.png",
        "gamegear" to "GG.png",
        "sg-1000" to "SG1000.png",
        "dreamcast" to "DC.png",
        "saturn" to "SATURN.png",
        "saturnjp" to "SATURN.png",
        "naomi" to "NAOMI.png",
        "naomi2" to "NAOMI.png",
        "naomigd" to "NAOMI.png",
        "atomiswave" to "ATOMISWAVE.png",

        // Sony
        "psx" to "PS.png",
        "ps2" to "PS2.png",
        "psp" to "PSP.png",

        // NEC
        "pcengine" to "PCE.png",
        "tg16" to "PCE.png",
        "pcenginecd" to "PCECD.png",
        "tg-cd" to "PCECD.png",
        "supergrafx" to "SFX.png",
        "pcfx" to "PCFX.png",

        // SNK
        "neogeo" to "NEOGEO.png",
        "neogeocd" to "NEOCD.png",
        "neogeocdjp" to "NEOCD.png",
        "ngp" to "NGP.png",
        "ngpc" to "NGP.png",

        // Atari
        "atari2600" to "ATARI2600.png",
        "atari5200" to "ATARI5200.png",
        "atari7800" to "ATARI7800.png",
        "atari800" to "ATARI800.png",
        "atarixe" to "ATARI800.png",
        "atarilynx" to "LYNX.png",
        "atarist" to "ATARIST.png",

        // Arcade / MAME
        "arcade" to "ARCADE.png",
        "mame" to "MAME.png",
        "mame-advmame" to "ADVMAME.png",
        "fbneo" to "FBNEO.png",
        "fba" to "ARCADE_FBNEO.png",
        "cps" to "CPS1.png",
        "cps1" to "CPS1.png",
        "cps2" to "CPS2.png",
        "cps3" to "CPS3.png",
        "daphne" to "DAPHNE.png",
        "consolearcade" to "ARCADE.png",

        // Computers
        "amiga" to "AMIGA.png",
        "amiga600" to "AMIGA.png",
        "amiga1200" to "AMIGA.png",
        "amigacd32" to "AMIGACD.png",
        "cdtv" to "AMIGACDTV.png",
        "c64" to "C64.png",
        "vic20" to "VIC20.png",
        "plus4" to "CPLUS4.png",
        "amstradcpc" to "CPC.png",
        "gx4000" to "CPC.png",
        "msx" to "MSX.png",
        "msx1" to "MSX.png",
        "msx2" to "MSX2.png",
        "msxturbor" to "MSX2.png",
        "pc" to "DOS.png",
        "dos" to "DOS.png",
        "pc88" to "PC88.png",
        "pc98" to "PC98.png",
        "x1" to "X1.png",
        "x68000" to "X68000.png",
        "zxspectrum" to "ZXS.png",
        "macintosh" to "VMAC.png",
        "thomson" to "THOMSON.png",
        "to8" to "THOMSON.png",

        // Other consoles / handhelds
        "3do" to "PANASONIC.png",
        "colecovision" to "COLECO.png",
        "channelf" to "CHANNELF.png",
        "intellivision" to "INTELLIVISION.png",
        "vectrex" to "VECTREX.png",
        "odyssey2" to "VIDEOPAC.png",
        "videopac" to "VIDEOPAC.png",
        "wonderswan" to "WS.png",
        "wonderswancolor" to "WSC.png",
        "supervision" to "SUPERVISION.png",
        "megaduck" to "MEGADUCK.png",
        "gameandwatch" to "GW.png",
        "arduboy" to "ARDUBOY.png",
        "palm" to "PALMOS.png",
        "uzebox" to "UZEBOX.png",

        // Engines / ports
        "doom" to "DOOM.png",
        "quake" to "TYRQUAKE.png",
        "scummvm" to "SCUMMVM.png",
        "easyrpg" to "EASYRPG.png",
        "chailove" to "CHAILOVE.png",
        "lutro" to "LUTRO.png",
        "lowresnx" to "LOWRESNX.png",
        "pico8" to "PICO.png",
        "tic80" to "TIC.png",
        "openbor" to "OPENBOR.png",
        "ports" to "PORTS.png",
    )
}
