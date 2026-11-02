package com.cinestream.tv

import java.io.IOException
import org.jsoup.HttpStatusException

enum class SearchFailureKind { UNRECOGNIZED, TEMPORARY, ACCESS, INPUT_REQUIRED }
internal class SearchResponseException(val kind: SearchFailureKind) : IllegalArgumentException(kind.name)
internal fun searchFailureKind(error: Exception): SearchFailureKind = when (error) {
    is SearchResponseException -> error.kind
    is HttpStatusException -> when { error.statusCode in listOf(401, 403, 429) -> SearchFailureKind.ACCESS; error.statusCode == 408 || error.statusCode >= 500 -> SearchFailureKind.TEMPORARY; else -> SearchFailureKind.UNRECOGNIZED }
    is IOException -> SearchFailureKind.TEMPORARY
    else -> SearchFailureKind.UNRECOGNIZED
}
internal fun searchFailureMessage(kind: SearchFailureKind): String = when (kind) {
    SearchFailureKind.INPUT_REQUIRED -> "Enter a title to search Cineby."
    SearchFailureKind.ACCESS -> "Provider access is restricted. Try again later."
    SearchFailureKind.TEMPORARY -> "Provider did not respond. Retry in a moment."
    SearchFailureKind.UNRECOGNIZED -> "Provider returned an unexpected search page. Retry."
}
