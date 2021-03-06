package edu.byu.uapi.library

import edu.byu.uapi.kotlin.examples.library.*
import edu.byu.uapi.model.UAPISortOrder
import edu.byu.uapi.server.resources.ResourceRequestContext
import edu.byu.uapi.server.resources.list.ListResource
import edu.byu.uapi.server.resources.list.fields
import edu.byu.uapi.spi.input.ListWithTotal

class BooksResource : ListResource<LibraryUser, OCLCNumber, Book, BookListParams>,
                      ListResource.ListWithSort<LibraryUser, OCLCNumber, Book, BookListParams, BookSortProperty>,
                      ListResource.ListWithFilters<LibraryUser, OCLCNumber, Book, BookListParams, BookFilters>,
                      ListResource.ListWithSearch<LibraryUser, OCLCNumber, Book, BookListParams, BookSearchContext>,
                      ListResource.ListWithSubset<LibraryUser, OCLCNumber, Book, BookListParams> {

    override val pluralName: String = "books"

    override val scalarIdParamName: String = "oclc"

    override fun loadModel(
        requestContext: ResourceRequestContext,
        userContext: LibraryUser,
        id: OCLCNumber
    ): Book? {
        return Library.getBookByOclc(id.oclc)
    }

    override fun idFromModel(model: Book): OCLCNumber {
        return model.oclc
    }

    override fun canUserViewModel(
        requestContext: ResourceRequestContext,
        userContext: LibraryUser,
        id: OCLCNumber,
        model: Book
    ): Boolean {
        return userContext.canViewBook(model)
    }

    override fun list(
        requestContext: ResourceRequestContext,
        userContext: LibraryUser,
        params: BookListParams
    ): ListWithTotal<Book> {
        val search = params.search?.run { context.toDomain(text) }
        val result = Library.listBooks(
            includeRestricted = userContext.canViewRestrictedBooks,
            sortColumns = params.sort.properties.map { it.domain },
            sortAscending = params.sort.order == UAPISortOrder.ASCENDING,
            filters = params.filters?.toDomain(),
            search = search,
            subsetSize = params.subset.subsetSize,
            subsetStart = params.subset.subsetStartOffset
        )
        return ListWithTotal(
            totalItems = result.totalItems,
            values = result.list
        )
    }

    override val listDefaultSortProperties: List<BookSortProperty> = listOf(BookSortProperty.TITLE, BookSortProperty.OCLC)
    override val listDefaultSortOrder: UAPISortOrder = UAPISortOrder.ASCENDING
    override val listDefaultSubsetSize: Int = 50
    override val listMaxSubsetSize: Int = 100
    override fun listSearchContexts(value: BookSearchContext) = when (value) {
        BookSearchContext.TITLES -> listOf("title", "subtitles")
        BookSearchContext.AUTHORS -> listOf("authors.name")
        BookSearchContext.GENRES -> listOf("genres.codes", "genres.name")
        BookSearchContext.CONTROL_NUMBERS -> listOf("oclc", "isbn")
    }

    override val responseFields = fields {
        value(Book::oclc) {
            key = true
            displayLabel = "OCLC Control Number"
            doc = "Control number assigned to this title by the [Online Computer Library Center](www.oclc.org)."
        }
        value(Book::title) {
            displayLabel = "Title"
            doc = "The main title of the book"
            modifiable { libraryUser, book, title -> libraryUser.canModifyBooks }
        }
        value<Int>("publisher_id") {
            getValue { book -> book.publisher.id }
            displayLabel = "Publisher"
            modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }

            description { book, publisherId -> book.publisher.commonName }
            longDescription { book, publisherId -> book.publisher.fullName }
        }
        value(Book::availableCopies) {
            isDerived = true
            displayLabel = "Available Copies"
        }
        nullableValue(Book::isbn) {
            isSystem = true
            displayLabel = "ISBN"
            doc = "International Standard Book Number"
        }
        valueArray(Book::subtitles) {
            displayLabel = "Subtitles"
            doc = "The book's subtitles, if any"
            modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
        }
        mappedValueArray("author_ids", Book::authors, Author::authorId) {
            description(Author::name)
            displayLabel = "Author(s)"
            modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
        }
        mappedValueArray(Book::genres, Genre::code) {
            displayLabel = "Genre(s)"
            description(Genre::name)
            modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
        }
        value(Book::publishedYear) {
            displayLabel = "Publication Year"
            doc = "The year the book was published"
            modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
        }
        value(Book::restricted) {
            displayLabel = "Is Restricted"
            doc = "Whether the book is shelved in the Restricted Section"
            modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
        }
    }

}
