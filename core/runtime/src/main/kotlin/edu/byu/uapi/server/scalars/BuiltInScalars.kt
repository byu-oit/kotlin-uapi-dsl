package edu.byu.uapi.server.scalars

import edu.byu.uapi.model.UAPIApiType
import edu.byu.uapi.model.UAPIValueConstraints
import edu.byu.uapi.model.UAPIValueType
import edu.byu.uapi.server.inputs.create
import edu.byu.uapi.spi.UAPITypeError
import edu.byu.uapi.spi.rendering.ScalarRenderer
import edu.byu.uapi.spi.scalars.ScalarFormat
import edu.byu.uapi.spi.scalars.ScalarType
import java.math.BigDecimal
import java.math.BigInteger
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.ByteBuffer
import java.time.*
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.reflect.KClass

val builtinScalarTypes: List<ScalarType<*>> = listOf<ScalarType<*>>(
    // Primitives and pseudo-primitives
    StringScalarType,
    BooleanScalarType,
    CharScalarType,
    ByteScalarType,
    ShortScalarType,
    IntScalarType,
    FloatScalarType,
    LongScalarType,
    DoubleScalarType,
    BigIntegerScalarType,
    BigDecimalScalarType,

    // Date/time
    InstantScalarType,
    LocalDateScalarType,
    LocalDateTimeScalarType,
    ZonedDateTimeScalarType,
    OffsetDateTimeScalarType,
    OffsetTimeScalarType,
    LocalTimeScalarType,
    YearMonthScalarType,
    MonthDayScalarType,
    DurationScalarType,
    PeriodScalarType,
    YearScalarType,
    EnumScalarType(DayOfWeek::class),
    EnumScalarType(Month::class),

    JavaUtilDateScalarType,
    JavaSqlDateScalarType,
    JavaSqlTimestampScalarType,

    // Misc platform types
    UUIDScalarType,
    URLScalarType,
    URIScalarType,
    ByteArrayScalarType,
    ByteBufferScalarType,

    // UAPI Built-ins
    ApiTypeScalarType
)

val builtinScalarTypeMap: Map<KClass<*>, ScalarType<*>> = builtinScalarTypes.map { it.type to it }.toMap()

open class EnumScalarType<E : Enum<E>>(
    final override val type: KClass<E>,
    private val strict: Boolean = false
) : ScalarType<E> {

    @Suppress("UNCHECKED_CAST")
    constructor(constants: Array<E>) : this(constants.first()::class as KClass<E>)

    val enumConstants: Set<E> = EnumSet.allOf(type.java)

    private val valuesToEnums: Map<String, E> = enumConstants.map { renderToString(it) to it }.toMap()
    private val enumsToValues: Map<E, String> = EnumMap(valuesToEnums.map { it.value to it.key }.toMap())

    val enumValues: List<String> = valuesToEnums.keys.toList()

    override val scalarFormat: ScalarFormat = ScalarFormat.STRING.asEnum(enumValues)

    private val variants: Map<String, E> by lazy {
        valuesToEnums.flatMap { e ->
            enumNameVariants(e.key).map { it to e.value }
        }.toMap()
    }

    override fun renderToString(value: E): String = value.toString()

    override fun fromString(value: String): E {
        val found = valuesToEnums[value]
        if (found == null && !strict) {
            val variant = variants[value]
            if (variant != null) return variant
        }
        if (found != null) {
            return found
        }
        throw UAPITypeError.create(type, "Invalid " + type.simpleName + " value")
    }

    override fun <S> render(
        value: E,
        renderer: ScalarRenderer<S>
    ) = renderer.string(enumsToValues.getValue(value))

    override val valueType: UAPIValueType = UAPIValueType.STRING
    override val constraints: UAPIValueConstraints? = UAPIValueConstraints(enum = enumValues.toSet())
}

object ApiTypeScalarType : EnumScalarType<UAPIApiType>(type = UAPIApiType::class, strict = true) {
    override fun renderToString(value: UAPIApiType): String {
        return value.apiValue
    }
}

private fun isCamelCase(value: String): Boolean {
    if (value.isBlank()) return false
    if (!value[0].isLowerCase()) return false
    if (value.contains('_') || value.contains('-')) return false
    return value.any { it.isUpperCase() }
}

