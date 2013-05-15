package net.floodlightcontroller.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.floodlightcontroller.util.EnumBitmaps;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortFeatures;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPhysicalPort.PortSpeed;


/**
 * An immutable version of an OFPhysical port. In addition, it uses EnumSets
 * instead of integer bitmaps to represent
 * OFPortConfig, OFPortState, and OFPortFeature bitmaps.
 *
 * TODO: create a Builder so we can easily construct OFPhysicalPorts
 * TODO: should we verify / ensure that the features make sense, i.e., that
 *     currentFeatures IsSubsetOf advertisedFeatures IsSubsetOf
 *     supportedFeatures
 *
 * @author gregor
 *
 */
public class ImmutablePort {
    private final short portNumber;
    private final byte[] hardwareAddress;
    private final String name;
    private final EnumSet<OFPortConfig> config;
    private final boolean portStateLinkDown;
    private final OFPortState stpState;
    private final EnumSet<OFPortFeatures> currentFeatures;
    private final EnumSet<OFPortFeatures> advertisedFeatures;
    private final EnumSet<OFPortFeatures> supportedFeatures;
    private final EnumSet<OFPortFeatures> peerFeatures;

    public static ImmutablePort fromOFPhysicalPort(OFPhysicalPort p) {
        if (p == null) {
            throw new NullPointerException("OFPhysicalPort must not be null");
        }

        return new ImmutablePort(

                p.getPortNumber(),
                Arrays.copyOf(p.getHardwareAddress(), 6),
                p.getName(),
                EnumBitmaps.toEnumSet(OFPortConfig.class, p.getConfig()),
                OFPortState.isPortDown(p.getState()),
                OFPortState.getStpState(p.getState()),
                EnumBitmaps.toEnumSet(OFPortFeatures.class,
                                      p.getCurrentFeatures()),
                EnumBitmaps.toEnumSet(OFPortFeatures.class,
                                      p.getAdvertisedFeatures()),
                EnumBitmaps.toEnumSet(OFPortFeatures.class,
                                      p.getSupportedFeatures()),
                EnumBitmaps.toEnumSet(OFPortFeatures.class,
                                      p.getPeerFeatures())
                                      );
    }

    public static ImmutablePort create(String name, Short portNumber) {
        return new ImmutablePort(portNumber,
                                         new byte[] { 0, 0, 0, 0, 0, 0 },
                                         name,
                                         EnumSet.noneOf(OFPortConfig.class),
                                         false,
                                         OFPortState.OFPPS_STP_LISTEN,
                                         EnumSet.noneOf(OFPortFeatures.class),
                                         EnumSet.noneOf(OFPortFeatures.class),
                                         EnumSet.noneOf(OFPortFeatures.class),
                                         EnumSet.noneOf(OFPortFeatures.class));
    }

    /**
     * Private constructor. Use factory methods.
     *
     * Verifies pre-conditions of arguments
     * Does NOT make defensive copies. Calling factory methods are required
     * to copy defensively.
     *
     * @param portNumber
     * @param hardwareAddress
     * @param name
     * @param config
     * @param portStateLinkDown
     * @param portStateStp
     * @param currentFeatures
     * @param advertisedFeatures
     * @param supportedFeatures
     * @param peerFeatures
     */
    private ImmutablePort(short portNumber, byte[] hardwareAddress,
                                 String name, EnumSet<OFPortConfig> config,
                                 boolean portStateLinkDown,
                                 OFPortState portStateStp,
                                 EnumSet<OFPortFeatures> currentFeatures,
                                 EnumSet<OFPortFeatures> advertisedFeatures,
                                 EnumSet<OFPortFeatures> supportedFeatures,
                                 EnumSet<OFPortFeatures> peerFeatures) {
        if (name == null) {
            throw new NullPointerException("Port name must not be null");
        }
        if (hardwareAddress== null)  {
            throw new NullPointerException("Hardware address must not be null");
        }
        if (hardwareAddress.length != 6) {
            throw new IllegalArgumentException("Harware address must be 6 " +
                    "bytes long but hardware address is " +
                    Arrays.toString(hardwareAddress));
        }
        this.portNumber = portNumber;
        this.hardwareAddress = hardwareAddress;
        this.name = name;
        this.config = config;
        this.portStateLinkDown = portStateLinkDown;
        this.stpState = portStateStp;
        this.currentFeatures = currentFeatures;
        this.advertisedFeatures = advertisedFeatures;
        this.supportedFeatures = supportedFeatures;
        this.peerFeatures = peerFeatures;
    }

    public short getPortNumber() {
        return portNumber;
    }

    public byte[] getHardwareAddress() {
        // FIXME: don't use arrays.
        return Arrays.copyOf(hardwareAddress, 6);
    }

    public String getName() {
        return name;
    }

    public Set<OFPortConfig> getConfig() {
        return Collections.unmodifiableSet(config);
    }

    /**
     * Returns true if the OFPortState indicates the port is down
     * @return
     */
    public boolean isLinkDown() {
        return portStateLinkDown;
    }

    /**
     * Returns the STP state portion of the OFPortState. The returned
     * enum constant will be one of the four STP states and will have
     * isStpState() return true
     * @return
     */
    public OFPortState getStpState() {
        return this.stpState;
    }

    public Set<OFPortFeatures> getCurrentFeatures() {
        return Collections.unmodifiableSet(currentFeatures);
    }

    public Set<OFPortFeatures> getAdvertisedFeatures() {
        return Collections.unmodifiableSet(advertisedFeatures);
    }

    public Set<OFPortFeatures> getSupportedFeatures() {
        return Collections.unmodifiableSet(supportedFeatures);
    }

    public Set<OFPortFeatures> getPeerFeatures() {
        return Collections.unmodifiableSet(peerFeatures);
    }


