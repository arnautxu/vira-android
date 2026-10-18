package cat.vira.android

import android.app.Application

/// Arrencada de l'app. Aquí hi aniran les inicialitzacions globals (base de dades, canals
/// de notificació, sincronització en segon pla) a mesura que els fils les construeixin.
class ViraApp : Application()
