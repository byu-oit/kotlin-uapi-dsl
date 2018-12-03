package edu.byu.uapi.kotlin.examples.library.impl

import edu.byu.uapi.kotlin.examples.library.Book
import edu.byu.uapi.kotlin.examples.library.Library
import edu.byu.uapi.server.resources.list.ListResource
import edu.byu.uapi.server.response.ResponseField
import edu.byu.uapi.server.response.uapiResponse
import edu.byu.uapi.spi.input.ListParams

/**
 * Created by Scott Hutchings on 9/17/2018.
 * kotlin-uapi-dsl-pom
 */
class BookResource : ListResource.Simple<
    MyUserContext, // user info
    Long, // id
    Book // model
    > {

    override val pluralName: String = "books"

    override fun loadModel(
        userContext: MyUserContext,
        id: Long
    ): Book? {
        return Library.getBookByOclc(id)
    }

    override fun canUserViewModel(
        userContext: MyUserContext,
        id: Long,
        model: Book
    ): Boolean {
        return true // all books are publicly viewable
    }

    override fun idFromModel(model: Book): Long {
        return model.oclc
    }

    override fun list(
        userContext: MyUserContext,
        params: ListParams.Empty
    ): List<Book> {
        TODO("not implemented")
    }

    override val responseFields: List<ResponseField<MyUserContext, Book, *>>
        get() = uapiResponse {
            value(Book::oclc) {
                key = true
                displayLabel = "OCLC Control Number"
            }
            nullableValue(Book::isbn) {
                isSystem = true
                displayLabel = "International Standard Book Number"
            }
            value(Book::title) {
            }
            nullableValue(Book::subtitles) {
            }
            value(Book::publishedYear) {
            }
            value<Int>("publisher") {
                getValue { book -> book.publisher.id }
                description { book, publisherId -> book.publisher.commonName }
            }
        }
}


