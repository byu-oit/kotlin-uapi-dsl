package edu.byu.uapi.spi.input

import kotlin.reflect.KClass

object Params {
    interface Filtering<Filter : Any> {
        val filter: Filter?

        interface Companion<Filter : Any> {
            val filterType: KClass<Filter>
        }
    }

    interface Sorting<SortableField : Enum<SortableField>> {
        val sort: SortParams<SortableField>

        interface Companion<SortableField : Enum<SortableField>> {
            val defaultSortFields: List<SortableField>
            val defaultSortOrder: SortOrder
                get() = SortOrder.ASCENDING
        }
    }

    interface Searching<SearchContext : Enum<SearchContext>> {
        val search: SearchParams<SearchContext>?

        interface Companion<SearchContext : Enum<SearchContext>> {
            val searchContextFields: Map<SearchContext, Collection<String>>
        }
    }
}

interface BetweenInclusive<Type> {
    val start: Type?
    val end: Type?
}

interface BetweenExclusive<Type> {
    val start: Type?
    val end: Type?
}

data class SortParams<SortableFields : Enum<SortableFields>>(
    val fields: List<SortableFields>,
    val order: SortOrder
)

data class SearchParams<SearchContext : Enum<SearchContext>>(
    val context: SearchContext,
    val text: String
)

enum class SortOrder {
    ASCENDING, DESCENDING
}
//
//enum class PersonSearchContexts {
//    NAME, IDENTIFIERS
//}
//
//enum class PersonSortParams {
//    BYU_ID, SORT_NAME, BIRTHDAY
//}
//
//data class PersonSearchFilters(
//    val byuId: String?,
//    val birthday: LocalDate?
//) {
//}
//
////TODO: Delete this
//data class PersonSearchParams(
//    override val filter: PersonSearchFilters?,
//    override val sort: SortParams<PersonSortParams>,
//    override val search: SearchParams<PersonSearchContexts>?
//) : Filtering<PersonSearchFilters>,
//    Searching<PersonSearchContexts>,
//    Sorting<PersonSortParams> {
//
//    companion object :
//        Filtering.Companion<PersonSearchFilters>,
//        Searching.Companion<PersonSearchContexts>,
//        Sorting.Companion<PersonSortParams> {
//        override val filterType: KClass<PersonSearchFilters> = PersonSearchFilters::class
//        override val defaultSortFields: List<PersonSortParams> = listOf(PersonSortParams.BYU_ID)
//        override val searchContextFields: Map<PersonSearchContexts, Collection<String>> = mapOf(
//            PersonSearchContexts.IDENTIFIERS to setOf("byu_id", "person_id"),
//            PersonSearchContexts.NAME to setOf("sort_name", "preferred_first_name")
//        )
//    }
//
//}