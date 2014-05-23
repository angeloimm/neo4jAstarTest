package it.angelo.routing.osm.reader.util;

public class EncodedValue
{
	private final String name;
	private final long maxValue;
	@SuppressWarnings("unused")
	private final long defaultValue;
	private final boolean allowNegative;
	private final boolean allowZero;
	@SuppressWarnings("unused")
	private long value;

	public EncodedValue(String name, int defaultValue, int maxValue)
	{
		this(name, defaultValue, maxValue, false, true);
	}

	public EncodedValue(String name, int defaultValue, int maxValue, boolean allowNegative, boolean allowZero)
	{
		this.name = name;
		this.defaultValue = defaultValue;
		this.maxValue = maxValue;
		this.allowNegative = allowNegative;
		this.allowZero = allowZero;
	}

	public long setValue(long value)
	{
		if (value > maxValue)
			throw new IllegalArgumentException(name + " value too large for encoding: " + value + ", maxValue:" + maxValue);
		if (!allowNegative && value < 0)
			throw new IllegalArgumentException("negative " + name + " value not allowed! " + value);
		if (!allowZero && value == 0)
			throw new IllegalArgumentException("zero " + name + " value not allowed! " + value);

		return this.value = value;

	}

	public long getMaxValue()
	{
		return maxValue;
	}

}