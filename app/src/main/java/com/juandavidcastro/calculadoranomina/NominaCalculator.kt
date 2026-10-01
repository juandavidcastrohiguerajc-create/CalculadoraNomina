package com.juandavidcastro.calculadoranomina

/**
 * Lógica de nómina simplificada, separada de la interfaz (F6).
 *
 * Modelo académico basado en la normativa vigente en septiembre de 2026.
 * No incluye retención en la fuente, recargos nocturnos ordinarios ni prestaciones sociales.
 */

// ---------------------------------------------------------------------------
// Constantes (con su fuente normativa)
// ---------------------------------------------------------------------------

/** Salario mínimo mensual legal vigente 2026 - Decreto 1469 de 2025. */
const val SMMLV_2026 = 1_750_905.0

/** Auxilio de transporte 2026 - Decreto 1470 de 2025. */
const val AUXILIO_TRANSPORTE_2026 = 249_095.0

/**
 * Horas ordinarias mensuales: jornada de 42 horas semanales
 * (Ley 2101 de 2021), vigente desde el 15 de julio de 2026.
 */
const val HORAS_ORDINARIAS_MES = 210.0

/** Porcentaje de aporte a salud del trabajador - Ley 100 de 1993. */
const val PORCENTAJE_SALUD = 0.04

/** Porcentaje de aporte a pensión del trabajador - Ley 100 de 1993. */
const val PORCENTAJE_PENSION = 0.04

/** Porcentaje del Fondo de Solidaridad Pensional - Ley 797 de 2003. */
const val PORCENTAJE_FONDO_SOLIDARIDAD = 0.01

/** Máximo de horas extra permitidas en el mes para el taller (12 sem. x 4 semanas). */
const val MAXIMO_HORAS_EXTRA_MES = 48.0

// ---------------------------------------------------------------------------
// Resultado del cálculo
// ---------------------------------------------------------------------------

data class ResultadoNomina(
    val valorHora: Double,
    val totalHorasExtra: Double,
    val auxilioTransporte: Double,
    val totalDevengado: Double,
    val aporteSalud: Double,
    val aportePension: Double,
    val fondoSolidaridad: Double,
    val totalDeducciones: Double,
    val salarioNeto: Double
)

// ---------------------------------------------------------------------------
// Rangos salariales (Regla 8)
// ---------------------------------------------------------------------------

enum class RangoSalarial { RANGO_1, RANGO_2, RANGO_3 }

/** Clasifica al trabajador según su salario básico (Regla 8). */
fun clasificarRango(salarioBasico: Double): RangoSalarial = when {
    salarioBasico <= 2 * SMMLV_2026 -> RangoSalarial.RANGO_1
    salarioBasico < 4 * SMMLV_2026 -> RangoSalarial.RANGO_2
    else -> RangoSalarial.RANGO_3
}

// ---------------------------------------------------------------------------
// Cálculo de la nómina (Reglas 1 a 7)
// ---------------------------------------------------------------------------

fun calcularNomina(
    salarioBasico: Double,
    horasDiurnas: Double,
    horasNocturnas: Double,
    esDominical: Boolean,
    transporteEmpresa: Boolean
): ResultadoNomina {
    // Regla 1 - valor de la hora ordinaria
    val valorHora = salarioBasico / HORAS_ORDINARIAS_MES

    // Regla 2 - factores según el tipo de día
    val factorDiurno = if (esDominical) 2.15 else 1.25
    val factorNocturno = if (esDominical) 2.65 else 1.75

    val pagoExtrasDiurnas = horasDiurnas * valorHora * factorDiurno
    val pagoExtrasNocturnas = horasNocturnas * valorHora * factorNocturno
    val totalHorasExtra = pagoExtrasDiurnas + pagoExtrasNocturnas

    // Regla 3 - Ingreso Base de Cotización (el auxilio de transporte no hace parte del IBC)
    val ibc = salarioBasico + totalHorasExtra

    // Regla 4 - auxilio de transporte
    val auxilioTransporte = if (salarioBasico <= 2 * SMMLV_2026 && !transporteEmpresa) {
        AUXILIO_TRANSPORTE_2026
    } else {
        0.0
    }

    // Regla 5 - total devengado
    val totalDevengado = ibc + auxilioTransporte

    // Regla 6 - deducciones
    val aporteSalud = ibc * PORCENTAJE_SALUD
    val aportePension = ibc * PORCENTAJE_PENSION
    val fondoSolidaridad = if (ibc >= 4 * SMMLV_2026) {
        ibc * PORCENTAJE_FONDO_SOLIDARIDAD
    } else {
        0.0
    }
    val totalDeducciones = aporteSalud + aportePension + fondoSolidaridad

    // Regla 7 - salario neto
    val salarioNeto = totalDevengado - totalDeducciones

    return ResultadoNomina(
        valorHora = valorHora,
        totalHorasExtra = totalHorasExtra,
        auxilioTransporte = auxilioTransporte,
        totalDevengado = totalDevengado,
        aporteSalud = aporteSalud,
        aportePension = aportePension,
        fondoSolidaridad = fondoSolidaridad,
        totalDeducciones = totalDeducciones,
        salarioNeto = salarioNeto
    )
}

