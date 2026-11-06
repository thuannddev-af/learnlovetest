package com.ads.admob.data

class IapItem {
    @JvmField
    var itemId: String
    @JvmField
    var trialId: String? = null
    @JvmField
    var type: String

    constructor(itemId: String, type: String) {
        this.itemId = itemId
        this.type = type
    }

    constructor(itemId: String, trialId: String?, type: String) {
        this.itemId = itemId
        this.trialId = trialId
        this.type = type
    }
}
