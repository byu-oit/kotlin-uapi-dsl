---
title: Generating Response Bodies
short_title: Responses
order: 5
tutorial_source: https://github.com/byu-oit/kotlin-uapi/tree/master/examples/library/tutorial-steps/5-response-body
---

# Contents
{:.no_toc}

* This will become the Table of Contents
{:toc}

In our last section, we stubbed out a response body containing two basic fields: `oclc` and `title`.
In this section, we're going to expand the list of fields, as well as adding a lot of useful metadata
to them, in accordance with the University API Specification.

This is a long chapter - it's one of the most important parts of our API! So, strap in, you're here for the long haul! But
don't worry, there's cake at the end!

![The Cake is a Lie!](./img/portal-cake-lie.png)

Unless otherwise specified, all JSON examples are from *Good Omens*, `http://localhost:8080/books/26811595`. Here's what that
request looks like using `cURL`:

```bash
curl -H "Authorization: Bearer {your OAuth token here}" http://localhost:8080/books/26811595
```

> While all of our examples will use `curl`, you may want to use a graphical HTTP client like [Postman](https://www.getpostman.com/).
> 
> If you want to use a command-line tool like `curl`, you may want to install [jq](https://stedolan.github.io/jq/) and pipe your responses
> to it. `jq` is a tool for pretty-printing JSON.
> 
> ```bash
> curl -H "Authorization: Bearer {your OAuth token here}" http://localhost:8080/books/26811595 | jq
> ```
{: .callout-protip }

# Review of the Specification

If you've read the UAPI specification, you know that there is no such thing as a simple key-value field. If you haven't,
well, then, *surprise!*

This means that fields in a response don't just look like this:

```json
{
  "title": "Good Omens"
}
```

Instead, we wrap the value with a bunch of metadata:

```json
{
  "title": {
    "value": "Good Omens",
    "api_type": "read-only",
    "display_label": "Title"
  }
}
```

Like we said, *surprise!* Go on, get it out of your system, and then come back when you want to hear the reasons *why* we
would do this.

We do this so as to provide information to the client about both the field and the value of the field. 
The intention is to simplify the development of client applications, especially Web UIs, by providing them with the information
they need to be able to drive decisions that would otherwise require the addition of business logic to the client.

There are three groupings of these metadata values: Field-Related, Value-Derived, and Contextual.

## Field-Related Metadata

Field-related metadata describes the *field* they are a part of, and do not change based on the value of the field. In
general, every response from the API will return the same values for these metadata (in fact, this runtime enforces that rule).

### "key"

*Optional Boolean*

Denotes whether this field is part of the URL path identifier of the resource.

If not set to `true`, this field is generally not set at all.

### "display_label"

*Optional String*

A suggested label for a UI to use for this property. Shouldn't be longer than 30 characters.

### "domain"

*Optional URL*

A link to a [Meta Data Resource](https://github.com/byu-oit/UAPI-Specification/blob/master/University%20API%20Specification.md#80-meta-data-sets)
that describes the allowable values of this field. Domains have their [own chapter](./domains.md), so don't worry about them yet.

## Value-Derived Metadata

Value-Derived metadata is derived from the actual value of a field. These metadata do not describe the field itself,
but rather the specific value in it. If they are present for one value of a field, they should be present for all values
(we enforce this rule too!).

### "description"

*Optional, Nullable String*

A short, human-friendly description of the value. Generally limited to 30 characters. 

For a 'state' field with a value of 'VA', this might be the common name of the state, like 'Virginia'.

### "long_description"

*Optional, Nullable String*

A slightly longer, human-friendly description of the value. Generally limited to 256 characters. 

For a 'state' field with a value of 'VA', this might be the full ceremonial name of the state, like 'Commonwealth of Virginia'.

### "related_resources"

*Optional URI*

A link to a related URL. This is generally a link to the system-of-record for this value.

As an example, for a user ID field, this would point to that user's record in the central Identity system.

If this is set, `api_type` must be set to `related`.

## Contextual Metadata

Contextual metadata are driven by a combination of the field, the value of the field, and some other information, such 
as the state of the Resource and the client's authorizations. It's okay if you don't fully understand how they work;
the Runtime will do most of the hard work of making sure you send back the correct values.

### "api_type"

*Required String Enum*

This describes how the value can be used within the API. For example, if the currently-logged in user can modify
a value on the current record, the `api_type` would be set to `"modifiable"`.

`api_type` currently allows the following values:

`api_type` | Description | Examples
-----------|-------------|----------
`"system"` | Denotes that this value was assigned by the system, and is not ever modifiable through the API. | Last-updated time fields, generated identifiers
`"modifiable"` | The value may be modified through the API, and the current user is allowed to do so. | 
`"read-only"` | The value is not modifiable. This may be always true for the field, or it may reflect the current user's permissions. | 
`"derived"` | The value is derived from some other fields. | Student GPA, class standing, etc.
`"related"` | The responsibility for manipulating this property doesn't belong to this API. The business logic to modify the property exists in the related_resource. | The ID of a user from a central Identity system

## Value Fields

The value of the field is mapped to a different key, depending on what type of data it contains.

Key | Data Type
-----------|-----------
`"value"` | Simple values (strings, numbers, booleans)
`"values"` | Arrays of simple values + metadata
`"object"` | Complex objects + metadata
`"objects"` | Arrays of complex objects + metadata

# Structure of a Response

Let's take another look at the response we got from the server in our last section:

```json
{
  "basic": {
    "oclc": {
      "value": 26811595,
      "api_type": "read-only"
    },
    "title": {
      "value": "Good Omens",
      "api_type": "read-only"
    },
    "links": {},
    "metadata": {
      "validation_response": {
        "code": 200,
        "message": "OK"
      }
    }
  },
  "links": {},
  "metadata": {
    "validation_response": {
      "code": 200,
      "message": "OK"
    },
    "field_sets_returned": [
      "basic"
    ],
    "field_sets_available": [
      "basic"
    ],
    "field_sets_default": [
      "basic"
    ],
    "contexts_available": {}
  }
}
```

All of our actual values (`oclc`, `title`) are wrapped in an object called `basic`. The University API Specification has
the concept of ["Fieldsets,"](https://github.com/byu-oit/UAPI-Specification/blob/master/University%20API%20Specification.md#50-sub-resources-field_sets-and-contexts) 
which we will cover in detail in [another chapter](./subresources.md). The 
["basic" fieldset](https://github.com/byu-oit/UAPI-Specification/blob/master/University%20API%20Specification.md#513-the-basic-field_set)
consists of the properties of the root resource - in our case, "books." If our response contained other fieldsets, 
they would show up as siblings of the "basic" key.

Each fieldset, including "basic," has three parts to its response. The first is the actual fields and their values and metadata.
Then, there is a "links" section, containing [HATEOAS links](https://github.com/byu-oit/UAPI-Specification/blob/master/University%20API%20Specification.md#40-hateoas-links)
(we'll learn how to manipulate them in the chapter on [Links](./links.md)). Then, there is the response metadata, which
the UAPI runtime automatically generates for you. For the "basic" fieldset, it generally consists of a "validation_response",
containing information about the status of that fieldset, as well as an optional "validation_information" field.

The top level of the response also contains a "links" and "metadata" section, which give information about the entire request.

You shouldn't have to worry about the "links" section often, and you won't ever have to worry about the "metadata" section.
In almost all cases, the Runtime will handle these parts of the specification for you. The only exceptions
will be covered in the [Links](./links.md) chapter.

In the rest of this chapter, we'll show you how to add information to your top-level "books" resource, and thus to your "basic" fieldset.

# Adding Response Fields

There are four types of values we can add to our response: [simple values](#simple-values), 
[arrays of values](#arrays-of-values), [complex objects](#complex-objects), and [arrays of objects](#arrays-of-objects).
We'll show you how to add each of them here.

## Simple Values

Most of the fields in our responses will consist of simple values - Strings, numbers, booleans, etc. They're easy 
(compared to everything else, at least), so we'll start with them!

### General Syntax

In our last section, we stubbed out our `responseFields` definition:

```kotlin
override val responseFields = fields {
    value<Long>("oclc") {
        getValue { book -> book.oclc }
    }
    value<String>("title") {
        getValue { book -> book.title }
    }
}
```

This produced two fields, named, unsurprisingly, "oclc" and "title". The response contained the value of each field and 
an `api_type` value of `read-only`.

> It's not too late to change this syntax. One alternative that has been considered looks like this:
> 
> ```kotlin
> override val responseFields = fields {
>     "oclc" isA value<Long> {
>         getValue { book -> book.oclc }
>     }
> }
> ```
> 
> Your comments are welcome!
{: .callout-might-change }

Both of these values are extracted directly from a field on the `Book` object. Because this is such a common occurrence,
the Runtime provides a bit of syntactic sugar to make this easier. If your Model object (in this case, `Book`) has a
field with the same name as the field in the response, you can use a Kotlin Property Reference to create the response field:

```diff
     override val responseFields = fields {
-        value<Long>("oclc") {
+        value(Book::oclc) {
-            getValue { book -> book.oclc }
         }
-        value<String>("title") {
+        value(Book::title) {
-            getValue { book -> book.title }
         }
```

Notice how we no longer have to specify the type or name of the field, nor how to get it from a `Book`. That's because 
the property reference tells the Runtime all of that information. Most of our fields can probably be defined this way!

> The `Book::oclc` syntax is a Kotlin [Property Reference](https://kotlinlang.org/docs/reference/reflection.html#property-references).
> Instead of giving us the value of a property, a Property Reference gives us information *about* the property, such
> as its name and its type.
{: .callout-kotlin }

> The Runtime will take care of translating your camelCase names to the UAPI's snake_case format.
{: .callout-protip }

### Keys 

Let's add some metadata to `oclc`. 

`oclc` is the key we use in the URL to look up our resource, so let's set `key = true`.

```diff
  value(Book::oclc) {
+   key = true
  }
```

```json
  "oclc": {
      "value": 26811595,
      "api_type": "read-only",
      "key": true
  },
```

### System Fields

`oclc` is a system-driven field, meaning that it's generated by the system not user-modifiable, so let's set `isSystem = true`:

```diff
  value(Book::oclc) {
    key = true
+   isSystem = true   
  }
```

Setting `isSystem` tells the Runtime that this is a system-generated value, which will result in the `api_type` being
set to `"system"`. We'll cover how `api_type` is determined [later](#how-we-derive-api_type). 

Now, our "oclc" JSON looks like this:

```json
  "oclc": {
      "value": 26811595,
      "api_type": "system",
      "key": true
  },
```

### Display Labels

Next, let's add a [`display_label`](#display_label):

```diff
  value(Book::oclc) {
    key = true
    isSystem = true
+   displayLabel = "OCLC Control Number"
  }
```

```json
  "oclc": {
      "value": 26811595,
      "api_type": "system",
      "key": true,
      "display_label": "OCLC Control Number"
  },
```

Note that the `displayLabel` value is static and does not include any information about the value of the field.

### Documentation

Finally, let's add some documentation to this field. This won't actually show up in our API responses, but it will
be used as part of our generated [documentation](./documenting.md).

```diff
  value(Book::oclc) {
    key = true
    isSystem = true
    displayLabel = "OCLC Control Number"
+   doc = "Control number assigned to this title by the [Online Computer Library Center](www.oclc.org)."
 }
```

The `doc` field can be as long as you want, and can contain valid Markdown expressions.

### Modifiable Values

Now, let's add some metadata to "title."

```kotlin
  value(Book::title) {
+   displayLabel = "Title"
+   doc = "The main title of the book"
  }
```

Nothing we haven't seen before, right? But, let's throw a wrench in the works - what if we need to be able to modify the
title? I mean, it's not likely, but it is theoretically possible that a member of the Most Pure and Undefiled Order of Librarians
could make a mistake while entering a title. So, we need to make sure that the `api_type` of "title" gets set to `"modifiable"`.

In order to do so, we need to tell the runtime what the rules are for being able to modify a book's title.  Let's 
start by adding a `canModifyBooks` value to `LibraryUser`:

```diff
     val isCardholder = cardholderId != null
+    val canModifyBooks: Boolean = isLibrarian
     val canViewRestrictedBooks = isLibrarian
```

Now, we can specify a `modifiable` function in the definition of "title".

```diff
  value(Book::title) {
    displayLabel = "Title"
    doc = "The main title of the book"
+   modifiable { libraryUser, book, title -> libraryUser.canModifyBooks }
  }
```

Now, if we make a request as a librarian user (Hey! That's you, remember!), we'll see that "title" is modifiable:

```json 
"title": {
  "value": "Good Omens",
  "api_type": "modifiable",
  "display_label": "Title"
},
```

If we were to make a request as a non-librarian user, `api_type` would be set to `"read_only"`.

### Descriptions

Some values, especially identifiers, aren't very descriptive. So, the UAPI allows us to give human-readable descriptions
of the value.

Let's add a "publisher_id" field. `Book` has a `publisher` property, which we'll use for this field.

```kotlin
value<Int>("publisher_id") {
  getValue { book -> book.publisher.publisherId }
  displayLabel = "Publisher"
  modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
}
```

In our database, our publishers are identified by an auto-generated integer key. Right now, if a client wants to display
a human-readable name for the publisher, it has to make another call to the `publishers` resource (no, we haven't written
that one yet). Yuck!  Let's add a `description`:

```diff
  value<Int>("publisher_id") {
    getValue { book -> book.publisher.publisherId }
    displayLabel = "Publisher"
    modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
+ 
+   description { book, publisherId -> book.publisher.commonName }
  }
```

This will use the publisher's commonly-used name as the description. However, some publishers have a longer official
name, so let's use that for the "long_description". Not all publishers have a longer official name, so this value
might contain `null`.

```diff
  value<Int>("publisher_id") {
    getValue { book -> book.publisher.publisherId }
    displayLabel = "Publisher"
    modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
    
    description { book, publisherId -> book.publisher.commonName }
+   longDescription { book, publisherId -> book.publisher.fullName }
  }
```

```json
"publisher_id": {
  "value": 5,
  
  "description": "Workman",
  "long_description": "Workman Publishing",
  
  "api_type": "modifiable",
  "display_label": "Publisher"
},
```

To show that the long_description is nullable, let's fetch *Catch-22* (OCLC number `35231812`):

```bash
curl -H "Authorization: Bearer {your OAuth token here}" http://localhost:8080/books/35231812
```

```json
"publisher_id": {
  "value": 1,
  "description": "Simon & Schuster",
  "long_description": null,
  "api_type": "modifiable",
  "display_label": "Publisher"
},
```

> It probably makes more sense to have the full name just match the common name if they're not different, 
> but we really wanted to show off nullable descriptions!
{: .callout-demo }

### Nullable Values

While we're using the more comprehensive OCLC Control Number as our main identifier, there is another unique identifier
for books that is more familiar to many: the ISBN (International Standard Book Number).

However, there's a complication - not all titles have ISBNs! Only books that have been published since the ISBN was 
created have been given one.

That means that our `isbn` field must be nullable.  We'll use `nullableValue` to describe this field:

```kotlin
nullableValue(Book::isbn) {
  isSystem = true
  displayLabel = "ISBN"
  doc = "International Standard Book Number"
}
```

If you specify `description` or `longDescription` for a `nullableValue`, they will only be invoked if the value is not 
`null` and will be set to `null` if it is. `modifiable`, if specified, will be invoked no matter what the value is.

> The separation of `value` and `nullableValue` is due to limitations in Kotlin's type system. It's worth it, though,
> to provide you with an API that catches issues at compile time instead of runtime!
{: .callout-workaround }

> There's also a variant of `nullableValue` for fields that can't use the property reference syntax. This is equivalent
> to the code above:
> 
> ```kotlin
> nullableValue<String>("isbn") {
>   getValue { book -> book.isbn } 
>   isSystem = true
>   displayLabel = "ISBN"
>   doc = "International Standard Book Number"
> }
> ```
> 
> Notice that the type parameter is `<String>`, not `<String?>`. If we could make this work with `<String?>`, we wouldn't
> need separate functions for nullable and non-nullable values!
{: .callout-protip }

### Derived Fields

Some response fields can't be modified because they are derived from some other value. For example, you can't modify
a student's GPA; it's derived from the grades they've gotten in their classes. The only way to change a GPA is to add or
change a grade.

In our case, `Book` has a derived property called `availableCopies`, which represents the number of copies that are currently
on shelves, ready to be checked out.

```kotlin
value(Book::availableCopies) {
  isDerived = true
  displayLabel = "Copies available for checkout"
}
```

Setting `isDerived = true` will change the `api_type` to `"derived"`:

```json
"available_copies": {
  "value": 1,
  "api_type": "derived",
  "display_label": "Copies available for checkout"
},
```

## Arrays of Values

In order to create an array of simple values, use `valueArray`:

```kotlin
valueArray(Book::subtitles) {
  displayLabel = "Subtitles"
  doc = "The book's subtitles"
  modifiable { libraryUser, book, subtitles -> libraryUser.canModifyBooks }
}
```

If you look closely, you'll notice that `modifiable` gets the full array of values passed as its third parameter. Other
than that, it works just like it does for a normal value.

Here's what the JSON looks like:

```json
"subtitles": {
  "values": [
    {
      "value": "The Nice and Accurate Prophecies of Agnes Nutter, Witch"
    } 
  ],
  "api_type": "modifiable",
  "display_label": "Subtitles"
},
```

Hey look! An array of one value! That's not very exciting. Let's look at *Robinson Crusoe* and its three(!!!) subtitles (OCLC `71126670`) instead:

```bash
curl -H "Authorization: Bearer {your OAuth token here}" http://localhost:8080/books/71126670
```

```json
"subtitles": {
  "values": [
    {
      "value": "Who lived Eight and Twenty Years, all alone in an un-inhabited Island on the Coast of America, near the Mouth of the Great River of Oroonoque"
    },
    {
      "value": "Having been cast on Shore by Shipwreck, wherein all the Men perished but himself"
    },
    {
      "value": "With An Account how he was at last as strangely deliver'd by Pyrates"
    }
  ],
  "api_type": "modifiable",
  "display_label": "Subtitles"
},
```

That's more like it!

You'll notice that the subtitles are contained in an array in the `values` field. They're wrapped in objects, with a 
nested `value` field. This is the most basic form of a value array in the UAPI specification.

Let's make the array values be a bit more interesting. There's not much to enhance with the subtitles, since they're just 
a boring list of strings, so let's move on to the list of genres.

`Book` has a property named `authors`, which contains a list of `Author` objects. The value of each entry will be
the Author's `authorId`, while the description will be the Author's `name`:

```kotlin
valueArray<Int>("author_ids") {
  getValues { book -> book.authors.map { it.authorId } }
  displayLabel = "Author(s)"
  modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
  
  description { book, item, index -> book.authors[index].name }
}
```

Because we're not using Property References, we have to tell the runtime what the value type is (`<Int>`), and we 
have to tell it how to get the list of values (`getValues`). Because we specified that the value is of type `<Int>`,
`getValues` must return a type of `Collection<Int?>`. This means that you can mix null and non-null values in the array.

Back to *Good Omens*, here's what the `authors` JSON will look like:

```json
"author_ids": {
  "values": [
    {
      "value": 8,
      "description": "Terry Pratchett"
    },
    {
      "value": 9,
      "description": "Neil Gaiman"
    }
  ],
  "api_type": "modifiable",
  "display_label": "Author(s)"
},
```

### Simplified Value Array Syntax

The `author_ids` example shows a common use case for arrays: often, our Model class will contain an list of objects which
contain the values, descriptions, and everything else we need to generate the contents of an array. Since this entire Runtime
is basically just some sugar to make UAPI easy, we've provided some sugar to make this use case much simpler: `mappedValueArray`.

Instead of having one function that gets the values in the array, `mappedValueArray` takes two functions: one that gets
the array of objects, and one that extracts the value from the object. In addition, the `description` and `longDescription`
functions get several new variations which include the item value.

The `author_ids` field can now look like this:

```kotlin
mappedValueArray<Int, Author>("author_ids") {
  getArray { book -> book.authors }
  getValue { author -> author.authorId }
  description { author -> author.name }
  displayLabel = "Author(s)"
  modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
}
```

This can be simplified using Property References:

```kotlin
mappedValueArray<Int, Author>("author_ids") {
  getArray(Book::authors)
  getValue(Author::authorId)
  description(Author::name)
  displayLabel = "Author(s)"
  modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
}
```

Or even:

```kotlin
mappedValueArray("author_ids", Book::authors, Author::authorId) {
  description(Author::name)
  displayLabel = "Author(s)"
  modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
}
```

And, if your array name matches the name of your array property, like it does for `genres`:

```kotlin
mappedValueArray(Book::genres, Genre::code) {
  description(Genre::name)
  displayLabel = "Genres(s)"
  modifiable { libraryUser, book, value -> libraryUser.canModifyBooks }
}
```

## Complex Objects

> Complex Object responses are still being added to the UAPI Specification and are slated for a future pre-release version of the Runtime.
{: .callout-in-progress }

## Arrays of Objects

> Complex Object responses are still being added to the UAPI Specification and are slated for a future pre-release version of the Runtime.
{: .callout-in-progress }

## Final Result

Here's our final implementation of `responseFields`:

```kotlin

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
    getValue { book -> book.publisher.publisherId }
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
  mappedValueArray(Book::genres, valueProp = Genre::code) {
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
```

And here is the output for *Good Omens*:

```json
{
    "basic": {
        "oclc": {
            "value": 26811595,
            "api_type": "read-only",
            "key": true,
            "display_label": "OCLC Control Number"
        },
        "title": {
            "value": "Good Omens",
            "api_type": "modifiable",
            "display_label": "Title"
        },
        "publisher_id": {
            "value": 9,
            "description": "Workman",
            "long_description": "Workman Publishing",
            "api_type": "modifiable",
            "display_label": "Publisher"
        },
        "available_copies": {
            "value": 1,
            "api_type": "derived",
            "display_label": "Available Copies"
        },
        "isbn": {
            "value": "0-575-04800-X",
            "api_type": "system",
            "display_label": "ISBN"
        },
        "subtitles": {
            "values": [
                {
                    "value": "The Nice and Accurate Prophecies of Agnes Nutter, Witch"
                }
            ],
            "api_type": "modifiable",
            "display_label": "Subtitles"
        },
        "author_ids": {
            "values": [
                {
                    "value": 8,
                    "description": "Terry Pratchett"
                },
                {
                    "value": 9,
                    "description": "Neil Gaiman"
                }
            ],
            "api_type": "modifiable",
            "display_label": "Author(s)"
        },
        "genres": {
            "values": [
                {
                    "value": "FAN",
                    "description": "Fantasy"
                },
                {
                    "value": "FI",
                    "description": "Fiction"
                },
                {
                    "value": "LOL",
                    "description": "Humor"
                }
            ],
            "api_type": "modifiable",
            "display_label": "Genre(s)"
        },
        "published_year": {
            "value": 1990,
            "api_type": "modifiable",
            "display_label": "Publication Year"
        },
        "restricted": {
            "value": false,
            "api_type": "modifiable",
            "display_label": "Is Restricted"
        },
        "links": {},
        "metadata": {
            "validation_response": {
                "code": 200,
                "message": "OK"
            }
        }
    },
    "links": {},
    "metadata": {
        "validation_response": {
            "code": 200,
            "message": "OK"
        },
        "field_sets_returned": [
            "basic"
        ],
        "field_sets_available": [
            "basic"
        ],
        "field_sets_default": [
            "basic"
        ],
        "contexts_available": {}
    }
}
```

# Formal Response Field Documentation

TODO

## How we derive `api_type`

TODO

# You Made It!

That was a long haul! Here, have some cake:

![The Cake Is A Lie!](./img/portal-cake.jpg)

Now that we've covered the hard part that is response generation, we get to move on to something much simpler - Listing Resources.