// ---------------------------------------------------------------------------
// Validación de las entradas (F10)
// ---------------------------------------------------------------------------

/** Tipos de error posibles; la interfaz los traduce a mensajes de strings.xml. */
enum class ErrorValidacion {
    SALARIO_INVALIDO,
    SALARIO_BAJO_MINIMO,
    HORAS_INVALIDAS,
    HORAS_EXCESO
}

data class ResultadoValidacion(
    val errorSalario: ErrorValidacion? = null,
    val errorHorasDiurnas: ErrorValidacion? = null,
    val errorHorasNocturnas: ErrorValidacion? = null,
    val salario: Double = 0.0,
    val horasDiurnas: Double = 0.0,
    val horasNocturnas: Double = 0.0
) {
    val esValido: Boolean
        get() = errorSalario == null && errorHorasDiurnas == null && errorHorasNocturnas == null
}

/**
 * Convierte los textos de entrada a números aplicando las restricciones de la sección 3.2.
 * Las horas vacías se interpretan como 0. Nunca lanza excepciones.
 */
fun validarEntradas(
    salarioTexto: String,
    horasDiurnasTexto: String,
    horasNocturnasTexto: String
): ResultadoValidacion {
    // Salario obligatorio, numérico y no inferior a un SMMLV.
    val salario = salarioTexto.trim().toDoubleOrNull()
        ?: return ResultadoValidacion(errorSalario = ErrorValidacion.SALARIO_INVALIDO)
    if (salario < SMMLV_2026) {
        return ResultadoValidacion(
            errorSalario = ErrorValidacion.SALARIO_BAJO_MINIMO,
            salario = salario
        )
    }

    // Las horas vacías se toman como 0; no pueden ser negativas ni no numéricas.
    val diurnas = if (horasDiurnasTexto.isBlank()) {
        0.0
    } else {
        horasDiurnasTexto.trim().toDoubleOrNull()
    }
    if (diurnas == null || diurnas < 0) {
        return ResultadoValidacion(
            errorHorasDiurnas = ErrorValidacion.HORAS_INVALIDAS,
            salario = salario
        )
    }

    val nocturnas = if (horasNocturnasTexto.isBlank()) {
        0.0
    } else {
        horasNocturnasTexto.trim().toDoubleOrNull()
    }
    if (nocturnas == null || nocturnas < 0) {
        return ResultadoValidacion(
            errorHorasNocturnas = ErrorValidacion.HORAS_INVALIDAS,
            salario = salario,
            horasDiurnas = diurnas
        )
    }

    // Máximo 48 horas extra en el mes (12 semanales x 4 semanas).
    if (diurnas + nocturnas > MAXIMO_HORAS_EXTRA_MES) {
        return ResultadoValidacion(
            errorHorasDiurnas = ErrorValidacion.HORAS_EXCESO,
            errorHorasNocturnas = ErrorValidacion.HORAS_EXCESO,
            salario = salario,
            horasDiurnas = diurnas,
            horasNocturnas = nocturnas
        )
    }

    return ResultadoValidacion(
        salario = salario,
        horasDiurnas = diurnas,
        horasNocturnas = nocturnas
    )
}
