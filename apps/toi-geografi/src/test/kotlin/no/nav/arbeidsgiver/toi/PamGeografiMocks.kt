package no.nav.arbeidsgiver.toi

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

fun WireMockServer.stubPostData() {
    stubFor(
        get(urlEqualTo("/rest/postdata"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """
                                [
	{
		"postkode": "0001",
		"by": "OSLO",
		"kommune": {
			"kommunenummer": "0301",
			"navn": "OSLO",
			"fylkesnummer": "03",
			"korrigertNavn": "Oslo"
		},
		"fylke": {
			"fylkesnummer": "03",
			"navn": "OSLO",
			"korrigertNavn": "Oslo"
		},
		"korrigertNavnBy": "Oslo"
	},
	{
		"postkode": "0123",
		"by": "OSLO",
		"kommune": {
			"kommunenummer": "0301",
			"navn": "OSLO",
			"fylkesnummer": "03",
			"korrigertNavn": "Oslo"
		},
		"fylke": {
			"fylkesnummer": "03",
			"navn": "OSLO",
			"korrigertNavn": "Oslo"
		},
		"korrigertNavnBy": "Oslo"
	},
	{
		"postkode": "0277",
		"by": "OSLO",
		"kommune": {
			"kommunenummer": "0301",
			"navn": "OSLO",
			"fylkesnummer": "03",
			"korrigertNavn": "Oslo"
		},
		"fylke": {
			"fylkesnummer": "03",
			"navn": "OSLO",
			"korrigertNavn": "Oslo"
		},
		"korrigertNavnBy": "Oslo"
	},
	{
		"postkode": "0579",
		"by": "OSLO",
		"kommune": {
			"kommunenummer": "0301",
			"navn": "OSLO",
			"fylkesnummer": "03",
			"korrigertNavn": "Oslo"
		},
		"fylke": {
			"fylkesnummer": "03",
			"navn": "OSLO",
			"korrigertNavn": "Oslo"
		},
		"korrigertNavnBy": "Oslo"
	},
	{
		"postkode": "0875",
		"by": "OSLO",
		"kommune": {
			"kommunenummer": "0301",
			"navn": "OSLO",
			"fylkesnummer": "03",
			"korrigertNavn": "Oslo"
		},
		"fylke": {
			"fylkesnummer": "03",
			"navn": "OSLO",
			"korrigertNavn": "Oslo"
		},
		"korrigertNavnBy": "Oslo"
	},
	{
		"postkode": "1319",
		"by": "BEKKESTUA",
		"kommune": {
			"kommunenummer": "3201",
			"navn": "BÆRUM",
			"fylkesnummer": "32",
			"korrigertNavn": "Bærum"
		},
		"fylke": {
			"fylkesnummer": "32",
			"navn": "AKERSHUS",
			"korrigertNavn": "Akershus"
		},
		"korrigertNavnBy": "Bekkestua"
	},
	{
		"postkode": "1487",
		"by": "HAKADAL",
		"kommune": {
			"kommunenummer": "3232",
			"navn": "NITTEDAL",
			"fylkesnummer": "32",
			"korrigertNavn": "Nittedal"
		},
		"fylke": {
			"fylkesnummer": "32",
			"navn": "AKERSHUS",
			"korrigertNavn": "Akershus"
		},
		"korrigertNavnBy": "Hakadal"
	},
	{
		"postkode": "1683",
		"by": "VESTERØY",
		"kommune": {
			"kommunenummer": "3110",
			"navn": "HVALER",
			"fylkesnummer": "31",
			"korrigertNavn": "Hvaler"
		},
		"fylke": {
			"fylkesnummer": "31",
			"navn": "ØSTFOLD",
			"korrigertNavn": "Østfold"
		},
		"korrigertNavnBy": "Vesterøy"
	},
	{
		"postkode": "1878",
		"by": "HÆRLAND",
		"kommune": {
			"kommunenummer": "3118",
			"navn": "INDRE ØSTFOLD",
			"fylkesnummer": "31",
			"korrigertNavn": "Indre Østfold"
		},
		"fylke": {
			"fylkesnummer": "31",
			"navn": "ØSTFOLD",
			"korrigertNavn": "Østfold"
		},
		"korrigertNavnBy": "Hærland"
	},
	{
		"postkode": "2208",
		"by": "KONGSVINGER",
		"kommune": {
			"kommunenummer": "3401",
			"navn": "KONGSVINGER",
			"fylkesnummer": "34",
			"korrigertNavn": "Kongsvinger"
		},
		"fylke": {
			"fylkesnummer": "34",
			"navn": "INNLANDET",
			"korrigertNavn": "Innlandet"
		},
		"korrigertNavnBy": "Kongsvinger"
	},
	{
		"postkode": "2478",
		"by": "HANESTAD",
		"kommune": {
			"kommunenummer": "3424",
			"navn": "RENDALEN",
			"fylkesnummer": "34",
			"korrigertNavn": "Rendalen"
		},
		"fylke": {
			"fylkesnummer": "34",
			"navn": "INNLANDET",
			"korrigertNavn": "Innlandet"
		},
		"korrigertNavnBy": "Hanestad"
	},
	{
		"postkode": "2804",
		"by": "GJØVIK",
		"kommune": {
			"kommunenummer": "3407",
			"navn": "GJØVIK",
			"fylkesnummer": "34",
			"korrigertNavn": "Gjøvik"
		},
		"fylke": {
			"fylkesnummer": "34",
			"navn": "INNLANDET",
			"korrigertNavn": "Innlandet"
		},
		"korrigertNavnBy": "Gjøvik"
	},
	{
		"postkode": "3029",
		"by": "DRAMMEN",
		"kommune": {
			"kommunenummer": "3301",
			"navn": "DRAMMEN",
			"fylkesnummer": "33",
			"korrigertNavn": "Drammen"
		},
		"fylke": {
			"fylkesnummer": "33",
			"navn": "BUSKERUD",
			"korrigertNavn": "Buskerud"
		},
		"korrigertNavnBy": "Drammen"
	},
	{
		"postkode": "3204",
		"by": "SANDEFJORD",
		"kommune": {
			"kommunenummer": "3907",
			"navn": "SANDEFJORD",
			"fylkesnummer": "39",
			"korrigertNavn": "Sandefjord"
		},
		"fylke": {
			"fylkesnummer": "39",
			"navn": "VESTFOLD",
			"korrigertNavn": "Vestfold"
		},
		"korrigertNavnBy": "Sandefjord"
	},
	{
		"postkode": "3409",
		"by": "TRANBY",
		"kommune": {
			"kommunenummer": "3312",
			"navn": "LIER",
			"fylkesnummer": "33",
			"korrigertNavn": "Lier"
		},
		"fylke": {
			"fylkesnummer": "33",
			"navn": "BUSKERUD",
			"korrigertNavn": "Buskerud"
		},
		"korrigertNavnBy": "Tranby"
	},
	{
		"postkode": "3631",
		"by": "RØDBERG",
		"kommune": {
			"kommunenummer": "3338",
			"navn": "NORE OG UVDAL",
			"fylkesnummer": "33",
			"korrigertNavn": "Nore og Uvdal"
		},
		"fylke": {
			"fylkesnummer": "33",
			"navn": "BUSKERUD",
			"korrigertNavn": "Buskerud"
		},
		"korrigertNavnBy": "Rødberg"
	},
	{
		"postkode": "3901",
		"by": "PORSGRUNN",
		"kommune": {
			"kommunenummer": "4001",
			"navn": "PORSGRUNN",
			"fylkesnummer": "40",
			"korrigertNavn": "Porsgrunn"
		},
		"fylke": {
			"fylkesnummer": "40",
			"navn": "TELEMARK",
			"korrigertNavn": "Telemark"
		},
		"korrigertNavnBy": "Porsgrunn"
	},
	{
		"postkode": "4089",
		"by": "HAFRSFJORD",
		"kommune": {
			"kommunenummer": "1103",
			"navn": "STAVANGER",
			"fylkesnummer": "11",
			"korrigertNavn": "Stavanger"
		},
		"fylke": {
			"fylkesnummer": "11",
			"navn": "ROGALAND",
			"korrigertNavn": "Rogaland"
		},
		"korrigertNavnBy": "Hafrsfjord"
	},
	{
		"postkode": "4332",
		"by": "FIGGJO",
		"kommune": {
			"kommunenummer": "1108",
			"navn": "SANDNES",
			"fylkesnummer": "11",
			"korrigertNavn": "Sandnes"
		},
		"fylke": {
			"fylkesnummer": "11",
			"navn": "ROGALAND",
			"korrigertNavn": "Rogaland"
		},
		"korrigertNavnBy": "Figgjo"
	},
	{
		"postkode": "4526",
		"by": "KONSMO",
		"kommune": {
			"kommunenummer": "4225",
			"navn": "LYNGDAL",
			"fylkesnummer": "42",
			"korrigertNavn": "Lyngdal"
		},
		"fylke": {
			"fylkesnummer": "42",
			"navn": "AGDER",
			"korrigertNavn": "Agder"
		},
		"korrigertNavnBy": "Konsmo"
	},
	{
		"postkode": "4701",
		"by": "VENNESLA",
		"kommune": {
			"kommunenummer": "4223",
			"navn": "VENNESLA",
			"fylkesnummer": "42",
			"korrigertNavn": "Vennesla"
		},
		"fylke": {
			"fylkesnummer": "42",
			"navn": "AGDER",
			"korrigertNavn": "Agder"
		},
		"korrigertNavnBy": "Vennesla"
	},
	{
		"postkode": "4952",
		"by": "RISØR",
		"kommune": {
			"kommunenummer": "4201",
			"navn": "RISØR",
			"fylkesnummer": "42",
			"korrigertNavn": "Risør"
		},
		"fylke": {
			"fylkesnummer": "42",
			"navn": "AGDER",
			"korrigertNavn": "Agder"
		},
		"korrigertNavnBy": "Risør"
	},
	{
		"postkode": "5177",
		"by": "BJØRØYHAMN",
		"kommune": {
			"kommunenummer": "4626",
			"navn": "ØYGARDEN",
			"fylkesnummer": "46",
			"korrigertNavn": "Øygarden"
		},
		"fylke": {
			"fylkesnummer": "46",
			"navn": "VESTLAND",
			"korrigertNavn": "Vestland"
		},
		"korrigertNavnBy": "Bjørøyhamn"
	},
	{
		"postkode": "5380",
		"by": "TELAVÅG",
		"kommune": {
			"kommunenummer": "4626",
			"navn": "ØYGARDEN",
			"fylkesnummer": "46",
			"korrigertNavn": "Øygarden"
		},
		"fylke": {
			"fylkesnummer": "46",
			"navn": "VESTLAND",
			"korrigertNavn": "Vestland"
		},
		"korrigertNavnBy": "Telavåg"
	},
	{
		"postkode": "5582",
		"by": "ØLENSVÅG",
		"kommune": {
			"kommunenummer": "1160",
			"navn": "VINDAFJORD",
			"fylkesnummer": "11",
			"korrigertNavn": "Vindafjord"
		},
		"fylke": {
			"fylkesnummer": "11",
			"navn": "ROGALAND",
			"korrigertNavn": "Rogaland"
		},
		"korrigertNavnBy": "Ølensvåg"
	},
	{
		"postkode": "5807",
		"by": "BERGEN",
		"kommune": {
			"kommunenummer": "4601",
			"navn": "BERGEN",
			"fylkesnummer": "46",
			"korrigertNavn": "Bergen"
		},
		"fylke": {
			"fylkesnummer": "46",
			"navn": "VESTLAND",
			"korrigertNavn": "Vestland"
		},
		"korrigertNavnBy": "Bergen"
	},
	{
		"postkode": "5994",
		"by": "VIKANES",
		"kommune": {
			"kommunenummer": "4631",
			"navn": "ALVER",
			"fylkesnummer": "46",
			"korrigertNavn": "Alver"
		},
		"fylke": {
			"fylkesnummer": "46",
			"navn": "VESTLAND",
			"korrigertNavn": "Vestland"
		},
		"korrigertNavnBy": "Vikanes"
	},
	{
		"postkode": "6220",
		"by": "STRAUMGJERDE",
		"kommune": {
			"kommunenummer": "1528",
			"navn": "SYKKYLVEN",
			"fylkesnummer": "15",
			"korrigertNavn": "Sykkylven"
		},
		"fylke": {
			"fylkesnummer": "15",
			"navn": "MØRE OG ROMSDAL",
			"korrigertNavn": "Møre og Romsdal"
		},
		"korrigertNavnBy": "Straumgjerde"
	},
	{
		"postkode": "6502",
		"by": "KRISTIANSUND N",
		"kommune": {
			"kommunenummer": "1505",
			"navn": "KRISTIANSUND",
			"fylkesnummer": "15",
			"korrigertNavn": "Kristiansund"
		},
		"fylke": {
			"fylkesnummer": "15",
			"navn": "MØRE OG ROMSDAL",
			"korrigertNavn": "Møre og Romsdal"
		},
		"korrigertNavnBy": "Kristiansund N"
	},
	{
		"postkode": "6771",
		"by": "NORDFJORDEID",
		"kommune": {
			"kommunenummer": "4649",
			"navn": "STAD",
			"fylkesnummer": "46",
			"korrigertNavn": "Stad"
		},
		"fylke": {
			"fylkesnummer": "46",
			"navn": "VESTLAND",
			"korrigertNavn": "Vestland"
		},
		"korrigertNavnBy": "Nordfjordeid"
	},
	{
		"postkode": "6909",
		"by": "FLORØ",
		"kommune": {
			"kommunenummer": "4602",
			"navn": "KINN",
			"fylkesnummer": "46",
			"korrigertNavn": "Kinn"
		},
		"fylke": {
			"fylkesnummer": "46",
			"navn": "VESTLAND",
			"korrigertNavn": "Vestland"
		},
		"korrigertNavnBy": "Florø"
	},
	{
		"postkode": "7070",
		"by": "BOSBERG",
		"kommune": {
			"kommunenummer": "5001",
			"navn": "TRONDHEIM",
			"fylkesnummer": "50",
			"korrigertNavn": "Trondheim"
		},
		"fylke": {
			"fylkesnummer": "50",
			"navn": "TRØNDELAG",
			"korrigertNavn": "Trøndelag"
		},
		"korrigertNavnBy": "Bosberg"
	},
	{
		"postkode": "7320",
		"by": "FANNREM",
		"kommune": {
			"kommunenummer": "5059",
			"navn": "ORKLAND",
			"fylkesnummer": "50",
			"korrigertNavn": "Orkland"
		},
		"fylke": {
			"fylkesnummer": "50",
			"navn": "TRØNDELAG",
			"korrigertNavn": "Trøndelag"
		},
		"korrigertNavnBy": "Fannrem"
	},
	{
		"postkode": "7467",
		"by": "TRONDHEIM",
		"kommune": {
			"kommunenummer": "5001",
			"navn": "TRONDHEIM",
			"fylkesnummer": "50",
			"korrigertNavn": "Trondheim"
		},
		"fylke": {
			"fylkesnummer": "50",
			"navn": "TRØNDELAG",
			"korrigertNavn": "Trøndelag"
		},
		"korrigertNavnBy": "Trondheim"
	},
	{
		"postkode": "7704",
		"by": "STEINKJER",
		"kommune": {
			"kommunenummer": "5006",
			"navn": "STEINKJER",
			"fylkesnummer": "50",
			"korrigertNavn": "Steinkjer"
		},
		"fylke": {
			"fylkesnummer": "50",
			"navn": "TRØNDELAG",
			"korrigertNavn": "Trøndelag"
		},
		"korrigertNavnBy": "Steinkjer"
	},
	{
		"postkode": "8012",
		"by": "BODØ",
		"kommune": {
			"kommunenummer": "1804",
			"navn": "BODØ",
			"fylkesnummer": "18",
			"korrigertNavn": "Bodø"
		},
		"fylke": {
			"fylkesnummer": "18",
			"navn": "NORDLAND",
			"korrigertNavn": "Nordland"
		},
		"korrigertNavnBy": "Bodø"
	},
	{
		"postkode": "8218",
		"by": "FAUSKE",
		"kommune": {
			"kommunenummer": "1841",
			"navn": "FAUSKE",
			"fylkesnummer": "18",
			"korrigertNavn": "Fauske"
		},
		"fylke": {
			"fylkesnummer": "18",
			"navn": "NORDLAND",
			"korrigertNavn": "Nordland"
		},
		"korrigertNavnBy": "Fauske"
	},
	{
		"postkode": "8517",
		"by": "NARVIK",
		"kommune": {
			"kommunenummer": "1806",
			"navn": "NARVIK",
			"fylkesnummer": "18",
			"korrigertNavn": "Narvik"
		},
		"fylke": {
			"fylkesnummer": "18",
			"navn": "NORDLAND",
			"korrigertNavn": "Nordland"
		},
		"korrigertNavnBy": "Narvik"
	},
	{
		"postkode": "8921",
		"by": "SØMNA",
		"kommune": {
			"kommunenummer": "1812",
			"navn": "SØMNA",
			"fylkesnummer": "18",
			"korrigertNavn": "Sømna"
		},
		"fylke": {
			"fylkesnummer": "18",
			"navn": "NORDLAND",
			"korrigertNavn": "Nordland"
		},
		"korrigertNavnBy": "Sømna"
	},
	{
		"postkode": "9355",
		"by": "SJØVEGAN",
		"kommune": {
			"kommunenummer": "5522",
			"navn": "SALANGEN",
			"fylkesnummer": "55",
			"korrigertNavn": "Salangen"
		},
		"fylke": {
			"fylkesnummer": "55",
			"navn": "TROMS",
			"korrigertNavn": "Troms"
		},
		"korrigertNavnBy": "Sjøvegan"
	},
	{
		"postkode": "9531",
		"by": "KVALFJORD",
		"kommune": {
			"kommunenummer": "5601",
			"navn": "ALTA",
			"fylkesnummer": "56",
			"korrigertNavn": "Alta"
		},
		"fylke": {
			"fylkesnummer": "56",
			"navn": "FINNMARK",
			"korrigertNavn": "Finnmark"
		},
		"korrigertNavnBy": "Kvalfjord"
	}
]
                                """.trimIndent()
                    )
            )
    )
}

