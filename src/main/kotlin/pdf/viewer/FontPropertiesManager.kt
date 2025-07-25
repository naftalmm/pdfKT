package pdf.viewer

import org.icepdf.core.pobjects.fonts.FontManager
import java.util.logging.Level
import java.util.logging.Logger
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

/**
 *
 * This class provides a basic Font Properties Management system.
 * In order for font substitution to work more reliable it is beneficial that it has read and cached all system fonts.
 * The scanning of system fonts can be time-consuming and negatively effect the startup time of the library.
 * To speed up subsequent launches of the PDF library the fonts are stored using the Preferences API using a backing store determined by the JVM.
 *
 * This class is a simplified & translated to Kotlin version of org.icepdf.ri.util.FontPropertiesManager
 */
object FontPropertiesManager {
    private val logger: Logger = Logger.getLogger(FontPropertiesManager::class.java.toString())

    // can't use system level cache on window as of JDK 1.8_14, but should work in 9.
    private val prefs: Preferences = Preferences.userNodeForPackage(javaClass)

    /**
     * Gets the underlying fontManger instance which is also a singleton.
     *
     * @return current font manager
     */
    private val fontManager: FontManager = FontManager.getInstance()

    /**
     * Checks to see if there is currently any cached properties in the backing store; if so they are returned,
     * otherwise a full read of the system fonts takes place and the results are stored in the backing store.
     */
    fun loadOrReadSystemFonts() {
        if (isFontPropertiesEmpty()) {
            readDefaultFontProperties()
            saveProperties()
        } else {
            // load properties from cache into the fontManager
            loadProperties()
        }
    }

    /**
     * Reads the default font paths as defined by the [FontManager] class.  This method does not save
     * any fonts to the backing store.
     *this
     * @param paths any extra paths that should be read as defined by the end user.
     */
    private fun readDefaultFontProperties(vararg paths: String?) {
        try {
            fontManager.readSystemFonts(paths)
        } catch (e: Exception) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Error reading system fonts path: ", e)
            }
        }
    }

    /**
     * Loads any font properties stored in the backing store and are passed to the [FontManager] class.  No
     * changes are made to the backing store.
     */
    private fun loadProperties() {
        fontManager.setFontProperties(prefs)
    }

    /**
     * Saves all fonts properties defined in the [FontManager] to the backing store.
     */
    private fun saveProperties() {
        val fontProps = fontManager.getFontProperties()
        for (key in fontProps.keys) {
            prefs.put(key as String?, fontProps.getProperty(key))
        }
    }

    /**
     * Check to see if any font properties are stored in the backing store.
     *
     * @return true if font properties backing store is empty, otherwise false.
     */
    private fun isFontPropertiesEmpty(): Boolean {
        try {
            return prefs.keys().isEmpty()
        } catch (e: BackingStoreException) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Error writing system fonts to backing store: ", e)
            }
        }
        return false
    }
}