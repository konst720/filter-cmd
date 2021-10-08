import kotlinx.coroutines.*
import java.io.*


//const val inFileName: String = "C:\\ipfile\\ip_addresses"
const val inFileName: String = "C:\\ipfile\\part_21.txt"
const val outFileName: String = "E:\\vki\\ECWID\\outIp.txt"
const val filterCounters: Long = 256L * 256L * 256L * 256L // 4294967296
const val packedFilterCounters: Int = (filterCounters / 4L).toInt() // 1073741824

const val filterArraySize = packedFilterCounters
val filterArray = UByteArray(filterArraySize) { i -> 0u }

var scanNumber: Int = 0
var readerUpdaterStopRun: Boolean = false
var writerUpdaterStopRun: Boolean = false
var fileIpAddressesLength: Long = 0
lateinit var fileOutIpWriter: BufferedWriter
lateinit var fileInIpReader: BufferedReader
var countReadedIpAddresses: Long = 0
var countWritedIpAddresses: Long = 0
var sumReadedLenght: Long = 0


fun main(): Unit = runBlocking {

    val wrkProc = ScanArray()

    val inputFile = File(inFileName)
    fileIpAddressesLength = inputFile.length()
    fileInIpReader = inputFile.bufferedReader()
    countReadedIpAddresses = 0

    val jobsR = mutableListOf<Job>()

    jobsR += GlobalScope.launch {
        wrkProc.updateReadProgress()
    }
    jobsR += GlobalScope.launch {
        wrkProc.readFile()
    }

    jobsR.forEach { it.join() }

    fileInIpReader.close()
//    fileIpAddresses.close()

    println("  countReadedIpAddresses = $countReadedIpAddresses")


    // Тестовые заполнения фильтрующего массива для проверки работы
    //   формирователя Ip-адресов из текущего индекса массива и номера счетчика

//    filterArray[10] = (85u).toUByte()
//    filterArray[11] = (85u).toUByte()
//    filterArray[12] = (85u).toUByte()
//    filterArray[12] = 16u

//    for (i in 0..9999){
//        filterArray[i] = 85u // 01010101 - все четыре счетчика в байте - в состоянии "уникальный Ip"
//    }


    // Чтение фильтрующего массива и запись в выходной файл найденных уникальных Ip-адресов
    fileOutIpWriter = File(outFileName).bufferedWriter()
    countWritedIpAddresses = 0

    val jobsW = mutableListOf<Job>()
    val startTime = System.currentTimeMillis()

    jobsW += GlobalScope.launch {
        wrkProc.updateWriteProgress()
    }
    jobsW += GlobalScope.launch {
        wrkProc.incScanNumber()
    }

    jobsW.forEach { it.join() }
    val finTime = System.currentTimeMillis()
    fileOutIpWriter.close()

    println("Done   countWritedIpAddresses = $countWritedIpAddresses   delta = " +
            (countReadedIpAddresses - countWritedIpAddresses).toString() +
            "   writingTime = " + (finTime - startTime).toString() + "mc")

}

