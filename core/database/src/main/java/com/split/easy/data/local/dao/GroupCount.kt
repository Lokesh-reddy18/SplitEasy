package com.split.easy.data.local.dao

/** A count per group, such as payments waiting for your confirmation (the F7 badge). */
data class GroupCount(val groupId: String, val count: Int)
