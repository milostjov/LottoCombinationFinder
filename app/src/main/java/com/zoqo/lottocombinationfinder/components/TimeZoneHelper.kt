package com.zoqo.lottocombinationfinder.components

object TimeZoneHelper {


        /**
         * Vraca listu UTC zona u intervalu -12 do +14
         * ukljucujuci polusatne zone
         */
        fun getUtcOffsets(): List<String> {
            val offsets = mutableListOf<String>()
            for (halfHour in -24..28) {  // -12*2 do +14*2
                val hours = halfHour / 2.0
                val label = if (hours >= 0)
                    "UTC+${if (hours % 1 == 0.0) hours.toInt() else hours}"
                else
                    "UTC${hours}"
                offsets.add(label)
            }
            return offsets
        }
    }

