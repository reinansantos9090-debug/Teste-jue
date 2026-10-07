package com.example.testejuerpg.offline.systems

import android.content.SharedPreferences

data class HistoryEntry(val id:String,val title:String,val type:String,val reward:Int,val timestamp:Long)

class OfflineHistoryArchive {
    private val entries=mutableListOf<HistoryEntry>()
    fun entries():List<HistoryEntry>=entries.toList()
    fun record(id:String,title:String,type:String,reward:Int,now:Long){
        entries.removeAll{it.id==id}; entries+=HistoryEntry(id,title,type,reward,now)
        if(entries.size>120) entries.removeAt(0)
    }
    fun saveTo(prefs:SharedPreferences){
        prefs.edit().putString("history",entries.joinToString(";"){listOf(it.id,it.title,it.type,it.reward.toString(),it.timestamp.toString()).joinToString("|")}).apply()
    }
    fun loadFrom(prefs:SharedPreferences){
        entries.clear()
        prefs.getString("history",null).orEmpty().split(";").mapNotNull{
            val p=it.split("|"); if(p.size!=5)null else HistoryEntry(p[0],p[1],p[2],p[3].toIntOrNull()?:0,p[4].toLongOrNull()?:0L)
        }.forEach{entries+=it}
    }
}
