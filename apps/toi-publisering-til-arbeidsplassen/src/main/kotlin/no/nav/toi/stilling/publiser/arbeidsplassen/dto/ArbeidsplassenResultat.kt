package no.nav.toi.stilling.publiser.arbeidsplassen.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsplassenResultat(
    val providerId: Int,
    val status: String,
    val message: String?,
    val md5: String,
    val items: Int,
    val created: String,
    val updated: String
)
