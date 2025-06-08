package com.auroraschaos.minigames.game.race;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.List;

public class KartVehicle {
    private final Player driver;
    private final Boat boat;
    private final TrackConfig cfg;
    private double speed = 0;
    private boolean gliderDeployed = false;
    private long gliderEndTime;
    private boolean wallRiding = false;
    private long wallRideEndTime;
    private int lastCheckpoint = 0;
    private int lap = 0;
    private boolean finished = false;
    private double progress = 0;

    public KartVehicle(Player driver, Boat boat, TrackConfig cfg) {
        this.driver = driver;
        this.boat = boat;
        this.cfg = cfg;
    }

    public void updateMovement() {
        // TODO: acceleration, braking, slope-climb, ramp, glider, wall-ride logic
    }

    public void handleOffTrack() {
        double minY = cfg.getDouble("respawn.yOffset");
        if (boat.getLocation().getY() < minY) {
            Location cp = getLastCheckpointLocation();
            boat.teleport(cp.clone().add(0, cfg.getDouble("respawn.yOffset"), 0));
            boat.setVelocity(new Vector(0, 0, 0));
        }
    }

    public void spawnParticles() {
        // TODO: boost trails, power-up, glider particles using cfg values
    }

    public void updateProgress(List<Vector> trackPath) {
        // TODO: calculate distance-along-path + lap-based progress
    }

    public boolean hasFinishedLap() {
        return finished;
    }

    public Boat getBoat() {
        return boat;
    }
    public Player getDriver() {
        return driver;
    }
    private Location getLastCheckpointLocation() {
        // TODO: convert checkpoint index to Location list
        return boat.getLocation();
    }
}