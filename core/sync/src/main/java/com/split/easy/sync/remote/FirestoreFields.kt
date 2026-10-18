package com.split.easy.sync.remote

/**
 * Remote documents ↔ the field maps Firestore stores. Written by hand (instead of Firestore's
 * reflection-based toObject) so field names are explicit and it works the same in release builds.
 *
 * Firestore returns whole numbers as Long and may leave out null fields, so reading is lenient:
 * a missing optional field becomes null, and a missing required field throws, which the caller
 * treats as a damaged document and skips.
 */
internal object FirestoreFields {

    fun RemoteGroup.toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "createdBy" to createdBy,
        "memberUserIds" to memberUserIds,
        "isArchived" to isArchived,
        "lastActivityAt" to lastActivityAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
    )

    fun group(id: String, data: Map<String, Any?>) = RemoteGroup(
        id = id,
        name = data.string("name"),
        createdBy = data.string("createdBy"),
        memberUserIds = data.stringList("memberUserIds"),
        isArchived = data.boolean("isArchived"),
        lastActivityAt = data.long("lastActivityAt"),
        updatedAt = data.long("updatedAt"),
        isDeleted = data.boolean("isDeleted"),
    )

    fun RemoteMember.toMap(): Map<String, Any?> = mapOf(
        "groupId" to groupId,
        "displayName" to displayName,
        "userId" to userId,
        "upiId" to upiId,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "upiEventId" to upiEventId,
    )

    fun member(id: String, data: Map<String, Any?>) = RemoteMember(
        id = id,
        groupId = data.string("groupId"),
        displayName = data.string("displayName"),
        userId = data.optionalString("userId"),
        upiId = data.optionalString("upiId"),
        createdAt = data.long("createdAt"),
        updatedAt = data.long("updatedAt"),
        isDeleted = data.boolean("isDeleted"),
        upiEventId = data.optionalString("upiEventId"),
    )

    fun RemoteExpense.toMap(): Map<String, Any?> = mapOf(
        "groupId" to groupId,
        "paidBy" to paidBy,
        "amountPaise" to amountPaise,
        "description" to description,
        "category" to category,
        "date" to date,
        "splitType" to splitType,
        "createdBy" to createdBy,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "shares" to shares.mapValues { (_, share) ->
            mapOf("paise" to share.paise, "basisPoints" to share.basisPoints)
        },
    )

    fun expense(id: String, data: Map<String, Any?>) = RemoteExpense(
        id = id,
        groupId = data.string("groupId"),
        paidBy = data.string("paidBy"),
        amountPaise = data.long("amountPaise"),
        description = data.string("description"),
        category = data.string("category"),
        date = data.string("date"),
        splitType = data.string("splitType"),
        createdBy = data.string("createdBy"),
        createdAt = data.long("createdAt"),
        updatedAt = data.long("updatedAt"),
        isDeleted = data.boolean("isDeleted"),
        shares = (data["shares"] as? Map<*, *>).orEmpty().entries.associate { (memberId, value) ->
            val share = value as Map<*, *>
            memberId as String to RemoteShare(
                paise = (share["paise"] as Number).toLong(),
                basisPoints = (share["basisPoints"] as Number?)?.toInt(),
            )
        },
    )

    fun RemoteSettlement.toMap(): Map<String, Any?> = mapOf(
        "groupId" to groupId,
        "fromMemberId" to fromMemberId,
        "toMemberId" to toMemberId,
        "amountPaise" to amountPaise,
        "date" to date,
        "upiTxnId" to upiTxnId,
        "status" to status,
        "method" to method,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
    )

    fun settlement(id: String, data: Map<String, Any?>) = RemoteSettlement(
        id = id,
        groupId = data.string("groupId"),
        fromMemberId = data.string("fromMemberId"),
        toMemberId = data.string("toMemberId"),
        amountPaise = data.long("amountPaise"),
        date = data.string("date"),
        upiTxnId = data.optionalString("upiTxnId"),
        status = data.string("status"),
        method = data.string("method"),
        updatedAt = data.long("updatedAt"),
        isDeleted = data.boolean("isDeleted"),
    )

    fun RemoteEvent.toMap(): Map<String, Any?> = mapOf(
        "groupId" to groupId,
        "actorMemberId" to actorMemberId,
        "targetMemberId" to targetMemberId,
        "type" to type,
        "oldValue" to oldValue,
        "newValue" to newValue,
        "createdAt" to createdAt,
    )

    fun event(id: String, data: Map<String, Any?>) = RemoteEvent(
        id = id,
        groupId = data.string("groupId"),
        actorMemberId = data.string("actorMemberId"),
        targetMemberId = data.string("targetMemberId"),
        type = data.string("type"),
        oldValue = data.optionalString("oldValue"),
        newValue = data.string("newValue"),
        createdAt = data.long("createdAt"),
    )

    private fun Map<String, Any?>.string(key: String) = get(key) as? String ?: missing(key)

    private fun Map<String, Any?>.optionalString(key: String) = get(key) as? String

    private fun Map<String, Any?>.long(key: String) =
        (get(key) as? Number)?.toLong() ?: missing(key)

    private fun Map<String, Any?>.boolean(key: String) = get(key) as? Boolean ?: false

    private fun Map<String, Any?>.stringList(key: String) =
        (get(key) as? List<*>).orEmpty().filterIsInstance<String>()

    private fun missing(key: String): Nothing = throw IllegalArgumentException("Missing field $key")
}