    /**
     * Returns true if the port is up, i.e., it's neither administratively
     * down nor link down. It currently does NOT take STP state into
     * consideration
     * @return
     */
    public boolean isEnabled() {
        return (!portStateLinkDown &&
                !config.contains(OFPortConfig.OFPPC_PORT_DOWN));
    }

    /**
     * @return the speed of the port (from currentFeatures) if the port is
     * enabled, otherwise return SPEED_NONE
     */
    public PortSpeed getCurrentPortSpeed() {
        if (!isEnabled())
            return PortSpeed.SPEED_NONE;
        PortSpeed maxSpeed = PortSpeed.SPEED_NONE;
        for (OFPortFeatures f: currentFeatures)
            PortSpeed.max(maxSpeed, f.getSpeed());
        return maxSpeed;
    }

    public OFPhysicalPort toOFPhysicalPort() {
        OFPhysicalPort ofpp = new OFPhysicalPort();
        ofpp.setHardwareAddress(this.getHardwareAddress());
        ofpp.setName(this.getName());
        ofpp.setConfig(EnumBitmaps.toBitmap(this.getConfig()));
        int state = this.getStpState().getValue();
        if (this.isLinkDown())
            state |= OFPortState.OFPPS_LINK_DOWN.getValue();
        ofpp.setState(state);
        ofpp.setCurrentFeatures(EnumBitmaps.toBitmap(this.getCurrentFeatures()));
        ofpp.setAdvertisedFeatures(
                EnumBitmaps.toBitmap(this.getAdvertisedFeatures()));
        ofpp.setSupportedFeatures(
                EnumBitmaps.toBitmap(this.getSupportedFeatures()));
        ofpp.setPeerFeatures(EnumBitmaps.toBitmap(this.getPeerFeatures()));
        return ofpp;
    }

    /**
     * Return a brief String describing this port containing the port number
     * and port name
     * @return
     */
    public String toBriefString() {
        return String.format("%d (%s)", portNumber, name);

    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                 * result
                 + ((advertisedFeatures == null) ? 0
                                                : advertisedFeatures.hashCode());
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime
                 * result
                 + ((currentFeatures == null) ? 0
                                             : currentFeatures.hashCode());
        result = prime * result + Arrays.hashCode(hardwareAddress);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                 + ((peerFeatures == null) ? 0 : peerFeatures.hashCode());
        result = prime * result + portNumber;
        result = prime * result + (portStateLinkDown ? 1231 : 1237);
        result = prime * result
                 + ((stpState == null) ? 0 : stpState.hashCode());
        result = prime
                 * result
                 + ((supportedFeatures == null) ? 0
                                               : supportedFeatures.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ImmutablePort other = (ImmutablePort) obj;
        if (portNumber != other.portNumber) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (advertisedFeatures == null) {
            if (other.advertisedFeatures != null) return false;
        } else if (!advertisedFeatures.equals(other.advertisedFeatures))
            return false;
        if (config == null) {
            if (other.config != null) return false;
        } else if (!config.equals(other.config)) return false;
        if (currentFeatures == null) {
            if (other.currentFeatures != null) return false;
        } else if (!currentFeatures.equals(other.currentFeatures))
            return false;
        if (!Arrays.equals(hardwareAddress, other.hardwareAddress))
            return false;
        if (peerFeatures == null) {
            if (other.peerFeatures != null) return false;
        } else if (!peerFeatures.equals(other.peerFeatures)) return false;
        if (portStateLinkDown != other.portStateLinkDown) return false;
        if (stpState != other.stpState) return false;
        if (supportedFeatures == null) {
            if (other.supportedFeatures != null) return false;
        } else if (!supportedFeatures.equals(other.supportedFeatures))
            return false;
        return true;
    }

    /**
     * Convert a Collection of OFPhysicalPorts to a list of ImmutablePorts.
     * All OFPhysicalPorts in the Collection must be non-null and valid.
     * No other checks (name / number uniqueness) are performed
     * @param ports
     * @return a list of {@link ImmutablePort}s. This is list is owned by
     * the caller. The returned list is not thread-safe
     * @throws NullPointerException if any OFPhysicalPort or important fields
     * of any OFPhysicalPort are null
     * @throws IllegalArgumentException
     */
    public static List<ImmutablePort>
            immutablePortListOf(Collection<OFPhysicalPort> ports) {
        if (ports == null) {
            throw new NullPointerException("Port list must not be null");
        }
        ArrayList<ImmutablePort> immutablePorts =
                new ArrayList<ImmutablePort>(ports.size());
        for (OFPhysicalPort p: ports)
            immutablePorts.add(fromOFPhysicalPort(p));
        return immutablePorts;
    }

    /**
     * Convert a Collection of ImmutablePort to a list of OFPhyscialPorts.
     * All ImmutablePorts in the Collection must be non-null.
     * No other checks (name / number uniqueness) are performed
     * @param ports
     * @return a list of {@link OFPhysicalPort}s. This is list is owned by
     * the caller. The returned list is not thread-safe
     * @throws NullPointerException if any {@link ImmutablePort} or the port
     * list is null
     * @throws IllegalArgumentException
     */
    public static List<OFPhysicalPort>
            ofPhysicalPortListOf(Collection<ImmutablePort> ports) {
        if (ports == null) {
            throw new NullPointerException("Port list must not be null");
        }
        ArrayList<OFPhysicalPort> ofppList=
                new ArrayList<OFPhysicalPort>(ports.size());
        for (ImmutablePort p: ports) {
            if (p == null)
                throw new NullPointerException("Port must not be null");
            ofppList.add(p.toOFPhysicalPort());
        }
        return ofppList;
    }
}
