@ConfigGroup("geflipper")
public interface GEFlipperConfig extends Config {
    @ConfigItem(
            keyName = "minMargin",
            name = "Minimum Margin",
            description = "Only flip items with at least this margin",
            position = 1
    )
    default int minMargin() { return 10; }

    @ConfigItem(
            keyName = "minVolume",
            name = "Minimum Volume",
            description = "Skip items with daily volume below this value",
            position = 2
    )
    default int minVolume() { return 1000; }
}