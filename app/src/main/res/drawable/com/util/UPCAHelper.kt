package com.util

class UPCAHelper {


    fun calculateUPCAChecksum(barcodeWithoutChecksum: String): Int {
        val reversed = barcodeWithoutChecksum.reversed().toCharArray()
        val sum = (0 until reversed.size).sumOf { i ->
            Character.getNumericValue(reversed[i]) * if (i % 2 == 0) 3 else 1
        }
        return (10 - sum % 10) % 10
    }


    fun convertToIS(upca: String): String {
        return "IS00" + upca.substring(2, upca.length - 1)
    }
    fun isValidUPCA(barcode: String): Boolean {
        if(barcode.startsWith("230"))
            return false;

        if (barcode.length != 12 || ( !barcode.startsWith("22") && !barcode.startsWith("23")) ){
            return false
        }

        val checksumDigit = Character.getNumericValue(barcode[11])
        val barcodeWithoutChecksum = barcode.substring(0, 11)
        val expectedChecksum = calculateUPCAChecksum(barcodeWithoutChecksum)

        return checksumDigit == expectedChecksum
    }



}