package data.util

import java.awt.Desktop
import java.io.File

fun openDownloadsFolder(downloadsPath:String) {
       try {
            val downloadsFolder = File(downloadsPath)
           if (downloadsFolder.exists() && Desktop.isDesktopSupported()) {
               Desktop.getDesktop().open(downloadsFolder)
           }
       } catch (e: Exception) {
           e.printStackTrace()
       }
   }