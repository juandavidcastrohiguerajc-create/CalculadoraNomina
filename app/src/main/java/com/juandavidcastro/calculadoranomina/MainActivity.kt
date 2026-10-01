package com.juandavidcastro.calculadoranomina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juandavidcastro.calculadoranomina.ui.theme.CalculadoraNominaTheme
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

// ---------------------------------------------------------------------------
// Formato de moneda colombiana (F8)
// ---------------------------------------------------------------------------

private val formatoPesos: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
        maximumFractionDigits = 0
        roundingMode = RoundingMode.HALF_UP
    }

/** Presenta un valor en pesos colombianos, redondeado y con separador de miles. */
fun formatearPesos(valor: Double): String = formatoPesos.format(valor)

/** Traduce un tipo de error de validación a su mensaje en strings.xml (F10). */
@StringRes
fun recursoDeError(error: ErrorValidacion?): Int? = when (error) {
    ErrorValidacion.SALARIO_INVALIDO -> R.string.error_salario_vacio
    ErrorValidacion.SALARIO_BAJO_MINIMO -> R.string.error_salario_minimo
    ErrorValidacion.HORAS_INVALIDAS -> R.string.error_horas_negativas
    ErrorValidacion.HORAS_EXCESO -> R.string.error_horas_exceso
    null -> null
}

// ---------------------------------------------------------------------------
// Actividad principal
// ---------------------------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculadoraNominaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaNomina()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pantalla principal
// ---------------------------------------------------------------------------

