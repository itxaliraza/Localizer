package common_components

import java.awt.Desktop
import java.net.URI

/** Opens [url] in the default browser. Returns false if the platform can't browse. */
fun openUrl(url: String): Boolean = runCatching {
    Desktop.getDesktop().browse(URI(url))
}.isSuccess
