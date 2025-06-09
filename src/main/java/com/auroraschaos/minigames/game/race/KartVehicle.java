package com.auroraschaos.minigames.game.race;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.List;

/**
 * Represents a player's kart (boat) with custom physics, steering, checkpoints, and particle effects.
 */
public class KartVehicle {
    private final Player driver;
    private final Boat boat;
    private final KartConfig cfg;
    private final List<Location> checkpoints;

    private double speed = 0;
    private Vector direction;
    private boolean finished = false;
    private int lastCheckpoint = 0;
    private int lap = 0;
    private int tickCount = 0;

    public KartVehicle(Player driver, Boat boat, KartConfig cfg, List<Location> checkpoints) {
        this.driver = driver;
        this.boat = boat;
        this.cfg = cfg;
        this.checkpoints = checkpoints;
        float initialYaw = boat.getLocation().getYaw();
        this.direction = new Vector(-Math.sin(Math.toRadians(initialYaw)), 0,
                                      Math.cos(Math.toRadians(initialYaw))).normalize();
    }

    /** Called each tick to update movement and state. */
    public void updateMovement() {
        tickCount++;
        applySteering();
        handleAcceleration();
        handleSlope();
        applyVelocity();
        spawnParticles();
        updateProgress();
    }

    // --- Steering via A/D strafe detection ---
    private void applySteering() {
        float boatYaw = boat.getLocation().getYaw();
        float playerYaw = driver.getLocation().getYaw();
        float delta = ((playerYaw - boatYaw + 540) % 360) - 180;
        double turnRate = cfg.getStat("turnRate");
        if (delta < -2) {
            boatYaw -= turnRate;
        } else if (delta > 2) {
            boatYaw += turnRate;
        }
        boat.setRotation(boatYaw, boat.getLocation().getPitch());
        this.direction = new Vector(-Math.sin(Math.toRadians(boatYaw)), 0,
                                     Math.cos(Math.toRadians(boatYaw))).normalize();
    }

    // --- Acceleration and Braking ---
    private void handleAcceleration() {
        boolean accel = driver.isSprinting();
        boolean brake = driver.isSneaking();
        double accelRate = cfg.getStat("accelRate");
        double decelRate = cfg.getStat("decelRate");
        double brakeRate = cfg.getStat("brakeRate");
        double maxSpeed  = cfg.getStat("maxSpeed");

        if (accel && !brake) {
            speed = Math.min(speed + accelRate, maxSpeed);
        } else if (brake) {
            speed = Math.max(speed - brakeRate, 0);
        } else {
            speed = Math.max(speed - decelRate, 0);
        }
    }

    // --- Slope Climbing and Controlled Drops ---
    private void handleSlope() {
        Location loc = boat.getLocation();
        Vector dirFlat = direction.clone().setY(0).normalize();
        Location front = loc.clone().add(dirFlat.multiply(1));
        Block underFront = front.subtract(0, 1, 0).getBlock();

        double stepHeight = cfg.getStat("slopeStep");
        double climbSpeed = cfg.getStat("climbSpeed");
        double dropSpeed  = cfg.getStat("dropSpeed");
        Vector vel = boat.getVelocity();
        double heightDiff = underFront.getY() - loc.getBlockY();

        if (heightDiff > 0 && heightDiff <= stepHeight) {
            vel.setY(climbSpeed);
        } else if (heightDiff < 0) {
            vel.setY(-dropSpeed);
        } else {
            vel.setY(0);
        }
        boat.setVelocity(vel);
    }

    // --- Apply final velocity vector ---
    private void applyVelocity() {
        Vector vel = direction.clone().multiply(speed);
        vel.setY(boat.getVelocity().getY());
        boat.setVelocity(vel);
    }

    // --- Particle Effects for Boost & Drift ---
    private void spawnParticles() {
        // Boost trail
        double boostThreshold = cfg.getStat("boostThreshold");
        if (speed > boostThreshold) {
            if (tickCount % (int)cfg.getStat("particles.boost.interval") == 0) {
                Particle type = Particle.valueOf(cfg.getString("particles.boost.type"));
                int count = (int)cfg.getStat("particles.boost.count");
                Location origin = boat.getLocation().clone().subtract(direction.multiply(0.5));
                boat.getWorld().spawnParticle(type, origin, count, 0.1, 0.1, 0.1, 0);
            }
        }
        // Drift sparks
        if (driver.isSprinting() && driver.isSneaking()) {
            if (tickCount % (int)cfg.getStat("particles.drift.interval") == 0) {
                Particle type = Particle.valueOf(cfg.getString("particles.drift.type"));
                int count = (int)cfg.getStat("particles.drift.count");
                boat.getWorld().spawnParticle(type, boat.getLocation(), count, 0, 0, 0, 0);
            }
        }
    }

    // --- Checkpoint & Lap Tracking ---
    private void updateProgress() {
        if (finished) return;
        Location target = checkpoints.get(lastCheckpoint);
        double radius = cfg.getStat("checkpointRadius");
        if (boat.getLocation().distanceSquared(target) <= radius * radius) {
            lastCheckpoint++;
            if (lastCheckpoint >= checkpoints.size()) {
                lap++;
                lastCheckpoint = 0;
                if (lap >= cfg.getStat("totalLaps")) {
                    finished = true;
                }
            }
        }
    }

    // --- Off-track Respawn ---
    public void handleOffTrack() {
        double minY = cfg.getStat("respawnYOffset");
        if (boat.getLocation().getY() < minY) {
            Location cp = getLastCheckpointLocation();
            boat.teleport(cp.add(0, minY, 0));
            boat.setVelocity(new Vector(0, 0, 0));
        }
    }

    private Location getLastCheckpointLocation() {
        int idx = Math.min(lastCheckpoint, checkpoints.size() - 1);
        return checkpoints.get(idx);
    }

    public boolean hasFinishedLap() {
        return finished;
    }

    public Player getDriver() {
        return driver;
    }

    public Boat getBoat() {
        return boat;
    }

    public int getLap() {
        return lap;
    }
}