package com.juandavidcastro.calculadoranomina

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la lógica de nómina contra los casos de verificación de la sección 3.4.
 * Se aceptan diferencias de hasta $1 por redondeo.
 */
class NominaCalculatorTest {

    private fun redondear(valor: Double): Long = Math.round(valor)

    /** Caso A – Salario mínimo con horas extra en día hábil. */
    @Test
    fun casoA_salarioMinimoDiaHabil() {
        val r = calcularNomina(
            salarioBasico = 1_750_905.0,
            horasDiurnas = 10.0,
            horasNocturnas = 4.0,
            esDominical = false,
            transporteEmpresa = false
        )
        assertEquals(8_338, redondear(r.valorHora))
        assertEquals(162_584, redondear(r.totalHorasExtra))
        assertEquals(249_095, redondear(r.auxilioTransporte))
        assertEquals(2_162_584, redondear(r.totalDevengado))
        assertEquals(76_540, redondear(r.aporteSalud))
        assertEquals(76_540, redondear(r.aportePension))
        assertEquals(0, redondear(r.fondoSolidaridad))
        assertEquals(2_009_505, redondear(r.salarioNeto))
        assertEquals(RangoSalarial.RANGO_1, clasificarRango(1_750_905.0))
    }

    /** Caso B – Horas extra en domingo o festivo. */
    @Test
    fun casoB_horasExtraDominicales() {
        val r = calcularNomina(
            salarioBasico = 2_500_000.0,
            horasDiurnas = 6.0,
            horasNocturnas = 2.0,
            esDominical = true,
            transporteEmpresa = false
        )
        assertEquals(11_905, redondear(r.valorHora))
        assertEquals(216_667, redondear(r.totalHorasExtra))
        assertEquals(249_095, redondear(r.auxilioTransporte))
        assertEquals(2_965_762, redondear(r.totalDevengado))
        assertEquals(108_667, redondear(r.aporteSalud))
        assertEquals(108_667, redondear(r.aportePension))
        assertEquals(0, redondear(r.fondoSolidaridad))
        assertEquals(2_748_428, redondear(r.salarioNeto))
        assertEquals(RangoSalarial.RANGO_1, clasificarRango(2_500_000.0))
    }

    /** Caso C – Transporte suministrado por la empresa. */
    @Test
    fun casoC_transporteDeLaEmpresa() {
        val r = calcularNomina(
            salarioBasico = 3_000_000.0,
            horasDiurnas = 5.0,
            horasNocturnas = 0.0,
            esDominical = false,
            transporteEmpresa = true
        )
        assertEquals(14_286, redondear(r.valorHora))
        assertEquals(89_286, redondear(r.totalHorasExtra))
        assertEquals(0, redondear(r.auxilioTransporte))
        assertEquals(3_089_286, redondear(r.totalDevengado))
        assertEquals(123_571, redondear(r.aporteSalud))
        assertEquals(123_571, redondear(r.aportePension))
        assertEquals(0, redondear(r.fondoSolidaridad))
        assertEquals(2_842_143, redondear(r.salarioNeto))
        assertEquals(RangoSalarial.RANGO_1, clasificarRango(3_000_000.0))
    }

    /** Caso D – Salario alto con Fondo de Solidaridad Pensional. */
    @Test
    fun casoD_salarioAltoConFondoSolidaridad() {
        val r = calcularNomina(
            salarioBasico = 8_000_000.0,
            horasDiurnas = 0.0,
            horasNocturnas = 0.0,
            esDominical = false,
            transporteEmpresa = false
        )
        assertEquals(38_095, redondear(r.valorHora))
        assertEquals(0, redondear(r.totalHorasExtra))
        assertEquals(0, redondear(r.auxilioTransporte))
        assertEquals(8_000_000, redondear(r.totalDevengado))
        assertEquals(320_000, redondear(r.aporteSalud))
        assertEquals(320_000, redondear(r.aportePension))
        assertEquals(80_000, redondear(r.fondoSolidaridad))
        assertEquals(7_280_000, redondear(r.salarioNeto))
        assertEquals(RangoSalarial.RANGO_3, clasificarRango(8_000_000.0))
    }

    /** Caso E – Validaciones. */
    @Test
    fun casoE_salarioVacio() {
        val v = validarEntradas("", "", "")
        assertEquals(ErrorValidacion.SALARIO_INVALIDO, v.errorSalario)
        assertTrue(!v.esValido)
    }

    @Test
    fun casoE_salarioNoNumerico() {
        val v = validarEntradas("abc", "", "")
        assertEquals(ErrorValidacion.SALARIO_INVALIDO, v.errorSalario)
    }

    @Test
    fun casoE_salarioInferiorAlMinimo() {
        val v = validarEntradas("1000000", "", "")
        assertEquals(ErrorValidacion.SALARIO_BAJO_MINIMO, v.errorSalario)
    }

    @Test
    fun casoE_excesoDeHorasExtra() {
        val v = validarEntradas("2000000", "30", "20")
        assertEquals(ErrorValidacion.HORAS_EXCESO, v.errorHorasDiurnas)
        assertEquals(ErrorValidacion.HORAS_EXCESO, v.errorHorasNocturnas)
    }

    @Test
    fun casoE_horasVaciasSeInterpretanComoCero() {
        val v = validarEntradas("2000000", "", "")
        assertTrue(v.esValido)
        assertEquals(0.0, v.horasDiurnas, 0.0)
        assertEquals(0.0, v.horasNocturnas, 0.0)

        val r = calcularNomina(v.salario, v.horasDiurnas, v.horasNocturnas, false, false)
        assertEquals(0, redondear(r.totalHorasExtra))
        assertEquals(2_000_000, redondear(r.totalDevengado - r.auxilioTransporte))
    }

    @Test
    fun casoE_horasNegativas() {
        val v = validarEntradas("2000000", "-3", "")
        assertEquals(ErrorValidacion.HORAS_INVALIDAS, v.errorHorasDiurnas)
    }

    @Test
    fun casoE_horasNoNumericas() {
        val v = validarEntradas("2000000", "", "x")
        assertEquals(ErrorValidacion.HORAS_INVALIDAS, v.errorHorasNocturnas)
    }

    /** Límite exacto de 48 horas extra: se permite. */
    @Test
    fun limite48HorasPermitido() {
        val v = validarEntradas("2000000", "30", "18")
        assertTrue(v.esValido)
    }

    /** El auxilio no aplica si el salario supera 2 SMMLV aunque la empresa no lo suministre. */
    @Test
    fun auxilioNoAplicaSobreDosSmmlv() {
        val r = calcularNomina(3_501_811.0, 0.0, 0.0, false, transporteEmpresa = false)
        assertEquals(0.0, r.auxilioTransporte, 0.0)
    }

    /** Frontera exacta de 2 SMMLV: sí recibe auxilio. */
    @Test
    fun auxilioAplicaHastaDosSmmlv() {
        val r = calcularNomina(3_501_810.0, 0.0, 0.0, false, transporteEmpresa = false)
        assertEquals(249_095.0, r.auxilioTransporte, 0.0)
    }

    /** Clasificación de rangos (Regla 8). */
    @Test
    fun rangosSalariales() {
        assertEquals(RangoSalarial.RANGO_1, clasificarRango(3_501_810.0))
        assertEquals(RangoSalarial.RANGO_2, clasificarRango(3_501_811.0))
        assertEquals(RangoSalarial.RANGO_2, clasificarRango(7_003_619.0))
        assertEquals(RangoSalarial.RANGO_3, clasificarRango(7_003_620.0))
    }
}
