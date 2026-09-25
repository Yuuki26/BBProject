package Ships;

/**
 * Everything a hull exposes to the rest of the game.
 *
 * <p>Implemented through {@link AbstractShip} and the six hull classes beneath it
 * ({@link AircraftCarrier}, {@link Battleship}, {@link HeavyCruiser}, {@link LightCruiser},
 * {@link Destroyer}, {@link Submarine}). Named vessels live in {@code Ships.vessels}.
 */
public interface Ships_Type {

    /** The vessel's name, e.g. {@code "Enterprise"}. */
    String getName();

    /** The hull class, e.g. {@code "Aircraft Carrier"}, with its abbreviation. */
    String getHullClass();

    /** Short hull code: CV, BB, CB, CL, DD or SS. */
    String getHullCode();

    /**
     * What this vessel costs to deploy.
     *
     * <p>Each named vessel sets its own, so ships of one class can be priced apart. It is
     * fixed when the ship is built and final on {@link AbstractShip}, so no skill or reward
     * can change it. Fleets are budgeted in cost rather than tile count, which is what keeps
     * a board of six destroyers and a board of two battleships comparable.
     */
    int getCost();

    int getHP();

    int getShields();

    int getDMG();

    int getShots();

    int getPenetration();

    /**
     * How easily the enemy finds this ship. Higher is more visible.
     *
     * <p>This is the value the stealth map is built from: it sets each tile's colour and the
     * weight a searching opponent pays there. A submerged submarine sits near zero; a
     * battleship is hard to miss.
     *
     */
    int getDetection();

    int getSize();

    String getImage();
}
