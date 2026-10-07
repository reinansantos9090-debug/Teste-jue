package com.example.testejuerpg.offline.systems

data class OfflineSquadMember(
    val id:String,val name:String,val role:String,val tint:Int,
    val damageMultiplier:Float,val healMultiplier:Float,val moveMultiplier:Float
)
object OfflineSquadCatalog {
    val all=listOf(
        OfflineSquadMember("ari","Ari","Vanguarda",0x7FD7FF,1.12f,1.00f,1.04f),
        OfflineSquadMember("mila","Mila","Suporte",0x7AFF91,0.82f,1.38f,0.98f),
        OfflineSquadMember("lio","Lio","Controle",0xC59AFF,0.96f,1.08f,1.12f)
    )
}
