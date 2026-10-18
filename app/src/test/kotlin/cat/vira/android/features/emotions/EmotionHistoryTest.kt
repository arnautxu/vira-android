package cat.vira.android.features.emotions

import cat.vira.android.core.data.model.Emotion
import cat.vira.android.core.data.model.EmotionEntry
import cat.vira.android.core.data.model.emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/// `EmotionHistory`: l'agregació del període, sense pintar res (`EmotionHistoryTests` de l'iPhone).
///
/// Les regles que es proven aquí no són d'estil: els dies buits hi han de ser, l'ordre dels
/// recomptes no pot dependre de si l'emoció és agradable, i res de fora del període hi entra.
class EmotionHistoryTest {

    /// Una zona amb canvi d'hora, perquè restar dies no es pugui fer amb 24 hores fixes.
    private val zone = ZoneId.of("Europe/Madrid")

    /// Dilluns 3 d'agost del 2026, a migdia.
    private val now = LocalDateTime.of(2026, 8, 3, 12, 0).atZone(zone).toInstant()

    private fun entry(emotion: Emotion, daysAgo: Long, hour: Int = 10) = EmotionEntry(
        emotionRaw = emotion.raw,
        createdAt = LocalDateTime.of(2026, 8, 3, hour, 0).minusDays(daysAgo).atZone(zone).toInstant(),
    )

    private fun history(entries: List<EmotionEntry>, range: EmotionHistoryRange = EmotionHistoryRange.WEEK) =
        EmotionHistory(entries, range, now, zone)

    @Test fun laSetmanaTeSetDiesIElMesTrenta() {
        assertEquals(7, history(emptyList()).days.size)
        assertEquals(30, history(emptyList(), EmotionHistoryRange.MONTH).days.size)
    }

    @Test fun lUltimDiaSempreEsAvui() {
        assertEquals(now.atZone(zone).toLocalDate(), history(emptyList()).days.last().date)
    }

    @Test fun unRegistreMesAnticQueElPeriodeEnQuedaFora() {
        assertTrue(history(listOf(entry(Emotion.CONTENT, 10))).isEmpty)
        assertEquals(1, history(listOf(entry(Emotion.CONTENT, 10)), EmotionHistoryRange.MONTH).entries.size)
    }

    @Test fun elsDiesSenseRegistreHiSonIgualment() {
        val result = history(listOf(entry(Emotion.CONTENT, 0)))
        assertEquals(7, result.days.size)
        assertEquals(6, result.days.count { it.isEmpty })
    }

    @Test fun laLlistaPerDiesNomesMostraElsQueTenenAlgunaCosa() {
        assertEquals(2, history(listOf(entry(Emotion.CONTENT, 0), entry(Emotion.SAD, 2))).daysWithEntries.size)
    }

    @Test fun elsDiesDeLaLlistaVanDelMesRecentAlMesAntic() {
        val dates = history(listOf(entry(Emotion.CONTENT, 3), entry(Emotion.SAD, 0))).daysWithEntries.map { it.date }
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test fun elsRegistresVanDelMesRecentAlMesAntic() {
        val result = history(listOf(entry(Emotion.CONTENT, 2), entry(Emotion.SAD, 0), entry(Emotion.ANGRY, 1)))
        assertEquals(listOf(Emotion.SAD, Emotion.ANGRY, Emotion.CONTENT), result.entries.map { it.emotion })
    }

    @Test fun dinsDUnMateixDiaTambeDelMesRecentAlMesAntic() {
        val result = history(listOf(entry(Emotion.CONTENT, 0, hour = 9), entry(Emotion.SAD, 0, hour = 20)))
        assertEquals(listOf(Emotion.SAD, Emotion.CONTENT), result.daysWithEntries.first().entries.map { it.emotion })
    }

    @Test fun elRecomptePerEmocioSumaLesRepeticions() {
        val result = history(listOf(entry(Emotion.ANXIOUS, 0), entry(Emotion.ANXIOUS, 1), entry(Emotion.CONTENT, 2)))
        assertEquals(listOf(2, 1), result.counts.map { it.count })
        assertEquals(Emotion.ANXIOUS, result.counts.first().emotion)
    }

    @Test fun nomesSurtenLesEmocionsRegistrades() {
        assertEquals(1, history(listOf(entry(Emotion.CONTENT, 0))).counts.size)
        assertTrue(history(emptyList()).counts.isEmpty())
    }

    /// Amb el mateix recompte, l'ordre és el de la graella del check-in, **mai** el d'agradable
    /// primer (`VIRI_AND_GAMIFICATION.md` §2.2.1, "prohibit: rànquings d'emocions").
    @Test fun ambElMateixRecompteLOrdreEsEstableINoValoraLEmocio() {
        val result = history(listOf(entry(Emotion.SAD, 0), entry(Emotion.CONTENT, 1)))
        assertEquals(listOf(Emotion.CONTENT, Emotion.SAD), result.counts.map { it.emotion })
    }

    @Test fun elDiaAmbMesRegistresMarcaElSostre() {
        val result = history(
            listOf(
                entry(Emotion.CONTENT, 0, hour = 9), entry(Emotion.SAD, 0, hour = 12),
                entry(Emotion.ANGRY, 0, hour = 18), entry(Emotion.CONTENT, 3),
            ),
        )
        assertEquals(3, result.busiestDayCount)
        assertEquals(0, history(emptyList()).busiestDayCount)
    }

    @Test fun lEscenaVaPerFranges() {
        assertEquals("morning", EmotionHistoryArt.band(6))
        assertEquals("morning", EmotionHistoryArt.band(12))
        assertEquals("afternoon", EmotionHistoryArt.band(13))
        assertEquals("afternoon", EmotionHistoryArt.band(19))
        assertEquals("night", EmotionHistoryArt.band(20))
        assertEquals("night", EmotionHistoryArt.band(2))
    }

    /// El 29 de març del 2026 Madrid passa de l'hora d'hivern a la d'estiu: el dia en fa 23. Un
    /// registre de les 23:30 d'aquell dia ha de caure al seu dia, no a l'endemà.
    @Test fun elCanviDHoraNoMouUnRegistreDeDia() {
        val springNow = LocalDateTime.of(2026, 3, 31, 12, 0).atZone(zone).toInstant()
        val late = EmotionEntry(createdAt = LocalDateTime.of(2026, 3, 29, 23, 30).atZone(zone).toInstant())
        val result = EmotionHistory(listOf(late), EmotionHistoryRange.WEEK, springNow, zone)
        assertEquals(LocalDateTime.of(2026, 3, 29, 0, 0).toLocalDate(), result.daysWithEntries.single().date)
    }
}
