package fr.hardel.asset_editor.client.compose.window.chrome

object NativeChromeFactory {

    private enum class Os { WINDOWS, MACOS, LINUX }

    private val os: Os = run {
        val name = System.getProperty("os.name").orEmpty().lowercase()
        when {
            name.startsWith("windows") -> Os.WINDOWS
            name.startsWith("mac") || name.startsWith("darwin") -> Os.MACOS
            else -> Os.LINUX
        }
    }

    fun create(): NativeWindowChrome = when (os) {
        Os.WINDOWS -> WindowsChrome()
        Os.MACOS -> MacOsChrome()
        Os.LINUX -> LinuxChrome()
    }

    /**
     * Platform-wide runtime setup that must run exactly once, before any frame is created.
     * LaF installation, system properties, etc.
     */
    fun prepareRuntime() {
        when (os) {
            Os.WINDOWS -> WindowsChrome.prepareRuntime()
            Os.MACOS -> MacOsChrome.prepareRuntime()
            Os.LINUX -> LinuxChrome.prepareRuntime()
        }
    }
}
