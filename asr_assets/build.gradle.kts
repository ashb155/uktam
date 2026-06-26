plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("asr_assets")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