private fun enumNameVariants(name: String): Iterable<String> {
    val set = mutableSetOf(name)
    val unCameled = if (isCamelCase(name)) {//Assume camel case
        name.fold("") { acc, c ->
            if (c.isUpperCase()) {
                acc + '_' + c.toLowerCase()
            } else {
                acc + c
            }
        }
    } else {
        name
    }

    val upper = unCameled.toUpperCase()
    val lower = unCameled.toLowerCase()

    set.add(upper)
    set.add(lower)
    set.add(lower.replace('_', '-'))
    set.add(upper.replace('_', '-'))

    return set
}

object StringScalarType : ScalarType<String> {
    override val type = String::class
    override val scalarFormat: ScalarFormat = ScalarFormat.STRING
    override fun fromString(value: String): String = value
    override fun <S> render(
        value: String,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value)

    override val valueType: UAPIValueType = UAPIValueType.STRING
}

object BooleanScalarType : ScalarType<Boolean> {
    override val type = Boolean::class
    override val scalarFormat: ScalarFormat = ScalarFormat.BOOLEAN
    override fun fromString(value: String): Boolean {
        return when (value.toLowerCase()) {
            "true" -> true
            "false" -> false
            else -> throw UAPITypeError.create(Boolean::class, "Invalid boolean value")
        }
    }

    override fun <S> render(
        value: Boolean,
        renderer: ScalarRenderer<S>
    ) = renderer.boolean(value)

    override val valueType: UAPIValueType = UAPIValueType.BOOLEAN
}

object CharScalarType : ScalarType<Char> {
    override val type = Char::class
    override val scalarFormat: ScalarFormat = ScalarFormat.STRING
    override fun fromString(value: String): Char {
        if (value.length != 1) {
            throw UAPITypeError.create(
                Char::class,
                "Expected an input with a length of 1, got a length of ${value.length}"
            )
        }
        return value[0]
    }

    override fun <S> render(
        value: Char,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.STRING
    override val constraints: UAPIValueConstraints? = UAPIValueConstraints(
        minLength = 1,
        maxLength = 1
    )
}

object ByteScalarType : ScalarType<Byte> {
    override val type = Byte::class
    override val scalarFormat: ScalarFormat = ScalarFormat.INTEGER
    override fun fromString(value: String): Byte {
        return value.toByteOrNull() ?: throw UAPITypeError.create(Byte::class, "Invalid byte value")
    }

    override fun <S> render(
        value: Byte,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value.toInt())

    override val valueType: UAPIValueType = UAPIValueType.INT
    override val constraints: UAPIValueConstraints? = UAPIValueConstraints(
        minimum = Byte.MIN_VALUE.toInt().toBigDecimal(),
        maximum = Byte.MAX_VALUE.toInt().toBigDecimal()
    )
}

object ShortScalarType : ScalarType<Short> {
    override val type = Short::class
    override val scalarFormat: ScalarFormat = ScalarFormat.INTEGER
    override fun fromString(value: String): Short {
        return value.toShortOrNull() ?: throw UAPITypeError.create(Short::class, "Invalid short integer value")
    }

    override fun <S> render(
        value: Short,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value.toInt())

    override val valueType: UAPIValueType = UAPIValueType.INT
}

object IntScalarType : ScalarType<Int> {
    override val type = Int::class
    override val scalarFormat: ScalarFormat = ScalarFormat.INTEGER
    override fun fromString(value: String): Int {
        return value.toIntOrNull() ?: throw UAPITypeError.create(Int::class, "Invalid integer value")
    }

    override fun <S> render(
        value: Int,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value)

    override val valueType: UAPIValueType = UAPIValueType.INT
}

object FloatScalarType : ScalarType<Float> {
    override val type = Float::class
    override val scalarFormat: ScalarFormat = ScalarFormat.FLOAT
    override fun fromString(value: String): Float {
        return value.toFloatOrNull() ?: throw UAPITypeError.create(Float::class, "Invalid decimal value")
    }

    override fun <S> render(
        value: Float,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value)

    override val valueType: UAPIValueType = UAPIValueType.DECIMAL
}

object LongScalarType : ScalarType<Long> {
    override val type = Long::class
    override val scalarFormat: ScalarFormat = ScalarFormat.LONG
    override fun fromString(value: String): Long {
        return value.toLongOrNull() ?: throw UAPITypeError.create(Long::class, "Invalid long integer value")
    }