fun WireMockServer.stubGeografier() {
    stubFor(
        get(urlEqualTo("/rest/geografier"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """
                            [
                            	{
                            		"geografikode": "NO55",
                            		"navn": "TROMS"
                            	},
                            	{
                            		"geografikode": "NO56",
                            		"navn": "FINNMARK"
                            	},
                            	{
                            		"geografikode": "NO18.1859",
                            		"navn": "FLAKSTAD"
                            	},
                            	{
                            		"geografikode": "NO15.1554",
                            		"navn": "AVERØY"
                            	},
                            	{
                            		"geografikode": "NO15.1557",
                            		"navn": "GJEMNES"
                            	},
                            	{
                            		"geografikode": "NO18.1856",
                            		"navn": "RØST"
                            	},
                            	{
                            		"geografikode": "NO18.1857",
                            		"navn": "VÆRØY"
                            	},
                            	{
                            		"geografikode": "NO18.1853",
                            		"navn": "EVENES"
                            	},
                            	{
                            		"geografikode": "NO18.1851",
                            		"navn": "LØDINGEN"
                            	},
                            	{
                            		"geografikode": "NO46",
                            		"navn": "VESTLAND"
                            	},
                            	{
                            		"geografikode": "NO15.1547",
                            		"navn": "AUKRA"
                            	},
                            	{
                            		"geografikode": "NO18.1860",
                            		"navn": "VESTVÅGØY"
                            	},
                            	{
                            		"geografikode": "NO18.1867",
                            		"navn": "BØ (NORDLAND)"
                            	},
                            	{
                            		"geografikode": "NO50",
                            		"navn": "TRØNDELAG"
                            	},
                            	{
                            		"geografikode": "NO18.1868",
                            		"navn": "ØKSNES"
                            	},
                            	{
                            		"geografikode": "NO18.1865",
                            		"navn": "VÅGAN"
                            	},
                            	{
                            		"geografikode": "NO18.1866",
                            		"navn": "HADSEL"
                            	},
                            	{
                            		"geografikode": "NO33.3328",
                            		"navn": "ÅL"
                            	},
                            	{
                            		"geografikode": "NO33.3326",
                            		"navn": "HEMSEDAL"
                            	},
                            	{
                            		"geografikode": "NO15.1539",
                            		"navn": "RAUMA"
                            	},
                            	{
                            		"geografikode": "NO33.3320",
                            		"navn": "FLÅ"
                            	},
                            	{
                            		"geografikode": "NO18.1870",
                            		"navn": "SORTLAND"
                            	},
                            	{
                            		"geografikode": "NO18.1871",
                            		"navn": "ANDØY"
                            	},
                            	{
                            		"geografikode": "NO15.1531",
                            		"navn": "SULA"
                            	},
                            	{
                            		"geografikode": "NO33.3324",
                            		"navn": "GOL"
                            	},
                            	{
                            		"geografikode": "NO15.1532",
                            		"navn": "GISKE"
                            	},
                            	{
                            		"geografikode": "NO33.3322",
                            		"navn": "NESBYEN"
                            	},
                            	{
                            		"geografikode": "NO15.1535",
                            		"navn": "VESTNES"
                            	},
                            	{
                            		"geografikode": "NO18.1874",
                            		"navn": "MOSKENES"
                            	},
                            	{
                            		"geografikode": "NO18.1875",
                            		"navn": "HAMARØY"
                            	},
                            	{
                            		"geografikode": "NO15.1525",
                            		"navn": "STRANDA"
                            	},
                            	{
                            		"geografikode": "NO33.3338",
                            		"navn": "NORE OG UVDAL"
                            	},
                            	{
                            		"geografikode": "NO33.3336",
                            		"navn": "ROLLAG"
                            	},
                            	{
                            		"geografikode": "NO15.1528",
                            		"navn": "SYKKYLVEN"
                            	},
                            	{
                            		"geografikode": "NO33.3330",
                            		"navn": "HOL"
                            	},
                            	{
                            		"geografikode": "NO15.1520",
                            		"navn": "ØRSTA"
                            	},
                            	{
                            		"geografikode": "NO33.3334",
                            		"navn": "FLESBERG"
                            	},
                            	{
                            		"geografikode": "NO33.3332",
                            		"navn": "SIGDAL"
                            	},
                            	{
                            		"geografikode": "NO99.2131",
                            		"navn": "HOPEN"
                            	},
                            	{
                            		"geografikode": "NO18.1818",
                            		"navn": "HERØY (NORDLAND)"
                            	},
                            	{
                            		"geografikode": "NO18.1816",
                            		"navn": "VEVELSTAD"
                            	},
                            	{
                            		"geografikode": "NO99",
                            		"navn": "ØVRIGE OMRÅDER"
                            	},
                            	{
                            		"geografikode": "NO18.1815",
                            		"navn": "VEGA"
                            	},
                            	{
                            		"geografikode": "NO33.3305",
                            		"navn": "RINGERIKE"
                            	},
                            	{
                            		"geografikode": "NO33.3303",
                            		"navn": "KONGSBERG"
                            	},
                            	{
                            		"geografikode": "NO33.3301",
                            		"navn": "DRAMMEN"
                            	},
                            	{
                            		"geografikode": "NO18.1812",
                            		"navn": "SØMNA"
                            	},
                            	{
                            		"geografikode": "NO18.1813",
                            		"navn": "BRØNNØY"
                            	},
                            	{
                            		"geografikode": "NO18.1811",
                            		"navn": "BINDAL"
                            	},
                            	{
                            		"geografikode": "NO18.1827",
                            		"navn": "DØNNA"
                            	},
                            	{
                            		"geografikode": "NO18.1828",
                            		"navn": "NESNA"
                            	},
                            	{
                            		"geografikode": "NO18.1825",
                            		"navn": "GRANE"
                            	},
                            	{
                            		"geografikode": "NO18.1826",
                            		"navn": "HATTFJELLDAL"
                            	},
                            	{
                            		"geografikode": "NO33.3316",
                            		"navn": "MODUM"
                            	},
                            	{
                            		"geografikode": "NO33.3314",
                            		"navn": "ØVRE EIKER"
                            	},
                            	{
                            		"geografikode": "NO33.3318",
                            		"navn": "KRØDSHERAD"
                            	},
                            	{
                            		"geografikode": "NO33.3312",
                            		"navn": "LIER"
                            	},
                            	{
                            		"geografikode": "NO33.3310",
                            		"navn": "HOLE"
                            	},
                            	{
                            		"geografikode": "NO18.1824",
                            		"navn": "VEFSN"
                            	},
                            	{
                            		"geografikode": "NO18.1822",
                            		"navn": "LEIRFJORD"
                            	},
                            	{
                            		"geografikode": "NO15.1580",
                            		"navn": "HARAM"
                            	},
                            	{
                            		"geografikode": "NO18.1820",
                            		"navn": "ALSTAHAUG"
                            	},
                            	{
                            		"geografikode": "NO18.1838",
                            		"navn": "GILDESKÅL"
                            	},
                            	{
                            		"geografikode": "NO18.1839",
                            		"navn": "BEIARN"
                            	},
                            	{
                            		"geografikode": "NO18.1836",
                            		"navn": "RØDØY"
                            	},
                            	{
                            		"geografikode": "NO18.1837",
                            		"navn": "MELØY"
                            	},
                            	{
                            		"geografikode": "NO32.3201",
                            		"navn": "BÆRUM"
                            	},
                            	{
                            		"geografikode": "NO32.3209",
                            		"navn": "ULLENSAKER"
                            	},
                            	{
                            		"geografikode": "NO15.1573",
                            		"navn": "SMØLA"
                            	},
                            	{
                            		"geografikode": "NO32.3207",
                            		"navn": "NORDRE FOLLO"
                            	},
                            	{
                            		"geografikode": "NO32.3205",
                            		"navn": "LILLESTRØM"
                            	},
                            	{
                            		"geografikode": "NO15.1576",
                            		"navn": "AURE"
                            	},
                            	{
                            		"geografikode": "NO15.1577",
                            		"navn": "VOLDA"
                            	},
                            	{
                            		"geografikode": "NO32.3203",
                            		"navn": "ASKER"
                            	},
                            	{
                            		"geografikode": "NO15.1578",
                            		"navn": "FJORD"
                            	},
                            	{
                            		"geografikode": "NO15.1579",
                            		"navn": "HUSTADVIKA"
                            	},
                            	{
                            		"geografikode": "NO18.1834",
                            		"navn": "LURØY"
                            	},
                            	{
                            		"geografikode": "NO18.1835",
                            		"navn": "TRÆNA"
                            	},
                            	{
                            		"geografikode": "NO18.1832",
                            		"navn": "HEMNES"
                            	},
                            	{
                            		"geografikode": "NO18.1833",
                            		"navn": "RANA"
                            	},
                            	{
                            		"geografikode": "NO18.1848",
                            		"navn": "STEIGEN"
                            	},
                            	{
                            		"geografikode": "NO15.1563",
                            		"navn": "SUNNDAL"
                            	},
                            	{
                            		"geografikode": "NO15.1566",
                            		"navn": "SURNADAL"
                            	},
                            	{
                            		"geografikode": "NO18.1845",
                            		"navn": "SØRFOLD"
                            	},
                            	{
                            		"geografikode": "NO18.1841",
                            		"navn": "FAUSKE"
                            	},
                            	{
                            		"geografikode": "NO15.1560",
                            		"navn": "TINGVOLL"
                            	},
                            	{
                            		"geografikode": "NO18.1840",
                            		"navn": "SALTDAL"
                            	},
                            	{
                            		"geografikode": "NO18.1806",
                            		"navn": "NARVIK"
                            	},
                            	{
                            		"geografikode": "NO18.1804",
                            		"navn": "BODØ"
                            	},
                            	{
                            		"geografikode": "NO39.3903",
                            		"navn": "HOLMESTRAND"
                            	},
                            	{
                            		"geografikode": "NO39.3905",
                            		"navn": "TØNSBERG"
                            	},
                            	{
                            		"geografikode": "NO39.3907",
                            		"navn": "SANDEFJORD"
                            	},
                            	{
                            		"geografikode": "NO39.3909",
                            		"navn": "LARVIK"
                            	},
                            	{
                            		"geografikode": "NO39.3901",
                            		"navn": "HORTEN"
                            	},
                            	{
                            		"geografikode": "NO39.3911",
                            		"navn": "FÆRDER"
                            	},
                            	{
                            		"geografikode": "NO",
                            		"navn": "NORGE"
                            	},
                            	{
                            		"geografikode": "NO34.3403",
                            		"navn": "HAMAR"
                            	},
                            	{
                            		"geografikode": "NO34.3405",
                            		"navn": "LILLEHAMMER"
                            	},
                            	{
                            		"geografikode": "NO34.3407",
                            		"navn": "GJØVIK"
                            	},
                            	{
                            		"geografikode": "NO22.2211",
                            		"navn": "JAN MAYEN"
                            	},
                            	{
                            		"geografikode": "NO34.3401",
                            		"navn": "KONGSVINGER"
                            	},
                            	{
                            		"geografikode": "NO34.3414",
                            		"navn": "NORD-ODAL"
                            	},
                            	{
                            		"geografikode": "NO34.3415",
                            		"navn": "SØR-ODAL"
                            	},
                            	{
                            		"geografikode": "NO34.3416",
                            		"navn": "EIDSKOG"
                            	},
                            	{
                            		"geografikode": "NO55.5503",
                            		"navn": "HARSTAD"
                            	},
                            	{
                            		"geografikode": "NO34.3417",
                            		"navn": "GRUE"
                            	},
                            	{
                            		"geografikode": "NO34.3418",
                            		"navn": "ÅSNES"
                            	},
                            	{
                            		"geografikode": "NO34.3419",
                            		"navn": "VÅLER (INNLANDET)"
                            	},
                            	{
                            		"geografikode": "NO40.4012",
                            		"navn": "BAMBLE"
                            	},
                            	{
                            		"geografikode": "NO40.4010",
                            		"navn": "SILJAN"
                            	},
                            	{
                            		"geografikode": "NO40.4016",
                            		"navn": "DRANGEDAL"
                            	},
                            	{
                            		"geografikode": "NO40.4014",
                            		"navn": "KRAGERØ"
                            	},
                            	{
                            		"geografikode": "NO40.4018",
                            		"navn": "NOME"
                            	},
                            	{
                            		"geografikode": "NO55.5501",
                            		"navn": "TROMSØ"
                            	},
                            	{
                            		"geografikode": "NO34.3411",
                            		"navn": "RINGSAKER"
                            	},
                            	{
                            		"geografikode": "NO34.3412",
                            		"navn": "LØTEN"
                            	},
                            	{
                            		"geografikode": "NO34.3413",
                            		"navn": "STANGE"
                            	},
                            	{
                            		"geografikode": "NO34.3425",
                            		"navn": "ENGERDAL"
                            	},
                            	{
                            		"geografikode": "NO55.5516",
                            		"navn": "GRATANGEN"
                            	},
                            	{
                            		"geografikode": "NO34.3426",
                            		"navn": "TOLGA"
                            	},
                            	{
                            		"geografikode": "NO34.3427",
                            		"navn": "TYNSET"
                            	},
                            	{
                            		"geografikode": "NO55.5514",
                            		"navn": "IBESTAD"
                            	},
                            	{
                            		"geografikode": "NO34.3428",
                            		"navn": "ALVDAL"
                            	},
                            	{
                            		"geografikode": "NO34.3429",
                            		"navn": "FOLLDAL"
                            	},
                            	{
                            		"geografikode": "NO55.5518",
                            		"navn": "LAVANGEN"
                            	},
                            	{
                            		"geografikode": "NO40.4001",
                            		"navn": "PORSGRUNN"
                            	},
                            	{
                            		"geografikode": "NO40.4005",
                            		"navn": "NOTODDEN"
                            	},
                            	{
                            		"geografikode": "NO40.4003",
                            		"navn": "SKIEN"
                            	},
                            	{
                            		"geografikode": "NO34.3420",
                            		"navn": "ELVERUM"
                            	},
                            	{
                            		"geografikode": "NO34.3421",
                            		"navn": "TRYSIL"
                            	},
                            	{
                            		"geografikode": "NO55.5512",
                            		"navn": "TJELDSUND"
                            	},
                            	{
                            		"geografikode": "NO34.3422",
                            		"navn": "ÅMOT"
                            	},
                            	{
                            		"geografikode": "NO34.3423",
                            		"navn": "STOR-ELVDAL"
                            	},
                            	{
                            		"geografikode": "NO55.5510",
                            		"navn": "KVÆFJORD"
                            	},
                            	{
                            		"geografikode": "NO34.3424",
                            		"navn": "RENDALEN"
                            	},
                            	{
                            		"geografikode": "NO50.5052",
                            		"navn": "LEKA"
                            	},
                            	{
                            		"geografikode": "NO50.5053",
                            		"navn": "INDERØY"
                            	},
                            	{
                            		"geografikode": "NO55.5528",
                            		"navn": "DYRØY"
                            	},
                            	{
                            		"geografikode": "NO55.5526",
                            		"navn": "SØRREISA"
                            	},
                            	{
                            		"geografikode": "NO50.5056",
                            		"navn": "HITRA"
                            	},
                            	{
                            		"geografikode": "NO50.5057",
                            		"navn": "ØRLAND"
                            	},
                            	{
                            		"geografikode": "NO50.5054",
                            		"navn": "INDRE FOSEN"
                            	},
                            	{
                            		"geografikode": "NO50.5055",
                            		"navn": "HEIM"
                            	},
                            	{
                            		"geografikode": "NO55.5520",
                            		"navn": "BARDU"
                            	},
                            	{
                            		"geografikode": "NO50.5058",
                            		"navn": "ÅFJORD"
                            	},
                            	{
                            		"geografikode": "NO50.5059",
                            		"navn": "ORKLAND"
                            	},
                            	{
                            		"geografikode": "NO55.5524",
                            		"navn": "MÅLSELV"
                            	},
                            	{
                            		"geografikode": "NO55.5522",
                            		"navn": "SALANGEN"
                            	},
                            	{
                            		"geografikode": "NO55.5538",
                            		"navn": "STORFJORD"
                            	},
                            	{
                            		"geografikode": "NO50.5061",
                            		"navn": "RINDAL"
                            	},
                            	{
                            		"geografikode": "NO55.5536",
                            		"navn": "LYNGEN"
                            	},
                            	{
                            		"geografikode": "NO11.1160",
                            		"navn": "VINDAFJORD"
                            	},
                            	{
                            		"geografikode": "NO50.5060",
                            		"navn": "NÆRØYSUND"
                            	},
                            	{
                            		"geografikode": "NO55.5530",
                            		"navn": "SENJA"
                            	},
                            	{
                            		"geografikode": "NO31.3120",
                            		"navn": "RAKKESTAD"
                            	},
                            	{
                            		"geografikode": "NO55.5534",
                            		"navn": "KARLSØY"
                            	},
                            	{
                            		"geografikode": "NO31.3124",
                            		"navn": "AREMARK"
                            	},
                            	{
                            		"geografikode": "NO55.5532",
                            		"navn": "BALSFJORD"
                            	},
                            	{
                            		"geografikode": "NO31.3122",
                            		"navn": "MARKER"
                            	},
                            	{
                            		"geografikode": "NO31.3116",
                            		"navn": "SKIPTVET"
                            	},
                            	{
                            		"geografikode": "NO31.3114",
                            		"navn": "VÅLER (ØSTFOLD)"
                            	},
                            	{
                            		"geografikode": "NO31.3118",
                            		"navn": "INDRE ØSTFOLD"
                            	},
                            	{
                            		"geografikode": "NO56.5634",
                            		"navn": "VARDØ"
                            	},
                            	{
                            		"geografikode": "NO56.5632",
                            		"navn": "BÅTSFJORD"
                            	},
                            	{
                            		"geografikode": "NO56.5630",
                            		"navn": "BERLEVÅG"
                            	},
                            	{
                            		"geografikode": "NO56.5636",
                            		"navn": "UNJARGGA NESSEBY"
                            	},
                            	{
                            		"geografikode": "NO55.5542",
                            		"navn": "SKJERVØY"
                            	},
                            	{
                            		"geografikode": "NO55.5540",
                            		"navn": "GÁIVUOTNA KÅFJORD"
                            	},
                            	{
                            		"geografikode": "NO31.3112",
                            		"navn": "RÅDE"
                            	},
                            	{
                            		"geografikode": "NO55.5546",
                            		"navn": "KVÆNANGEN"
                            	},
                            	{
                            		"geografikode": "NO31.3110",
                            		"navn": "HVALER"
                            	},
                            	{
                            		"geografikode": "NO55.5544",
                            		"navn": "NORDREISA"
                            	},
                            	{
                            		"geografikode": "NO31.3105",
                            		"navn": "SARPSBORG"
                            	},
                            	{
                            		"geografikode": "NO31.3103",
                            		"navn": "MOSS"
                            	},
                            	{
                            		"geografikode": "NO31.3107",
                            		"navn": "FREDRIKSTAD"
                            	},
                            	{
                            		"geografikode": "NO31.3101",
                            		"navn": "HALDEN"
                            	},
                            	{
                            		"geografikode": "NO32.3224",
                            		"navn": "RÆLINGEN"
                            	},
                            	{
                            		"geografikode": "NO11.1133",
                            		"navn": "HJELMELAND"
                            	},
                            	{
                            		"geografikode": "NO56.5612",
                            		"navn": "GUOVDAGEAIDNU KAUTOKEINO"
                            	},
                            	{
                            		"geografikode": "NO11.1134",
                            		"navn": "SULDAL"
                            	},
                            	{
                            		"geografikode": "NO32.3222",
                            		"navn": "LØRENSKOG"
                            	},
                            	{
                            		"geografikode": "NO11.1135",
                            		"navn": "SAUDA"
                            	},
                            	{
                            		"geografikode": "NO56.5610",
                            		"navn": "KARASJOHKA KARASJOK"
                            	},
                            	{
                            		"geografikode": "NO32.3220",
                            		"navn": "ENEBAKK"
                            	},
                            	{
                            		"geografikode": "NO11.1130",
                            		"navn": "STRAND"
                            	},
                            	{
                            		"geografikode": "NO99.2321",
                            		"navn": "SOKKELEN NORD FOR 62 BRGR"
                            	},
                            	{
                            		"geografikode": "NO56.5618",
                            		"navn": "MÅSØY"
                            	},
                            	{
                            		"geografikode": "NO99.2201",
                            		"navn": "NORDSJØEN"
                            	},
                            	{
                            		"geografikode": "NO32.3228",
                            		"navn": "NES (AKERSHUS)"
                            	},
                            	{
                            		"geografikode": "NO56.5616",
                            		"navn": "HASVIK"
                            	},
                            	{
                            		"geografikode": "NO32.3226",
                            		"navn": "AURSKOG-HØLAND"
                            	},
                            	{
                            		"geografikode": "NO56.5614",
                            		"navn": "LOPPA"
                            	},
                            	{
                            		"geografikode": "NO50.5014",
                            		"navn": "FRØYA"
                            	},
                            	{
                            		"geografikode": "NO50.5020",
                            		"navn": "OSEN"
                            	},
                            	{
                            		"geografikode": "NO50.5021",
                            		"navn": "OPPDAL"
                            	},
                            	{
                            		"geografikode": "NO50.5022",
                            		"navn": "RENNEBU"
                            	},
                            	{
                            		"geografikode": "NO32.3212",
                            		"navn": "NESODDEN"
                            	},
                            	{
                            		"geografikode": "NO11.1121",
                            		"navn": "TIME"
                            	},
                            	{
                            		"geografikode": "NO56.5624",
                            		"navn": "LEBESBY"
                            	},
                            	{
                            		"geografikode": "NO11.1122",
                            		"navn": "GJESDAL"
                            	},
                            	{
                            		"geografikode": "NO56.5622",
                            		"navn": "PORSANGER PORSÁNGU PORSANKI"
                            	},
                            	{
                            		"geografikode": "NO11.1124",
                            		"navn": "SOLA"
                            	},
                            	{
                            		"geografikode": "NO56.5620",
                            		"navn": "NORDKAPP"
                            	},
                            	{
                            		"geografikode": "NO11.1120",
                            		"navn": "KLEPP"
                            	},
                            	{
                            		"geografikode": "NO32.3218",
                            		"navn": "ÅS"
                            	},
                            	{
                            		"geografikode": "NO99.2311",
                            		"navn": "SOKKELEN SØR FOR 62 BRGR"
                            	},
                            	{
                            		"geografikode": "NO32.3216",
                            		"navn": "VESTBY"
                            	},
                            	{
                            		"geografikode": "NO56.5628",
                            		"navn": "DEATNU TANA"
                            	},
                            	{
                            		"geografikode": "NO32.3214",
                            		"navn": "FROGN"
                            	},
                            	{
                            		"geografikode": "NO11.1127",
                            		"navn": "RANDABERG"
                            	},
                            	{
                            		"geografikode": "NO56.5626",
                            		"navn": "GAMVIK"
                            	},
                            	{
                            		"geografikode": "NO50.5027",
                            		"navn": "MIDTRE GAULDAL"
                            	},
                            	{
                            		"geografikode": "NO50.5028",
                            		"navn": "MELHUS"
                            	},
                            	{
                            		"geografikode": "NO50.5025",
                            		"navn": "RØROS"
                            	},
                            	{
                            		"geografikode": "NO50.5026",
                            		"navn": "HOLTÅLEN"
                            	},
                            	{
                            		"geografikode": "NO50.5029",
                            		"navn": "SKAUN"
                            	},
                            	{
                            		"geografikode": "NO50.5031",
                            		"navn": "MALVIK"
                            	},
                            	{
                            		"geografikode": "NO50.5034",
                            		"navn": "MERÅKER"
                            	},
                            	{
                            		"geografikode": "NO50.5035",
                            		"navn": "STJØRDAL"
                            	},
                            	{
                            		"geografikode": "NO50.5032",
                            		"navn": "SELBU"
                            	},
                            	{
                            		"geografikode": "NO50.5033",
                            		"navn": "TYDAL"
                            	},
                            	{
                            		"geografikode": "NO46.4651",
                            		"navn": "STRYN"
                            	},
                            	{
                            		"geografikode": "NO03.0301",
                            		"navn": "OSLO"
                            	},
                            	{
                            		"geografikode": "NO46.4650",
                            		"navn": "GLOPPEN"
                            	},
                            	{
                            		"geografikode": "NO32.3242",
                            		"navn": "HURDAL"
                            	},
                            	{
                            		"geografikode": "NO11.1151",
                            		"navn": "UTSIRA"
                            	},
                            	{
                            		"geografikode": "NO32.3240",
                            		"navn": "EIDSVOLL"
                            	},
                            	{
                            		"geografikode": "NO50.5038",
                            		"navn": "VERDAL"
                            	},
                            	{
                            		"geografikode": "NO50.5036",
                            		"navn": "FROSTA"
                            	},
                            	{
                            		"geografikode": "NO50.5037",
                            		"navn": "LEVANGER"
                            	},
                            	{
                            		"geografikode": "NO50.5041",
                            		"navn": "SNÅSA"
                            	},
                            	{
                            		"geografikode": "NO50.5042",
                            		"navn": "LIERNE"
                            	},
                            	{
                            		"geografikode": "NO50.5045",
                            		"navn": "GRONG"
                            	},
                            	{
                            		"geografikode": "NO50.5046",
                            		"navn": "HØYLANDET"
                            	},
                            	{
                            		"geografikode": "NO50.5043",
                            		"navn": "RØYRVIK"
                            	},
                            	{
                            		"geografikode": "NO21.2100",
                            		"navn": "SVALBARD"
                            	},
                            	{
                            		"geografikode": "NO50.5044",
                            		"navn": "NAMSSKOGAN"
                            	},
                            	{
                            		"geografikode": "NO32.3234",
                            		"navn": "LUNNER"
                            	},
                            	{
                            		"geografikode": "NO46.4640",
                            		"navn": "SOGNDAL"
                            	},
                            	{
                            		"geografikode": "NO11.1144",
                            		"navn": "KVITSØY"
                            	},
                            	{
                            		"geografikode": "NO56.5601",
                            		"navn": "ALTA"
                            	},
                            	{
                            		"geografikode": "NO32.3232",
                            		"navn": "NITTEDAL"
                            	},
                            	{
                            		"geografikode": "NO11.1145",
                            		"navn": "BOKN"
                            	},
                            	{
                            		"geografikode": "NO46.4642",
                            		"navn": "LÆRDAL"
                            	},
                            	{
                            		"geografikode": "NO11.1146",
                            		"navn": "TYSVÆR"
                            	},
                            	{
                            		"geografikode": "NO46.4641",
                            		"navn": "AURLAND"
                            	},
                            	{
                            		"geografikode": "NO32.3230",
                            		"navn": "GJERDRUM"
                            	},
                            	{
                            		"geografikode": "NO46.4644",
                            		"navn": "LUSTER"
                            	},
                            	{
                            		"geografikode": "NO46.4643",
                            		"navn": "ÅRDAL"
                            	},
                            	{
                            		"geografikode": "NO46.4646",
                            		"navn": "FJALER"
                            	},
                            	{
                            		"geografikode": "NO46.4645",
                            		"navn": "ASKVOLL"
                            	},
                            	{
                            		"geografikode": "NO46.4648",
                            		"navn": "BREMANGER"
                            	},
                            	{
                            		"geografikode": "NO46.4647",
                            		"navn": "SUNNFJORD"
                            	},
                            	{
                            		"geografikode": "NO46.4649",
                            		"navn": "STAD"
                            	},
                            	{
                            		"geografikode": "NO56.5607",
                            		"navn": "VADSØ"
                            	},
                            	{
                            		"geografikode": "NO32.3238",
                            		"navn": "NANNESTAD"
                            	},
                            	{
                            		"geografikode": "NO56.5605",
                            		"navn": "SØR-VARANGER"
                            	},
                            	{
                            		"geografikode": "NO32.3236",
                            		"navn": "JEVNAKER"
                            	},
                            	{
                            		"geografikode": "NO11.1149",
                            		"navn": "KARMØY"
                            	},
                            	{
                            		"geografikode": "NO56.5603",
                            		"navn": "HAMMERFEST"
                            	},
                            	{
                            		"geografikode": "NO50.5049",
                            		"navn": "FLATANGER"
                            	},
                            	{
                            		"geografikode": "NO50.5047",
                            		"navn": "OVERHALLA"
                            	},
                            	{
                            		"geografikode": "NO34.3436",
                            		"navn": "NORD-FRON"
                            	},
                            	{
                            		"geografikode": "NO34.3437",
                            		"navn": "SEL"
                            	},
                            	{
                            		"geografikode": "NO15",
                            		"navn": "MØRE OG ROMSDAL"
                            	},
                            	{
                            		"geografikode": "NO34.3438",
                            		"navn": "SØR-FRON"
                            	},
                            	{
                            		"geografikode": "NO42.4202",
                            		"navn": "GRIMSTAD"
                            	},
                            	{
                            		"geografikode": "NO34.3439",
                            		"navn": "RINGEBU"
                            	},
                            	{
                            		"geografikode": "NO42.4201",
                            		"navn": "RISØR"
                            	},
                            	{
                            		"geografikode": "NO11",
                            		"navn": "ROGALAND"
                            	},
                            	{
                            		"geografikode": "NO15.1514",
                            		"navn": "SANDE (MØRE OG ROMSDAL)"
                            	},
                            	{
                            		"geografikode": "NO40.4030",
                            		"navn": "NISSEDAL"
                            	},
                            	{
                            		"geografikode": "NO15.1515",
                            		"navn": "HERØY (MØRE OG ROMSDAL)"
                            	},
                            	{
                            		"geografikode": "NO46.4631",
                            		"navn": "ALVER"
                            	},
                            	{
                            		"geografikode": "NO15.1516",
                            		"navn": "ULSTEIN"
                            	},
                            	{
                            		"geografikode": "NO46.4630",
                            		"navn": "OSTERØY"
                            	},
                            	{
                            		"geografikode": "NO15.1517",
                            		"navn": "HAREID"
                            	},
                            	{
                            		"geografikode": "NO46.4633",
                            		"navn": "FEDJE"
                            	},
                            	{
                            		"geografikode": "NO18",
                            		"navn": "NORDLAND"
                            	},
                            	{
                            		"geografikode": "NO40.4034",
                            		"navn": "TOKKE"
                            	},
                            	{
                            		"geografikode": "NO46.4632",
                            		"navn": "AUSTRHEIM"
                            	},
                            	{
                            		"geografikode": "NO46.4635",
                            		"navn": "GULEN"
                            	},
                            	{
                            		"geografikode": "NO40.4032",
                            		"navn": "FYRESDAL"
                            	},
                            	{
                            		"geografikode": "NO46.4634",
                            		"navn": "MASFJORDEN"
                            	},
                            	{
                            		"geografikode": "NO46.4637",
                            		"navn": "HYLLESTAD"
                            	},
                            	{
                            		"geografikode": "NO46.4636",
                            		"navn": "SOLUND"
                            	},
                            	{
                            		"geografikode": "NO46.4639",
                            		"navn": "VIK"
                            	},
                            	{
                            		"geografikode": "NO40.4036",
                            		"navn": "VINJE"
                            	},
                            	{
                            		"geografikode": "NO46.4638",
                            		"navn": "HØYANGER"
                            	},
                            	{
                            		"geografikode": "NO15.1511",
                            		"navn": "VANYLVEN"
                            	},
                            	{
                            		"geografikode": "NO99.2121",
                            		"navn": "BJØRNØYA"
                            	},
                            	{
                            		"geografikode": "NO42.4207",
                            		"navn": "FLEKKEFJORD"
                            	},
                            	{
                            		"geografikode": "NO34.3430",
                            		"navn": "OS (INNLANDET)"
                            	},
                            	{
                            		"geografikode": "NO34.3431",
                            		"navn": "DOVRE"
                            	},
                            	{
                            		"geografikode": "NO34.3432",
                            		"navn": "LESJA"
                            	},
                            	{
                            		"geografikode": "NO42.4204",
                            		"navn": "KRISTIANSAND"
                            	},
                            	{
                            		"geografikode": "NO34.3433",
                            		"navn": "SKJÅK"
                            	},
                            	{
                            		"geografikode": "NO42.4203",
                            		"navn": "ARENDAL"
                            	},
                            	{
                            		"geografikode": "NO34.3434",
                            		"navn": "LOM"
                            	},
                            	{
                            		"geografikode": "NO42.4206",
                            		"navn": "FARSUND"
                            	},
                            	{
                            		"geografikode": "NO34.3435",
                            		"navn": "VÅGÅ"
                            	},
                            	{
                            		"geografikode": "NO42.4205",
                            		"navn": "LINDESNES"
                            	},
                            	{
                            		"geografikode": "NO34.3447",
                            		"navn": "SØNDRE LAND"
                            	},
                            	{
                            		"geografikode": "NO42.4211",
                            		"navn": "GJERSTAD"
                            	},
                            	{
                            		"geografikode": "NO03",
                            		"navn": "OSLO"
                            	},
                            	{
                            		"geografikode": "NO34.3448",
                            		"navn": "NORDRE LAND"
                            	},
                            	{
                            		"geografikode": "NO34.3449",
                            		"navn": "SØR-AURDAL"
                            	},
                            	{
                            		"geografikode": "NO42.4213",
                            		"navn": "TVEDESTRAND"
                            	},
                            	{
                            		"geografikode": "NO42.4212",
                            		"navn": "VEGÅRSHEI"
                            	},
                            	{
                            		"geografikode": "NO40.4020",
                            		"navn": "MIDT-TELEMARK"
                            	},
                            	{
                            		"geografikode": "NO46.4620",
                            		"navn": "ULVIK"
                            	},
                            	{
                            		"geografikode": "NO15.1505",
                            		"navn": "KRISTIANSUND"
                            	},
                            	{
                            		"geografikode": "NO15.1506",
                            		"navn": "MOLDE"
                            	},
                            	{
                            		"geografikode": "NO40.4024",
                            		"navn": "HJARTDAL"
                            	},
                            	{
                            		"geografikode": "NO46.4622",
                            		"navn": "KVAM"
                            	},
                            	{
                            		"geografikode": "NO46.4621",
                            		"navn": "VOSS"
                            	},
                            	{
                            		"geografikode": "NO15.1508",
                            		"navn": "ÅLESUND"
                            	},
                            	{
                            		"geografikode": "NO40.4022",
                            		"navn": "SELJORD"
                            	},
                            	{
                            		"geografikode": "NO46.4624",
                            		"navn": "BJØRNAFJORDEN"
                            	},
                            	{
                            		"geografikode": "NO46.4623",
                            		"navn": "SAMNANGER"
                            	},
                            	{
                            		"geografikode": "NO40.4028",
                            		"navn": "KVITESEID"
                            	},
                            	{
                            		"geografikode": "NO46.4626",
                            		"navn": "ØYGARDEN"
                            	},
                            	{
                            		"geografikode": "NO46.4625",
                            		"navn": "AUSTEVOLL"
                            	},
                            	{
                            		"geografikode": "NO99.2111",
                            		"navn": "LONGYEARBYEN"
                            	},
                            	{
                            		"geografikode": "NO40.4026",
                            		"navn": "TINN"
                            	},
                            	{
                            		"geografikode": "NO46.4628",
                            		"navn": "VAKSDAL"
                            	},
                            	{
                            		"geografikode": "NO46.4627",
                            		"navn": "ASKØY"
                            	},
                            	{
                            		"geografikode": "NO46.4629",
                            		"navn": "MODALEN"
                            	},
                            	{
                            		"geografikode": "NO42.4219",
                            		"navn": "EVJE OG HORNNES"
                            	},
                            	{
                            		"geografikode": "NO34.3440",
                            		"navn": "ØYER"
                            	},
                            	{
                            		"geografikode": "NO42.4218",
                            		"navn": "IVELAND"
                            	},
                            	{
                            		"geografikode": "NO34.3441",
                            		"navn": "GAUSDAL"
                            	},
                            	{
                            		"geografikode": "NO34.3442",
                            		"navn": "ØSTRE TOTEN"
                            	},
                            	{
                            		"geografikode": "NO34.3443",
                            		"navn": "VESTRE TOTEN"
                            	},
                            	{
                            		"geografikode": "NO42.4215",
                            		"navn": "LILLESAND"
                            	},
                            	{
                            		"geografikode": "NO42.4214",
                            		"navn": "FROLAND"
                            	},
                            	{
                            		"geografikode": "NO42.4217",
                            		"navn": "ÅMLI"
                            	},
                            	{
                            		"geografikode": "NO34.3446",
                            		"navn": "GRAN"
                            	},
                            	{
                            		"geografikode": "NO42.4216",
                            		"navn": "BIRKENES"
                            	},
                            	{
                            		"geografikode": "NO42.4222",
                            		"navn": "BYKLE"
                            	},
                            	{
                            		"geografikode": "NO42.4221",
                            		"navn": "VALLE"
                            	},
                            	{
                            		"geografikode": "NO42.4224",
                            		"navn": "ÅSERAL"
                            	},
                            	{
                            		"geografikode": "NO42.4223",
                            		"navn": "VENNESLA"
                            	},
                            	{
                            		"geografikode": "NO39",
                            		"navn": "VESTFOLD"
                            	},
                            	{
                            		"geografikode": "NO32",
                            		"navn": "AKERSHUS"
                            	},
                            	{
                            		"geografikode": "NO33",
                            		"navn": "BUSKERUD"
                            	},
                            	{
                            		"geografikode": "NO42.4220",
                            		"navn": "BYGLAND"
                            	},
                            	{
                            		"geografikode": "NO34",
                            		"navn": "INNLANDET"
                            	},
                            	{
                            		"geografikode": "NO11.1111",
                            		"navn": "SOKNDAL"
                            	},
                            	{
                            		"geografikode": "NO11.1112",
                            		"navn": "LUND"
                            	},
                            	{
                            		"geografikode": "NO46.4611",
                            		"navn": "ETNE"
                            	},
                            	{
                            		"geografikode": "NO46.4613",
                            		"navn": "BØMLO"
                            	},
                            	{
                            		"geografikode": "NO46.4612",
                            		"navn": "SVEIO"
                            	},
                            	{
                            		"geografikode": "NO46.4615",
                            		"navn": "FITJAR"
                            	},
                            	{
                            		"geografikode": "NO11.1119",
                            		"navn": "HÅ"
                            	},
                            	{
                            		"geografikode": "NO46.4614",
                            		"navn": "STORD"
                            	},
                            	{
                            		"geografikode": "NO99.2100",
                            		"navn": "SVALBARD"
                            	},
                            	{
                            		"geografikode": "NO46.4617",
                            		"navn": "KVINNHERAD"
                            	},
                            	{
                            		"geografikode": "NO46.4616",
                            		"navn": "TYSNES"
                            	},
                            	{
                            		"geografikode": "NO11.1114",
                            		"navn": "BJERKREIM"
                            	},
                            	{
                            		"geografikode": "NO46.4619",
                            		"navn": "EIDFJORD"
                            	},
                            	{
                            		"geografikode": "NO46.4618",
                            		"navn": "ULLENSVANG"
                            	},
                            	{
                            		"geografikode": "NO34.3450",
                            		"navn": "ETNEDAL"
                            	},
                            	{
                            		"geografikode": "NO34.3451",
                            		"navn": "NORD-AURDAL"
                            	},
                            	{
                            		"geografikode": "NO40",
                            		"navn": "TELEMARK"
                            	},
                            	{
                            		"geografikode": "NO34.3452",
                            		"navn": "VESTRE SLIDRE"
                            	},
                            	{
                            		"geografikode": "NO34.3453",
                            		"navn": "ØYSTRE SLIDRE"
                            	},
                            	{
                            		"geografikode": "NO42",
                            		"navn": "AGDER"
                            	},
                            	{
                            		"geografikode": "NO34.3454",
                            		"navn": "VANG"
                            	},
                            	{
                            		"geografikode": "NO42.4226",
                            		"navn": "HÆGEBOSTAD"
                            	},
                            	{
                            		"geografikode": "NO42.4225",
                            		"navn": "LYNGDAL"
                            	},
                            	{
                            		"geografikode": "NO42.4228",
                            		"navn": "SIRDAL"
                            	},
                            	{
                            		"geografikode": "NO42.4227",
                            		"navn": "KVINESDAL"
                            	},
                            	{
                            		"geografikode": "NO50.5001",
                            		"navn": "TRONDHEIM"
                            	},
                            	{
                            		"geografikode": "NO21",
                            		"navn": "SVALBARD"
                            	},
                            	{
                            		"geografikode": "NO22",
                            		"navn": "JAN MAYEN"
                            	},
                            	{
                            		"geografikode": "NO23",
                            		"navn": "KONTINENTALSOKKELEN"
                            	},
                            	{
                            		"geografikode": "NO11.1101",
                            		"navn": "EIGERSUND"
                            	},
                            	{
                            		"geografikode": "NO46.4602",
                            		"navn": "KINN"
                            	},
                            	{
                            		"geografikode": "NO46.4601",
                            		"navn": "BERGEN"
                            	},
                            	{
                            		"geografikode": "NO99.2211",
                            		"navn": "JAN MAYEN"
                            	},
                            	{
                            		"geografikode": "NO11.1108",
                            		"navn": "SANDNES"
                            	},
                            	{
                            		"geografikode": "NO11.1103",
                            		"navn": "STAVANGER"
                            	},
                            	{
                            		"geografikode": "NO11.1106",
                            		"navn": "HAUGESUND"
                            	},
                            	{
                            		"geografikode": "NO50.5006",
                            		"navn": "STEINKJER"
                            	},
                            	{
                            		"geografikode": "NO31",
                            		"navn": "ØSTFOLD"
                            	},
                            	{
                            		"geografikode": "NO50.5007",
                            		"navn": "NAMSOS"
                            	}
                            ]
                                """.trimIndent()
                    )
            )
    )
}