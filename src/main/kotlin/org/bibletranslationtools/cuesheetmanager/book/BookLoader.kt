package org.bibletranslationtools.cuesheetmanager.book

import java.lang.Exception

class BookLoader {
    companion object {
        fun getBooks(): List<Book> {
            return try {
                val json = this::class.java.getResource("/book_catalog.json").readText()
                BooksMapper().fromJSON(json)
            } catch (ex: Exception) {
                listOf()
            }
        }
    }
}
