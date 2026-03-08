package com.ritesh.parser.core.bank

fun main() {
    val sender = "JK-NOBRKR-S"
    val parser = BankParserFactory.getParser(sender)
    println("Parser for $sender: ${parser?.getBankName() ?: "None"}")
}