    override fun <S> render(
        value: Long,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value)

    override val valueType: UAPIValueType = UAPIValueType.BIG_INT
}

object DoubleScalarType : ScalarType<Double> {
    override val type = Double::class
    override val scalarFormat: ScalarFormat = ScalarFormat.DOUBLE
    override fun fromString(value: String): Double {
        return value.toDoubleOrNull() ?: throw UAPITypeError.create(Double::class, "Invalid long decimal value")
    }

    override fun <S> render(
        value: Double,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value)

    override val valueType: UAPIValueType = UAPIValueType.BIG_DECIMAL
}

object BigIntegerScalarType : ScalarType<BigInteger> {
    override val type = BigInteger::class
    override val scalarFormat: ScalarFormat = ScalarFormat.LONG
    override fun fromString(value: String): BigInteger {
        return value.toBigIntegerOrNull() ?: throw UAPITypeError.create(BigInteger::class, "Invalid integer")
    }

    override fun <S> render(
        value: BigInteger,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value)

    override val valueType: UAPIValueType = UAPIValueType.BIG_INT
}

object BigDecimalScalarType : ScalarType<BigDecimal> {
    override val type = BigDecimal::class
    override val scalarFormat: ScalarFormat = ScalarFormat.DOUBLE
    override fun fromString(value: String): BigDecimal {
        return value.toBigDecimalOrNull() ?: throw UAPITypeError.create(BigDecimal::class, "Invalid decimal")
    }

    override fun <S> render(
        value: BigDecimal,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value)

