package com.uvos.gallery.domain.model

enum class ThreatLevel {
    /** GPS latitude/longitude detected - immediate leak hazard */
    HIGH_RISK,

    /** Camera model, lens serial or exact timestamp present */
    MEDIUM_RISK,

    /** Stripped / no privacy-compromising EXIF tags present */
    STERILE,

    /** Encrypted in local AES-256 vault */
    ENCRYPTED
}
