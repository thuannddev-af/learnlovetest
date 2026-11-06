package com.ads.admob.data

import com.google.gson.annotations.SerializedName

data class ReponseInfo(
    @SerializedName("Adapter Responses")
    val adapterResponses: List<AdapterResponse>?,
    @SerializedName("Loaded Adapter Response")
    val loadedAdapterResponse: LoadedAdapterResponse?,
    @SerializedName("Mediation Adapter Class Name")
    val mediationAdapterClassName: String?,
    @SerializedName("Response Extras")
    val responseExtras: ResponseExtras?,
    @SerializedName("Response ID")
    val responseID: String?
)

data class AdapterResponse(
    @SerializedName("Ad Error")
    val adError: String?,
    @SerializedName("Ad Source ID")
    val adSourceID: String?,
    @SerializedName("Ad Source Instance ID")
    val adSourceInstanceID: String?,
    @SerializedName("Ad Source Instance Name")
    val adSourceInstanceName: String?,
    @SerializedName("Ad Source Name")
    val adSourceName: String?,
    @SerializedName("Adapter")
    val adapter: String?,
    @SerializedName("Credentials")
    val credentials: Credentials?,
    @SerializedName("Latency")
    val latency: Int?
)

data class LoadedAdapterResponse(
    @SerializedName("Ad Error")
    val adError: String?,
    @SerializedName("Ad Source ID")
    val adSourceID: String?,
    @SerializedName("Ad Source Instance ID")
    val adSourceInstanceID: String?,
    @SerializedName("Ad Source Instance Name")
    val adSourceInstanceName: String?,
    @SerializedName("Ad Source Name")
    val adSourceName: String?,
    @SerializedName("Adapter")
    val adapter: String?,
    @SerializedName("Credentials")
    val credentials: Credentials?,
    @SerializedName("Latency")
    val latency: Int?
)

data class ResponseExtras(
    @SerializedName("mediation_group_name")
    val mediationGroupName: String?
)

class Credentials