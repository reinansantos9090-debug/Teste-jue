package com.example.testejuerpg.offline.systems

data class OfflineEmote(
    val id: String,
    val name: String,
    val category: String,
    val duration: Float,
    val motion: Int,
    val accent: Int
)

object OfflineEmoteCatalog {
    val all = listOf(
        e("wave","Acenar","social",1.0f,0,0x7FD7FF), e("cheer","Comemorar","social",1.2f,1,0xFFD15C),
        e("dance_neon","Dança Neon","dance",2.4f,2,0xB98CFF), e("dance_prism","Dança Prisma","dance",2.6f,3,0x7AFF91),
        e("victory","Vitória","social",1.5f,4,0xFF8A5C), e("salute","Continência","social",1.1f,5,0x9FE7FF),
        e("point","Apontar","social",0.9f,6,0xFFE89A), e("laugh","Rir","social",1.3f,7,0xFF89C8),
        e("think","Pensar","personality",1.8f,8,0xC59AFF), e("shrug","Dar de Ombros","personality",1.4f,9,0xAFC4DD),
        e("sit","Sentar","personality",2.0f,10,0x8AC7FF), e("spin","Giro","dance",1.8f,11,0x66E7E9),
        e("hero_pose","Pose de Herói","social",1.7f,12,0xFFB76A), e("charge","Carregar Energia","combat",1.4f,13,0x75FFB8),
        e("shield","Escudo","combat",1.1f,14,0x8FC4FF), e("ready","Pronto!","combat",0.8f,15,0xFF718E),
        e("spark","Faísca","social",1.0f,16,0xD6F5FF), e("portal_call","Chamar Portal","social",1.5f,17,0xA98BFF),
        e("sleep","Descansar","personality",2.2f,18,0x8E9AC8), e("celebrate","Festa","dance",2.8f,19,0xFF8DCB),
        e("hype","Aquecer","combat",1.7f,20,0xFFDA5A), e("friendship","Parceiro","social",1.2f,21,0x9AFFD0),
        e("scan","Escanear","utility",1.3f,22,0x63F1FF), e("focus","Concentrar","personality",1.6f,23,0xC7B8FF)
    )
    private fun e(id: String,name: String,category: String,duration: Float,motion: Int,accent: Int) =
        OfflineEmote(id,name,category,duration,motion,accent)
}