@Composable
fun PantallaNomina(modifier: Modifier = Modifier) {
    // Entradas (F1 a F5): el estado vive en este composable padre y se conserva
    // al rotar la pantalla gracias a rememberSaveable.
    var salario by rememberSaveable { mutableStateOf("") }
    var horasDiurnas by rememberSaveable { mutableStateOf("") }
    var horasNocturnas by rememberSaveable { mutableStateOf("") }
    var esDominical by rememberSaveable { mutableStateOf(false) }
    var transporteEmpresa by rememberSaveable { mutableStateOf(false) }

    // Resultados y mensajes de error (F7, F10)
    var resultado by remember { mutableStateOf<ResultadoNomina?>(null) }
    var rango by remember { mutableStateOf<RangoSalarial?>(null) }
    var salarioCalculado by remember { mutableStateOf(0.0) }
    var errorSalario by remember { mutableStateOf<ErrorValidacion?>(null) }
    var errorHorasDiurnas by remember { mutableStateOf<ErrorValidacion?>(null) }
    var errorHorasNocturnas by remember { mutableStateOf<ErrorValidacion?>(null) }

    // Enfoque encadenado entre los campos (F1, F2, F3)
    val focoDiurnas = remember { FocusRequester() }
    val focoNocturnas = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current

    val scrollState = rememberScrollState()

    val mensajeErrorSalario = recursoDeError(errorSalario)?.let { stringResource(it) }
    val mensajeErrorDiurnas = recursoDeError(errorHorasDiurnas)?.let { stringResource(it) }
    val mensajeErrorNocturnas = recursoDeError(errorHorasNocturnas)?.let { stringResource(it) }

    /** Valida las entradas y, si son correctas, calcula la nómina (F6 y F10). */
    fun procesarCalculo() {
        val validacion = validarEntradas(salario, horasDiurnas, horasNocturnas)
        errorSalario = validacion.errorSalario
        errorHorasDiurnas = validacion.errorHorasDiurnas
        errorHorasNocturnas = validacion.errorHorasNocturnas
        if (validacion.esValido) {
            resultado = calcularNomina(
                salarioBasico = validacion.salario,
                horasDiurnas = validacion.horasDiurnas,
                horasNocturnas = validacion.horasNocturnas,
                esDominical = esDominical,
                transporteEmpresa = transporteEmpresa
            )
            rango = clasificarRango(validacion.salario)
            salarioCalculado = validacion.salario
        }
    }

    /** Vuelve la aplicación a su estado inicial (F11). */
    fun limpiarFormulario() {
        salario = ""
        horasDiurnas = ""
        horasNocturnas = ""
        esDominical = false
        transporteEmpresa = false
        resultado = null
        rango = null
        salarioCalculado = 0.0
        errorSalario = null
        errorHorasDiurnas = null
        errorHorasNocturnas = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.titulo_app),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.subtitulo_app),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // F1 – Salario básico mensual
        CampoNumerico(
            etiqueta = R.string.campo_salario,
            valor = salario,
            onValueChange = {
                salario = it
                errorSalario = null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focoDiurnas.requestFocus() }),
            modifier = Modifier,
            isError = errorSalario != null,
            mensajeError = mensajeErrorSalario
        )

        // F2 – Horas extra diurnas
        CampoNumerico(
            etiqueta = R.string.campo_horas_diurnas,
            valor = horasDiurnas,
            onValueChange = {
                horasDiurnas = it
                errorHorasDiurnas = null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focoNocturnas.requestFocus() }),
            modifier = Modifier.focusRequester(focoDiurnas),
            isError = errorHorasDiurnas != null,
            mensajeError = mensajeErrorDiurnas
        )

        // F3 – Horas extra nocturnas
        CampoNumerico(
            etiqueta = R.string.campo_horas_nocturnas,
            valor = horasNocturnas,
            onValueChange = {
                horasNocturnas = it
                errorHorasNocturnas = null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { teclado?.hide() }),
            modifier = Modifier.focusRequester(focoNocturnas),
            isError = errorHorasNocturnas != null,
            mensajeError = mensajeErrorNocturnas
        )

        Spacer(modifier = Modifier.height(4.dp))

        // F4 – Domingo o festivo
        FilaInterruptor(
            etiqueta = R.string.switch_dominical,
            checked = esDominical,
            onCheckedChange = {
                esDominical = it
                if (resultado != null) procesarCalculo()
            }
        )

        // F5 – Transporte suministrado por la empresa
        FilaInterruptor(
            etiqueta = R.string.switch_transporte,
            checked = transporteEmpresa,
            onCheckedChange = {
                transporteEmpresa = it
                if (resultado != null) procesarCalculo()
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // F6 y F11 – Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { procesarCalculo() },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.boton_calcular))
            }
            OutlinedButton(
                onClick = { limpiarFormulario() },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.boton_limpiar))
            }
        }

        // F9 – Imagen según el rango salarial, sólo después del primer cálculo
        val rangoActual = rango
        val resultadoActual = resultado
        if (rangoActual != null && resultadoActual != null) {
            bloqueRango(rangoActual)
            DesgloseNomina(
                resultado = resultadoActual,
                salarioBasico = salarioCalculado,
                rango = rangoActual
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.nombre_estudiante),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

/** Imagen del rango salarial con su texto explicativo y contentDescription (F9). */
@Composable
private fun bloqueRango(rango: RangoSalarial) {
    val (recurso, descripcion, texto) = when (rango) {
        RangoSalarial.RANGO_1 ->
            Triple(R.drawable.rango_1, R.string.content_description_rango_1, R.string.descripcion_rango_1)
        RangoSalarial.RANGO_2 ->
            Triple(R.drawable.rango_2, R.string.content_description_rango_2, R.string.descripcion_rango_2)
        RangoSalarial.RANGO_3 ->
            Triple(R.drawable.rango_3, R.string.content_description_rango_3, R.string.descripcion_rango_3)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(recurso),
            contentDescription = stringResource(descripcion),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ---------------------------------------------------------------------------
// F7 – Desglose de la nómina
// ---------------------------------------------------------------------------

@Composable
private fun DesgloseNomina(
    resultado: ResultadoNomina,
    salarioBasico: Double,
    rango: RangoSalarial,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        EncabezadoSeccion(texto = R.string.seccion_devengado)
        FilaConcepto(etiqueta = R.string.concepto_valor_hora, valor = formatearPesos(resultado.valorHora))
        FilaConcepto(etiqueta = R.string.concepto_salario_basico, valor = formatearPesos(salarioBasico))
        FilaConcepto(etiqueta = R.string.concepto_horas_extra, valor = formatearPesos(resultado.totalHorasExtra))
        FilaConcepto(
            etiqueta = R.string.concepto_auxilio_transporte,
            valor = formatearPesos(resultado.auxilioTransporte)
        )
        FilaConcepto(
            etiqueta = R.string.concepto_total_devengado,
            valor = formatearPesos(resultado.totalDevengado),
            destacado = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        EncabezadoSeccion(texto = R.string.seccion_deducciones)
        FilaConcepto(etiqueta = R.string.concepto_salud, valor = formatearPesos(resultado.aporteSalud))
        FilaConcepto(etiqueta = R.string.concepto_pension, valor = formatearPesos(resultado.aportePension))
        FilaConcepto(etiqueta = R.string.concepto_fondo, valor = formatearPesos(resultado.fondoSolidaridad))
        FilaConcepto(
            etiqueta = R.string.concepto_total_deducciones,
            valor = formatearPesos(resultado.totalDeducciones),
            destacado = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        EncabezadoSeccion(texto = R.string.seccion_resultado)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.concepto_salario_neto),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatearPesos(resultado.salarioNeto),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Funcionalidad opcional: equivalencia en salarios mínimos.
        Text(
            text = stringResource(
                R.string.equivalencia_smmlv,
                (resultado.salarioNeto / SMMLV_2026 * 100.0).roundToLong() / 100.0
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EncabezadoSeccion(@StringRes texto: Int) {
    Text(
        text = stringResource(texto),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun FilaConcepto(
    @StringRes etiqueta: Int,
    valor: String,
    destacado: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(etiqueta),
            style = if (destacado) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = valor,
            style = if (destacado) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ---------------------------------------------------------------------------
// Composables reutilizables sin estado propio (elevación de estado)
// ---------------------------------------------------------------------------

/**
 * Campo numérico reutilizable (F1, F2 y F3). No tiene estado propio:
 * el valor y el manejo de cambios viven en el composable padre.
 */
@Composable
fun CampoNumerico(
    @StringRes etiqueta: Int,
    valor: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions(),
    isError: Boolean = false,
    mensajeError: String? = null
) {
    val contenidoError: @Composable (() -> Unit)? = if (mensajeError != null) {
        { Text(text = mensajeError) }
    } else {
        null
    }

    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(etiqueta)) },
        singleLine = true,
        isError = isError,
        supportingText = contenidoError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

/** Fila con interruptor reutilizable (F4 y F5), sin estado propio. */
@Composable
fun FilaInterruptor(
    @StringRes etiqueta: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(etiqueta),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 12.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ---------------------------------------------------------------------------
// Vista previa (F12)
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun PantallaNominaPreview() {
    CalculadoraNominaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            PantallaNomina()
        }
    }
}
