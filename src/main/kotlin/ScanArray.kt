import kotlinx.coroutines.delay

val myFilterBits = arrayOf<UByte>(0x03u, 0x0Cu, 0x30u, 0xC0u)
val goodSumUniqueIp = arrayOf<UByte>(0x01u, 0x04u, 0x10u, 0x40u)

class ScanArray {

    private fun reconstructIpAddress(indexArray: Int, counterNumberInByte: Int): String {
        val prom: Long = indexArray * 4L + counterNumberInByte
        return (((prom shr 24) and 0xFF).toUByte()).toString() + "." +
                (((prom shr 16) and 0xFF).toUByte()).toString() + "." +
                (((prom shr 8) and 0xFF).toUByte()).toString() + "." +
                ((prom and 0xFF).toUByte()).toString() + "\n"
    }

    suspend fun updateReadProgress() {
        var readyRun: Boolean = true
        var finished: Double = 0.0
        var msgFinished: String = ""

        while (readyRun) {
            delay(200)
            finished = ((sumReadedLenght.toDouble() / fileIpAddressesLength.toDouble()) * 100)
            msgFinished = "%.2f".format(finished) + "%"

            //print("--> countReadedIpAddresses $countReadedIpAddresses   sumReadedLenght $sumReadedLenght\r")
            print("--> FileLength $fileIpAddressesLength   Readed $sumReadedLenght   ReadedIp $countReadedIpAddresses   $msgFinished\r")
            if (readerUpdaterStopRun){
                readyRun = false
            }
        }
    }

    private fun updateFilterArray(indexArray: Int, counterNumberInByte: Int) {

        // IndexArray - номер байта в фильтрующем массиве, 0..1073741823
        // counterNumberInByte - номер двухбитового счетчика в пределах байта, 0..3

        val readByte: Int = filterArray[indexArray].toInt()
        var counterValue: Int = 0
        var writeByte: UByte = 0u

        // обработка байтовых значений в виде Int нужна для работы функций shl и shr
        when (counterNumberInByte) {
            0 -> {
                counterValue = readByte and 0x3
            }
            1 -> {
                counterValue = (readByte shr 2) and 0x3
            }
            2 -> {
                counterValue = (readByte shr 4) and 0x3
            }
            3 -> {
                counterValue = (readByte shr 6) and 0x3
            }
        }

        counterValue++
        if (counterValue > 1) {
            counterValue = 2
        }

        // очистка битов счетчика в байте обнулением и добавление двухбитогового нового значения счетчика
        //    в место в байте согласно номеру счетчика
        when (counterNumberInByte) {
            0 -> {
                // FC 11111100    3 00000011
                writeByte = (readByte and 0xFC).toUByte() or (counterValue and 0x3).toUByte()
            }
            1 -> {
                // F3 11110011    C 00001100
                writeByte =
                    (readByte and 0xF3).toUByte() or ((counterValue shl 2) and 0xC).toUByte()
            }
            2 -> {
                // CF 11001111    30 00110000
                writeByte =
                    (readByte and 0xCF).toUByte() or ((counterValue shl 4) and 0x30).toUByte()
            }
            3 -> {
                // 3F 00111111    C0 11000000
                writeByte =
                    (readByte and 0x3F).toUByte() or ((counterValue shl 6) and 0xC0).toUByte()
            }
        }

        filterArray[indexArray] = writeByte

    }

    fun readFile() {
        var ipString: String = ""
        var ipStringLength: Int = 0
        val iterator = fileInIpReader.lineSequence().iterator()

        while(iterator.hasNext()) {
            ipString = iterator.next()
            ipStringLength  = ipString.length
            if (ipStringLength > 6) {
                sumReadedLenght += ipString.length

                val bytes = ipString.split(".")
                var pos = 0
                val ipLong: Long = (((bytes[pos++].toLong() shl 24) and 0xFF000000) or
                        ((bytes[pos++].toLong() shl 16) and 0x00FF0000) or
                        ((bytes[pos++].toLong() shl 8) and 0x0000FF00)
                        or (bytes[pos++].toLong() and 0x000000FF))
                val indexFilterArray: Int = (ipLong / 4L).toInt()
                val counterNumber: Int = (ipLong % 4L).toInt()
                updateFilterArray(indexFilterArray, counterNumber)
                countReadedIpAddresses++
            }
        }
        readerUpdaterStopRun = true
    }


    suspend fun updateWriteProgress() {
        var readyRun: Boolean = true
        var finished: Double = 0.0
        var msgFinished: String = ""

        while (readyRun) {
            delay(200)
            finished = ((scanNumber.toDouble() / filterArraySize.toDouble()) * 100)
            msgFinished = "%.2f".format(finished) + "%"
            print("--> FilterArraySize = $filterArraySize   scanNumber = $scanNumber   $msgFinished\r")
            if (writerUpdaterStopRun) {
                readyRun = false
            }
        }
    }

    fun incScanNumber() {
        var myUByte: UByte = 0u

        while (scanNumber < filterArraySize) {
            myUByte = filterArray[scanNumber]
            for (i in 0..3) {
                if ((myUByte and myFilterBits[i]) == goodSumUniqueIp[i]) {
                    //WriteIpAddress(scanNumber, i)
                    kotlin.runCatching {
                        fileOutIpWriter.write(reconstructIpAddress(scanNumber, i))
                    }
                    countWritedIpAddresses++
                }
            }
            scanNumber++
        }
        writerUpdaterStopRun = true
        //println("finished scanNumber = $scanNumber")
    }


}