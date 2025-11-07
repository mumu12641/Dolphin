package io.github.mumu12641.dolphin.network.utils

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

object Decaptcha {

    fun decaptcha(imgContent: ByteArray): String {
        val frames = extractFramesFromGif(imgContent)
        if (frames.isEmpty()) {
            return ""
        }
        val mergedImage = mergeFrames(frames)
        return recognizeDigitsWithTesseract(mergedImage)
    }

    private fun extractFramesFromGif(imgContent: ByteArray): List<BufferedImage> {
        val frames = mutableListOf<BufferedImage>()
        try {
            ByteArrayInputStream(imgContent).use { inputStream ->
                val reader = ImageIO.getImageReadersByFormatName("gif").next()
                ImageIO.createImageInputStream(inputStream).use { imageInputStream ->
                    reader.input = imageInputStream
                    val frameCount = reader.getNumImages(true)
                    for (i in 0 until frameCount) {
                        val image = reader.read(i)
                        val grayImage =
                            BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_GRAY)
                        grayImage.createGraphics().apply {
                            drawImage(image, 0, 0, null)
                            dispose()
                        }
                        frames.add(grayImage)
                    }
                }
                reader.dispose()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return frames
    }

    private fun mergeFrames(frames: List<BufferedImage>): BufferedImage {
        val width = frames.first().width
        val height = frames.first().height
        val mergedImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)

        mergedImage.createGraphics().apply {
            color = Color.WHITE
            fillRect(0, 0, width, height)
            dispose()
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val darkPixelCount = frames.count { (it.getRGB(x, y) and 0xFF) < 254 }
                if (darkPixelCount >= 3) {
                    mergedImage.setRGB(x, y, Color.BLACK.rgb)
                }
            }
        }
        return mergedImage
    }

    private fun recognizeDigitsWithTesseract(image: BufferedImage): String {
        val tempImageFile = File.createTempFile("captcha_image", ".png")
        val tempOutputBase = File.createTempFile("tesseract_output", "")
        val tempOutputTxt = File("${tempOutputBase.absolutePath}.txt")

        try {
            ImageIO.write(image, "png", tempImageFile)

            val appDir = System.getProperty("user.dir")
            val tesseractPath = File(appDir, "tesseract/tesseract.exe").absolutePath
            val tessdataPath = File(appDir, "tesseract/tessdata").absolutePath

            val processBuilder = ProcessBuilder(
//                tesseractPath,
                "tesseract",
                tempImageFile.absolutePath,
                tempOutputBase.absolutePath,
//                "--tessdata-dir", tessdataPath,
                "--tessdata-dir", "D:\\Softwares\\Tesseract-OCR\\tessdata",
                "-c", "tessedit_char_whitelist=0123456789",
                "--psm", "6", "txt"
            )

            val process = processBuilder.redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                return ""
            }

            return if (tempOutputTxt.exists()) tempOutputTxt.readText().trim() else ""
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        } finally {
            tempImageFile.delete()
            tempOutputBase.delete()
            tempOutputTxt.delete()
        }
    }
}
