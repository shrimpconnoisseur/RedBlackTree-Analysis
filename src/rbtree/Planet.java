package rbtree;

/*
* Contains information extracted from the NASA Exoplanet Archive.
* Specifically, the table containing generic planetary system information.
* Many of these measurements are in comparison to Earth or another planetary body in the Solar System.
 */
public class Planet implements Comparable<Planet> {
    // Identification
    // These are self-explanatory, right?
    public final String planetName;     // name of planetary body used in literature
    public final String hostName;       // stellar name used in literature
    public final String discoveryMethod;    // method by which the planet was discovered
    public final int discoveryYear;     // year the planetary body was discovered
    public final String discFacility;   // name of the facility that discovered the planetary body

    // Orbital
    public final double orbitalPeriod;  // planet's orbit measured in days
    public final double semiMajorAxis;  // planet's average distance from the Sun (or star) measured in astronomical units
    public final double eccentricity;   // planet's orbit deviation from a perfect circle

    // Planet Physical
    public final double radiusEarth;    // planet's radii in comparison to Earth
    public final double massEarth;      // planet's mass in comparison to Earth
    public final double massJupiter;    // planet's mass in comparison to Jupiter
    public final double insolationFlux; // planet's incoming solar radiation
    public final double eqTemperature;  // planet's temperature measured in Kelvin

    // Stellar
    public final String spectralType;   // a star's spectral type following the Morgan-Keenan classification
                                        // spectra refers to the star's temperature, chemical composition, and luminosity
    public final double stellarTemperature; // a star's temperature measured in Kelvin
    public final double stellarRadius;  // a star's radii in comparison to the Sun
    public final double stellarMass;    // a star's mass in comparison to the Sun
    public final double stellarMetallicity; // a star's metal content in comparison to its hydrogen

    // Celestial Coordinates
    public final double ra;             // right ascension, the "longitude" of a planetary body from its vernal equinox.
                                        // the vernal equinox is the exact point on a planetary body where its axis is tilted neither toward nor away from its star
                                        // for example, Earth's vernal equinox is approximately on March 20th, marking a seasonal change.
                                        // measured in hours (h), minutes (m), and seconds (s) where 24 hours equals 360 degrees.

    public final double dec;             // declination, the "latitude" of a planetary body from its celestial equator.
                                        // measured in degrees, arc-minutes, and arc-seconds (60 arc-minutes = 1 degree, 60 arc-seconds = 1 arc-minute)
                                        // for example, Earth's celestial equator is equal to 0 degrees, the North Pole equal to +90 degrees, and the South Pole equal to -90 degrees.

    public final double distance;       // distance from Earth to the planetary body measured in parsecs
                                        // 1 parsec = 206265 au     -->     1 au = 9.296e+7 mi

    // Defining a Planet
    public Planet(String planetName, String hostname, String discoveryMethod, int discoveryYear, String discFacility,                   // Identification
                  double orbitalPeriod, double semiMajorAxis, double eccentricity,                                                      // Orbital
                  double radiusEarth, double massEarth, double massJupiter, double insolationFlux, double eqTemperature,                // Planet Physical
                  String spectralType, double stellarTemperature, double stellarRadius, double stellarMass, double stellarMetallicity,  // Stellar
                  double ra, double dec, double distance) {                                                                              // Celestial Coordinates

        this.planetName = planetName;
        this.hostName = hostname;
        this.discoveryMethod = discoveryMethod;
        this.discoveryYear = discoveryYear;
        this.discFacility = discFacility;
        this.orbitalPeriod = orbitalPeriod;
        this.semiMajorAxis = semiMajorAxis;
        this.eccentricity = eccentricity;
        this.radiusEarth = radiusEarth;
        this.massEarth = massEarth;
        this.massJupiter = massJupiter;
        this.insolationFlux = insolationFlux;
        this.eqTemperature = eqTemperature;
        this.spectralType = spectralType;
        this.stellarTemperature = stellarTemperature;
        this.stellarRadius = stellarRadius;
        this.stellarMass = stellarMass;
        this.stellarMetallicity = stellarMetallicity;
        this.ra = ra;
        this.dec = dec;
        this.distance = distance;
    }

    @Override
    public int compareTo(Planet other) {
        return Double.compare(this.orbitalPeriod, other.orbitalPeriod);
    }

    // A few cells in the table have null values, so we handle some of that here.
    @Override
    public String toString() {
        return String.format(
                "%-25s | host: %-18s | period: %11.4f d | radius: %6.2f Re | mass: %8.3f Me | dist: %.1f pc | disc: %d (%s)",
                planetName, hostName, orbitalPeriod,
                Double.isNaN(radiusEarth) ? 0.0 : radiusEarth,
                Double.isNaN(massEarth) ? 0.0 : massEarth,
                Double.isNaN(distance) ? 0.0 : distance,
                discoveryYear, discoveryMethod
        );
    }

    public String toShortString() {
        return String.format("%-25s %10.4f d %s", planetName, orbitalPeriod, hostName);
    }
}

