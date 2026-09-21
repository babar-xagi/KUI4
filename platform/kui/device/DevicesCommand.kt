package kui.device

/**
 * Handlers for `kui devices` CLI command.
 * Lists connected physical Android devices and emulators with real-time status and actionable diagnostics.
 */
object DevicesCommand {

    fun execute(): Int {
        println("==================================================")
        println(" 📱 Android Devices (via ADB)")
        println("==================================================")
        val dm = DeviceManager()
        println("ADB Path: ${dm.adbPath}")
        println()

        val devices = dm.listDevices()
        if (devices.isEmpty()) {
            println("No connected devices or emulators found.")
            println()
            println("Troubleshooting tips:")
            println("  1. Connect your Android phone with a USB data cable (not charge-only).")
            println("  2. In phone Settings -> Developer Options -> Turn ON 'USB Debugging'.")
            println("  3. (Xiaomi/Tecno/Realme): Also turn ON 'Install via USB'.")
            println("  4. Set USB mode in phone notification shade to 'File Transfer' / 'MTP'.")
            println("  5. Check phone screen for 'Allow USB debugging?' dialog and tap 'Allow'.")
            return 0
        }

        println("Found ${devices.size} connected device(s):")
        for (d in devices) {
            val typeStr = if (d.isEmulator) "Emulator" else "Physical Device"
            val modelStr = d.model ?: d.product ?: "Android Device"
            when {
                d.isOnline -> {
                    println("  [ONLINE]       ${d.serial.padEnd(20)} ($typeStr: $modelStr)")
                }
                d.isUnauthorized -> {
                    println("  [UNAUTHORIZED] ${d.serial.padEnd(20)} ($typeStr: $modelStr)")
                    println("                 ⚠️ ACTION: Unlock phone screen and tap 'Allow USB debugging'!")
                }
                d.isOffline -> {
                    println("  [OFFLINE]      ${d.serial.padEnd(20)} ($typeStr: $modelStr)")
                    println("                 ⚠️ ACTION: Reconnect USB cable or toggle USB debugging off and on.")
                }
                else -> {
                    println("  [${d.state.uppercase().padEnd(12)}] ${d.serial.padEnd(20)} ($typeStr: $modelStr)")
                }
            }
        }
        println()
        return 0
    }
}