    override val valueType: UAPIValueType = UAPIValueType.BIG_DECIMAL
}

object InstantScalarType : ScalarType<Instant> {
    override val type = Instant::class
    override val scalarFormat: ScalarFormat = ScalarFormat.DATE_TIME
    override fun fromString(value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                Instant::class,
                "Invalid timestamp. Must be a valid RFC-3339 'date-time' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: Instant,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.DATE_TIME
}

object LocalDateScalarType : ScalarType<LocalDate> {
    override val type = LocalDate::class
    override val scalarFormat: ScalarFormat = ScalarFormat.DATE
    override fun fromString(value: String): LocalDate {
        return try {
            LocalDate.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                LocalDate::class,
                "Invalid timestamp. Must be a valid RFC-3339 'full-date' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: LocalDate,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.DATE
}

object LocalDateTimeScalarType : ScalarType<LocalDateTime> {
    override val type = LocalDateTime::class
    override val scalarFormat: ScalarFormat = ScalarFormat.DATE_TIME
    override fun fromString(value: String): LocalDateTime {
        return try {
            LocalDateTime.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                LocalDateTime::class,
                "Invalid date/time. Must be a valid RFC-3339 'date-time' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: LocalDateTime,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.DATE_TIME
}

object ZonedDateTimeScalarType : ScalarType<ZonedDateTime> {
    override val type = ZonedDateTime::class
    override val scalarFormat: ScalarFormat = ScalarFormat.DATE_TIME
    override fun fromString(value: String): ZonedDateTime {
        return try {
            ZonedDateTime.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                ZonedDateTime::class,
                "Invalid date/time with time zone. Must be a valid RFC-3339 'date-time' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: ZonedDateTime,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.DATE_TIME
}

object OffsetDateTimeScalarType : ScalarType<OffsetDateTime> {
    override val type = OffsetDateTime::class
    override val scalarFormat: ScalarFormat = ScalarFormat.DATE_TIME
    override fun fromString(value: String): OffsetDateTime {
        return try {
            OffsetDateTime.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                OffsetDateTime::class,
                "Invalid date/time with zone offset. Must be a valid RFC-3339 'date-time' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: OffsetDateTime,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.DATE_TIME
}

object OffsetTimeScalarType : ScalarType<OffsetTime> {
    override val type = OffsetTime::class
    override val scalarFormat: ScalarFormat = ScalarFormat.TIME
    override fun fromString(value: String): OffsetTime {
        return try {
            OffsetTime.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                OffsetTime::class,
                "Invalid time with zone offset. Must be a valid RFC-3339 'full-time' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: OffsetTime,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.STRING //TODO Better type?
}

object LocalTimeScalarType : ScalarType<LocalTime> {
    override val type = LocalTime::class
    override val scalarFormat: ScalarFormat = ScalarFormat.TIME
    override fun fromString(value: String): LocalTime {
        return try {
            LocalTime.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                LocalTime::class,
                "Invalid time value. Must be a valid RFC-3339 'partial-time' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: LocalTime,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.STRING //TODO Better type?
}

object YearMonthScalarType : ScalarType<YearMonth> {
    override val type = YearMonth::class
    override val scalarFormat: ScalarFormat = ScalarFormat.STRING
    override fun fromString(value: String): YearMonth {
        return try {
            YearMonth.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(YearMonth::class, "Invalid year/month value. Must be formatted like 'yyyy-MM'.")
        }
    }

    override fun <S> render(
        value: YearMonth,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.STRING
    override val constraints = UAPIValueConstraints(pattern = """^(-)?\d{4}-(01|02|03|04|05|06|07|08|09|10|11|12)$""")
}

object MonthDayScalarType : ScalarType<MonthDay> {
    override val type = MonthDay::class
    override val scalarFormat: ScalarFormat = ScalarFormat.STRING
    override fun fromString(value: String): MonthDay {
        return try {
            MonthDay.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                MonthDay::class,
                "Invalid year/month value. Must be formatted like '--MM-dd', per ISO-8601."
            )
        }
    }

    override fun <S> render(
        value: MonthDay,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())


    override val valueType: UAPIValueType = UAPIValueType.STRING
    override val constraints = UAPIValueConstraints(pattern = """^--(01|02|03|04|05|06|07|08|09|10|11|12)-\d{2}$""")

}

object DurationScalarType : ScalarType<Duration> {
    override val type = Duration::class
    override val scalarFormat: ScalarFormat = ScalarFormat.STRING
    override fun fromString(value: String): Duration {
        return try {
            Duration.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                Duration::class,
                "Invalid duration. Must be formatted as an ISO-8601 duration (PnDTnHnMn.nS)."
            )
        }
    }

    override fun <S> render(
        value: Duration,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.STRING
}

object PeriodScalarType : ScalarType<Period> {
    override val type = Period::class
    override val scalarFormat: ScalarFormat = ScalarFormat.STRING
    override fun fromString(value: String): Period {
        return try {
            Period.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                Period::class,
                "Invalid duration. Must be formatted as an ISO-8601 period (PnYnMnD or PnW)."
            )
        }
    }

    override fun <S> render(
        value: Period,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.STRING
}

object YearScalarType : ScalarType<Year> {
    override val type = Year::class
    override val scalarFormat: ScalarFormat = ScalarFormat.INTEGER
    override fun fromString(value: String): Year {
        return try {
            Year.parse(value)
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(Year::class, "Invalid year value.")
        }
    }

    override fun <S> render(
        value: Year,
        renderer: ScalarRenderer<S>
    ) = renderer.number(value.value)

    override val valueType: UAPIValueType = UAPIValueType.INT
    override val constraints = UAPIValueConstraints(
        minimum = Year.MIN_VALUE.toBigDecimal(),
        maximum = Year.MAX_VALUE.toBigDecimal()
    )
}

object UUIDScalarType : ScalarType<UUID> {
    override val type = UUID::class
    override val scalarFormat: ScalarFormat = ScalarFormat.UUID
    override fun fromString(value: String): UUID {
        return try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw UAPITypeError.create(UUID::class, "Invalid UUID value.")
        }
    }

    override fun <S> render(
        value: UUID,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toString())

    override val valueType: UAPIValueType = UAPIValueType.STRING
}

object ByteArrayScalarType : ScalarType<ByteArray> {
    override val type = ByteArray::class
    override val scalarFormat: ScalarFormat = ScalarFormat.BYTE_ARRAY
    override fun fromString(value: String): ByteArray {
        val decoder = decoderFor(value)
            ?: throw UAPITypeError.create(ByteArray::class, "Invalid base64-encoded bytes.")

        return try {
            decoder.decode(value)
        } catch (er: IllegalArgumentException) {
            throw UAPITypeError.create(ByteArray::class, "Invalid base64-encoded bytes.")
        }
    }

    override fun <S> render(
        value: ByteArray,
        renderer: ScalarRenderer<S>
    ): S {
        TODO("not implemented")
    }

    override val valueType: UAPIValueType = UAPIValueType.BYTE_ARRAY
}

object ByteBufferScalarType : ScalarType<ByteBuffer> {
    override val type = ByteBuffer::class
    override val scalarFormat: ScalarFormat = ScalarFormat.BYTE_ARRAY
    override fun fromString(value: String): ByteBuffer {
        val decoder = decoderFor(value)
            ?: throw UAPITypeError.create(ByteBuffer::class, "Invalid base64-encoded bytes.")

        return try {
            ByteBuffer.wrap(decoder.decode(value))
        } catch (er: IllegalArgumentException) {
            throw UAPITypeError.create(ByteBuffer::class, "Invalid base64-encoded bytes.")
        }
    }

    override fun <S> render(
        value: ByteBuffer,
        renderer: ScalarRenderer<S>
    ): S {
        TODO("not implemented")
    }

    override val valueType: UAPIValueType = UAPIValueType.BYTE_ARRAY
}

object URLScalarType : ScalarType<URL> {
    override val type: KClass<URL> = URL::class
    override val scalarFormat: ScalarFormat = ScalarFormat.URI

    override fun fromString(value: String): URL {
        return try {
            URL(value)
        } catch (ex: MalformedURLException) {
            throw UAPITypeError.create(URL::class, "Malformed URL")
        }
    }

    override fun <S> render(
        value: URL,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toExternalForm())

    override val valueType: UAPIValueType = UAPIValueType.STRING //TODO Better type?
}

object URIScalarType : ScalarType<URI> {
    override val type: KClass<URI> = URI::class
    override val scalarFormat: ScalarFormat = ScalarFormat.URI

    override fun fromString(value: String): URI {
        return try {
            URI(value)
        } catch (ex: URISyntaxException) {
            throw UAPITypeError.create(URI::class, "Invalid URI")
        }
    }

    override fun <S> render(
        value: URI,
        renderer: ScalarRenderer<S>
    ) = renderer.string(value.toASCIIString())

    override val valueType: UAPIValueType = UAPIValueType.STRING //TODO Better type?
}

abstract class PreJavaTimeScalarTypeBase<T : java.util.Date>
    : ScalarType<T> {
    abstract override val type: KClass<T>
    override val scalarFormat: ScalarFormat = ScalarFormat.DATE_TIME
    final override fun fromString(value: String): T {
        return try {
            val instant = Instant.parse(value)
            fromEpochMillis(instant.toEpochMilli())
        } catch (ex: DateTimeParseException) {
            throw UAPITypeError.create(
                this.type,
                "Invalid timestamp. Must be a valid RFC-3339 'date-time' (https://tools.ietf.org/html/rfc3339)."
            )
        }
    }

    override fun <S> render(
        value: T,
        renderer: ScalarRenderer<S>
    ) = renderer.string(Instant.ofEpochMilli(value.time).toString())

    protected abstract fun fromEpochMillis(time: Long): T

    override val valueType: UAPIValueType = UAPIValueType.DATE_TIME
}

object JavaUtilDateScalarType : PreJavaTimeScalarTypeBase<java.util.Date>() {
    override val type = java.util.Date::class

    override fun fromEpochMillis(time: Long): Date = Date(time)
}

object JavaSqlDateScalarType : PreJavaTimeScalarTypeBase<java.sql.Date>() {
    override val type = java.sql.Date::class

    override fun fromEpochMillis(time: Long): java.sql.Date = java.sql.Date(time)
}

object JavaSqlTimestampScalarType : PreJavaTimeScalarTypeBase<java.sql.Timestamp>() {
    override val type = java.sql.Timestamp::class

    override fun fromEpochMillis(time: Long): java.sql.Timestamp = java.sql.Timestamp(time)
}

private fun decoderFor(value: String): Base64.Decoder? {
    return when {
        value.contains('+') || value.contains('/') -> Base64.getDecoder()
        value.contains('-') || value.contains('_') -> Base64.getUrlDecoder()
        value.contains('\r') || value.contains('\n') -> Base64.getMimeDecoder()
        else -> null
    }
}
